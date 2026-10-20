package com.jjkay03.classgraph.ui.settings

import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.ColorIcon
import com.jjkay03.classgraph.render.LegendEntry
import com.jjkay03.classgraph.ui.safeTooltip
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.SwingConstants

/** Lists what each node color stands for. Hidden when nothing is colored. */
class ColorLegend : JPanel() {
    private var lastSignature: String? = null

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createEmptyBorder(4, 0, 0, 0)
        isVisible = false
    }

    /** Function to redraw the legend rows (capped) for [entries]; hides the legend when empty. */
    fun refresh(entries: List<LegendEntry>) {
        // Settings fire on every slider tick; skip the rebuild when nothing in the legend changed.
        val signature = entries.joinToString("|") { "${it.label}:${it.color.rgb}" }
        if (signature == lastSignature) return
        lastSignature = signature

        removeAll()
        if (entries.isNotEmpty()) {
            add(JBLabel("<html><b>Legend</b></html>"))
            entries.take(MAX_ROWS).forEach { entry ->
                add(JBLabel(entry.label, ColorIcon(12, entry.color), SwingConstants.LEFT).apply {
                    toolTipText = safeTooltip(entry.label)
                })
            }
            if (entries.size > MAX_ROWS) add(JBLabel("+ ${entries.size - MAX_ROWS} more"))
        }
        isVisible = entries.isNotEmpty()
        revalidateAncestors()
    }

    private companion object {
        const val MAX_ROWS = 40
    }
}
