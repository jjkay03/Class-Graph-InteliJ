package com.jjkay03.classgraph.analysis

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiModifier
import com.intellij.psi.SmartPointerManager
import com.jjkay03.classgraph.model.ClassNode
import com.jjkay03.classgraph.model.NodeKind
import org.jetbrains.uast.UAnonymousClass
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import kotlin.random.Random

/**
 * Turns UAST classes into graph nodes, recursing into nested classes. Every Kotlin file also gets a node:
 * files with top-level functions/properties show as one "file" node, and files with nothing else get a plain one.
 */
internal class ClassCollector {
    val nodes = LinkedHashMap<String, ClassNode>()
    val uClasses = LinkedHashMap<String, UClass>()

    /** Function to add every class of [uFile] as a node, plus a file node for Kotlin files that have no other node. */
    fun collectFile(uFile: UFile, file: VirtualFile, module: String) {
        val kotlinFileName = file.name.takeIf { file.extension == "kt" }
        uFile.classes.forEach { collect(it, module, file.path, kotlinFileName) }
        if (kotlinFileName != null && nodes.values.none { it.filePath == file.path }) addFileNode(uFile, file, module)
    }

    /**
     * Function to add [c] and its nested classes as nodes. For Kotlin, the synthetic class holding a file's
     * top-level declarations has no source element; it is shown as a file node named after the file.
     */
    private fun collect(c: UClass, module: String, filePath: String, kotlinFileName: String?) {
        val qualified = c.qualifiedName
        val name = c.name
        if (qualified != null && name != null && c !is UAnonymousClass && qualified !in nodes) {
            val isFileFacade = kotlinFileName != null && (c.sourcePsi == null || c.sourcePsi is PsiFile)
            val node = if (isFileFacade) {
                createNode(c, qualified, kotlinFileName, module, filePath, NodeKind.FILE)
            } else {
                createNode(c, qualified, name, module, filePath, kindOf(c))
            }
            nodes[qualified] = node
            uClasses[qualified] = c
        }
        c.innerClasses.forEach { collect(it, module, filePath, kotlinFileName) }
    }

    /** Function to build the node for [c], with a stable random start position derived from its name. */
    private fun createNode(c: UClass, qualified: String, name: String, module: String, filePath: String, kind: NodeKind): ClassNode {
        val psi = c.javaPsi
        val source = c.sourcePsi ?: psi.navigationElement
        val pointer = SmartPointerManager.getInstance(psi.project).createSmartPsiElementPointer(source)
        val pkg = qualified.substringBeforeLast('.', "").ifEmpty { "(default)" }
        return ClassNode(qualified, name, pkg, module, kind, filePath, pointer).also { randomizePosition(it, qualified) }
    }

    /** Function to add a node for a Kotlin file that declares no class and no top-level members. */
    private fun addFileNode(uFile: UFile, file: VirtualFile, module: String) {
        val psiFile = uFile.sourcePsi
        val pointer = SmartPointerManager.getInstance(psiFile.project).createSmartPsiElementPointer<com.intellij.psi.PsiElement>(psiFile)
        val id = "file:${file.path}"
        val pkg = uFile.packageName.ifEmpty { "(default)" }
        nodes[id] = ClassNode(id, file.name, pkg, module, NodeKind.FILE, file.path, pointer).also { randomizePosition(it, id) }
    }

    /** Function to give [node] a stable pseudo-random start position, so the layout begins spread out. */
    private fun randomizePosition(node: ClassNode, seed: String) {
        val random = Random(seed.hashCode())
        node.x = random.nextDouble(-400.0, 400.0)
        node.y = random.nextDouble(-400.0, 400.0)
    }

    /** Function to classify [c] as class, abstract class, interface, enum or annotation. */
    private fun kindOf(c: UClass): NodeKind {
        val psi = c.javaPsi
        return when {
            psi.isAnnotationType -> NodeKind.ANNOTATION
            psi.isInterface -> NodeKind.INTERFACE
            psi.isEnum -> NodeKind.ENUM
            psi.hasModifierProperty(PsiModifier.ABSTRACT) -> NodeKind.ABSTRACT
            else -> NodeKind.CLASS
        }
    }
}
