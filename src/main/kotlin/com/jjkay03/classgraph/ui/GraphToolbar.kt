package com.jjkay03.classgraph.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JToggleButton

/** Slim top bar: settings toggle, reload and the class/link counts. */
class GraphToolbar(onToggleSettings: (Boolean) -> Unit, onReload: () -> Unit) : JPanel(FlowLayout(FlowLayout.LEFT, 6, 2)) {
    private val status = JBLabel("")

    init {
        add(JToggleButton(AllIcons.General.GearPlain).apply {
            toolTipText = "Graph settings"
            addActionListener { onToggleSettings(isSelected) }
        })
        add(JButton(AllIcons.Actions.Refresh).apply {
            toolTipText = "Rescan project"
            addActionListener { onReload() }
        })
        add(status)
    }

    /** Function to show [text] (scan progress or counts) next to the toolbar buttons. */
    fun setStatus(text: String) { status.text = text }
}
