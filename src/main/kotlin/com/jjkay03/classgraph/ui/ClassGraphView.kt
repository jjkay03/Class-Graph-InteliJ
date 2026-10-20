package com.jjkay03.classgraph.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jjkay03.classgraph.settings.GraphSettings
import com.jjkay03.classgraph.ui.settings.ColorLegend
import com.jjkay03.classgraph.ui.settings.GraphSettingsPanel
import java.awt.BorderLayout
import java.awt.event.HierarchyEvent
import javax.swing.JPanel

/**
 * Tool window content: toolbar on top, graph canvas in the middle, collapsible settings on the right.
 * This class only wires the pieces together; scanning, auto-refresh and editor tracking live in their own classes.
 */
class ClassGraphView(project: Project) : JPanel(BorderLayout()), Disposable {
    private val settings = project.service<GraphSettings>()
    private val canvas = GraphCanvas(settings)
    private val legend = ColorLegend()
    private val settingsPanel = GraphSettingsPanel(settings, legend).apply { isVisible = false }
    private val toolbar: GraphToolbar = GraphToolbar({ toggleSettings(it) }) { reload() }

    private val scanner: GraphScanner = GraphScanner(project, this, { toolbar.setStatus(it) }, { canvas.setGraph(it) })
    private val autoRefresh: AutoRefresh = AutoRefresh(project, this, settings, { isShowing }) { reload(silent = true) }

    init {
        add(toolbar, BorderLayout.NORTH)
        add(canvas, BorderLayout.CENTER)
        add(settingsPanel, BorderLayout.EAST)

        canvas.onStatsChanged = { toolbar.setStatus(canvas.stats) }
        canvas.onGraphLoaded = { refreshLegend() }
        settings.addListener(this) {
            canvas.onSettingsChanged()
            refreshLegend()
        }

        // The highlighted node and the local graph follow the file open in the editor.
        EditorFileTracker(project, this) { canvas.setActiveFile(it) }

        addHierarchyListener { event ->
            if (event.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && isShowing) autoRefresh.onShown()
        }
    }

    /** Function to rescan the project; [silent] scans (automatic refreshes) keep the toolbar status quiet. */
    fun reload(silent: Boolean = false) {
        scanner.scan(silent)
    }

    /** Function to bring the settings legend in line with the current colors. */
    private fun refreshLegend() {
        legend.refresh(canvas.legendEntries())
    }

    /** Function to show or hide the settings side panel. */
    private fun toggleSettings(show: Boolean) {
        settingsPanel.isVisible = show
        revalidate()
        repaint()
    }

    /** Function to release resources; scanner, alarm and listeners are disposed through this view. */
    override fun dispose() {}
}
