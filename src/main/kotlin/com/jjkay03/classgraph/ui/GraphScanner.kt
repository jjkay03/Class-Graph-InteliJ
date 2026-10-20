package com.jjkay03.classgraph.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import com.jjkay03.classgraph.analysis.ClassGraphBuilder
import com.jjkay03.classgraph.model.ClassGraph
import org.jetbrains.concurrency.CancellablePromise
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.swing.Timer

/**
 * Runs project scans in the background and reports progress. Only one scan runs at a time: starting a new one
 * cancels the previous. [onStatus] and [onResult] are always called on the UI thread.
 */
class GraphScanner(
    private val project: Project,
    parent: Disposable,
    private val onStatus: (String) -> Unit,
    private val onResult: (ClassGraph) -> Unit,
) : Disposable {
    private var current: CancellablePromise<*>? = null

    init {
        Disposer.register(parent, this)
    }

    /** Function to start a scan; [silent] scans (automatic refreshes) never touch the status text. */
    fun scan(silent: Boolean = false) {
        current?.cancel()

        val startedAt = System.currentTimeMillis()
        val reading = AtomicBoolean(false)
        val lastPost = AtomicLong(0)

        /** Function to show [text] from any thread, unless this is a silent scan. */
        fun post(text: String) {
            if (!silent) ApplicationManager.getApplication().invokeLater({ onStatus(text) }, ModalityState.any())
        }

        // While waiting for indexing the scan reports nothing, so tick the elapsed time instead.
        val waitTicker = Timer(WAIT_TICK_MS) {
            if (!reading.get()) onStatus("Waiting for indexing… ${(System.currentTimeMillis() - startedAt) / 1000}s")
        }
        if (!silent) {
            waitTicker.start()
            onStatus("Waiting for indexing…")
        }

        current = ReadAction.nonBlocking(Callable {
            reading.set(true)
            ClassGraphBuilder.build(project) { status ->
                val now = System.currentTimeMillis()
                if (now - lastPost.get() >= STATUS_THROTTLE_MS) {
                    lastPost.set(now)
                    post(status)
                }
            }
        })
            .inSmartMode(project)
            .expireWith(this)
            .finishOnUiThread(ModalityState.any()) {
                waitTicker.stop()
                onResult(it)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
            .onError { error ->
                waitTicker.stop()
                // A newer scan superseded this one; nothing went wrong.
                if (error is CancellationException || error is ProcessCanceledException) return@onError
                logger<GraphScanner>().warn("Class graph scan failed", error)
                post("Scan failed: ${error.message}")
            }
    }

    /** Function to cancel any running scan when the tool window closes. */
    override fun dispose() {
        current?.cancel()
    }

    private companion object {
        const val STATUS_THROTTLE_MS = 80L
        const val WAIT_TICK_MS = 1000
    }
}
