package com.jjkay03.classgraph.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiTreeChangeAdapter
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.util.Alarm
import com.jjkay03.classgraph.settings.GraphSettings

/**
 * Calls [refresh] shortly after Java/Kotlin files change, if the user turned auto-refresh on. While the graph
 * tab is closed ([isShowing] false) it only remembers that it is stale; [onShown] catches up when it reopens.
 */
class AutoRefresh(
    project: Project,
    parent: Disposable,
    private val settings: GraphSettings,
    private val isShowing: () -> Boolean,
    private val refresh: () -> Unit,
) {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, parent)
    private var staleWhileHidden = false

    init {
        PsiManager.getInstance(project).addPsiTreeChangeListener(object : PsiTreeChangeAdapter() {
            /** Function to react when a PSI element was added. */
            override fun childAdded(event: PsiTreeChangeEvent) = onCodeChanged(event)
            /** Function to react when a PSI element was removed. */
            override fun childRemoved(event: PsiTreeChangeEvent) = onCodeChanged(event)
            /** Function to react when a PSI element was replaced. */
            override fun childReplaced(event: PsiTreeChangeEvent) = onCodeChanged(event)
            /** Function to react when a PSI element was moved. */
            override fun childMoved(event: PsiTreeChangeEvent) = onCodeChanged(event)
            /** Function to react when the children of a PSI element changed. */
            override fun childrenChanged(event: PsiTreeChangeEvent) = onCodeChanged(event)
        }, parent)
    }

    /** Function to catch up with edits made while the tab was closed, once it is visible again. */
    fun onShown() {
        if (staleWhileHidden && settings.values.autoRefresh) {
            staleWhileHidden = false
            refresh()
        }
    }

    /** Function to schedule a quiet rescan after a Java/Kotlin file changed, when auto-refresh is on. */
    private fun onCodeChanged(event: PsiTreeChangeEvent) {
        // During indexing the scan simply waits for smart mode, so no dumb-mode check is needed here.
        if (!settings.values.autoRefresh) return
        val file = event.file ?: event.child as? PsiFile ?: event.oldChild as? PsiFile ?: return
        if (file.name.substringAfterLast('.') !in SOURCE_EXTENSIONS) return
        alarm.cancelAllRequests()
        alarm.addRequest({
            // Closed tab: don't spend CPU now, catch up when it is shown again.
            if (isShowing()) refresh() else staleWhileHidden = true
        }, DELAY_MS)
    }

    private companion object {
        const val DELAY_MS = 1500
        val SOURCE_EXTENSIONS = setOf("java", "kt")
    }
}
