package com.jjkay03.classgraph.analysis

import com.intellij.concurrency.JobLauncher
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicatorProvider
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiManager
import com.jjkay03.classgraph.model.ClassEdge
import com.jjkay03.classgraph.model.ClassGraph
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElementOfType
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

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

        // Resolving every call is the expensive part, so classes are analysed in parallel. Each class only produces
        // edges starting at itself, so the per-class results never overlap and can simply be concatenated.
        val entries = collector.uClasses.entries.toList()
        val results = ConcurrentLinkedQueue<List<ClassEdge>>()
        val done = AtomicInteger(0)
        val indicator = ProgressIndicatorProvider.getGlobalProgressIndicator() ?: EmptyProgressIndicator()
        val completed = JobLauncher.getInstance().invokeConcurrentlyUnderProgress(entries, indicator) { (qualified, uClass) ->
            ProgressManager.checkCanceled()
            val collected = EdgeCollector(collector.nodes)
            collected.collect(collector.nodes.getValue(qualified), uClass)
            results.add(collected.result())
            onProgress("Analysing classes (${done.incrementAndGet()}/${entries.size})")
            true
        }
        ProgressManager.checkCanceled()
        check(completed) { "Class analysis was interrupted" }
        return ClassGraph(collector.nodes.values.toList(), results.flatten())
    }
}
