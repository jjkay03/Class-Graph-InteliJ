package com.jjkay03.classgraph.analysis

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.jjkay03.classgraph.model.ClassEdge
import com.jjkay03.classgraph.model.ClassNode
import com.jjkay03.classgraph.model.EdgeKind
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastVisitor

/** Finds the relationships of each class to other project classes and merges duplicates into weighted edges. */
internal class EdgeCollector(private val nodes: Map<String, ClassNode>) {
    private val edges = LinkedHashMap<Triple<ClassNode, ClassNode, EdgeKind>, ClassEdge>()

    /** Function to get the merged, weighted edges collected so far. */
    fun result(): List<ClassEdge> = edges.values.toList()

    /** Function to collect every relationship of [uClass] (structure and body usages) as edges from [from]. */
    fun collect(from: ClassNode, uClass: UClass) {
        collectStructure(from, uClass)
        collectBodyUsages(from, uClass)
    }

    /** Function to record one edge from [from] to [target] of [kind]; repeats add weight, targets outside the project and self-links are ignored. */
    private fun add(from: ClassNode, target: PsiClass?, kind: EdgeKind) {
        val to = nodes[target?.qualifiedName ?: return] ?: return
        if (to === from) return
        edges.getOrPut(Triple(from, to, kind)) { ClassEdge(from, to, kind, 0) }.weight++
    }

    /** Super types, fields and method signatures. */
    private fun collectStructure(from: ClassNode, uClass: UClass) {
        val psi = uClass.javaPsi
        psi.superTypes.forEach { type ->
            type.resolve()
                ?.takeIf { it.qualifiedName != "java.lang.Object" }
                ?.let { add(from, it, EdgeKind.INHERITANCE) }
        }
        psi.fields.forEach { field ->
            PsiTypes.classesIn(field.type).forEach { add(from, it, EdgeKind.FIELD) }
        }
        psi.methods.forEach { method ->
            PsiTypes.classesIn(method.returnType).forEach { add(from, it, EdgeKind.SIGNATURE) }
            method.parameterList.parameters.forEach { param ->
                PsiTypes.classesIn(param.type).forEach { add(from, it, EdgeKind.SIGNATURE) }
            }
        }
    }

    /** Constructor calls and method calls inside the class body. */
    private fun collectBodyUsages(from: ClassNode, uClass: UClass) {
        val own = uClass.javaPsi
        uClass.accept(object : AbstractUastVisitor() {
            // Nested named classes are their own nodes; anonymous ones stay attributed to the owner.
            /** Function to skip nested named classes during the body walk, keeping anonymous ones attributed to the owner. */
            override fun visitClass(node: UClass): Boolean = node.javaPsi !== own && node !is UAnonymousClass

            /** Function to record constructor calls as instantiation edges and method calls as call edges. */
            override fun visitCallExpression(node: UCallExpression): Boolean {
                when (node.kind) {
                    UastCallKind.CONSTRUCTOR_CALL -> add(from, constructedClass(node), EdgeKind.INSTANTIATION)
                    UastCallKind.METHOD_CALL -> add(from, node.resolve()?.containingClass, EdgeKind.CALL)
                    else -> {}
                }
                return super.visitCallExpression(node)
            }
        })
    }

    /** Function to find the class a constructor call creates, falling back from resolve to class reference to return type. */
    private fun constructedClass(call: UCallExpression): PsiClass? =
        call.resolve()?.containingClass
            ?: call.classReference?.resolve() as? PsiClass
            ?: (call.returnType as? PsiClassType)?.resolve()
}
