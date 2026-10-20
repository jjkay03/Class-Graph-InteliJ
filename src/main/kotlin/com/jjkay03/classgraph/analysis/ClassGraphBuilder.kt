package com.jjkay03.classgraph.analysis

import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jjkay03.classgraph.model.ClassGraph
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType

/** Scans project sources via UAST (identical for Java and Kotlin). Must run inside a read action. */
object ClassGraphBuilder {
    /** [onProgress] receives a status like "Reading files (2/100)" for every item, on the background thread. */
    fun build(project: Project, onProgress: (String) -> Unit = {}): ClassGraph {
        val psiManager = PsiManager.getInstance(project)
        val collector = ClassCollector()

        val files = SourceFiles.find(project)
        files.forEachIndexed { i, file ->
            ProgressManager.checkCanceled()
            onProgress("Reading files (${i + 1}/${files.size})")
            val module = ModuleUtilCore.findModuleForFile(file, project)?.name ?: "(none)"
            val uFile = psiManager.findFile(file)?.toUElementOfType<UFile>() ?: return@forEachIndexed
            collector.collectFile(uFile, file, module)
        }

        val edges = EdgeCollector(collector.nodes)
        val total = collector.uClasses.size
        var done = 0
        for ((qualified, uClass) in collector.uClasses) {
            ProgressManager.checkCanceled()
            onProgress("Analysing classes (${++done}/$total)")
            edges.collect(collector.nodes.getValue(qualified), uClass)
        }
        return ClassGraph(collector.nodes.values.toList(), edges.result())
    }
}
