package com.jjkay03.classgraph.ui.settings

import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.jjkay03.classgraph.settings.GraphSettings
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

/** Obsidian-style collapsible settings: Filters, Groups, Display, Forces, plus a reset button at the bottom. */
class GraphSettingsPanel(
    private val settings: GraphSettings,
    private val legend: ColorLegend,
) : JBScrollPane(VERTICAL_SCROLLBAR_AS_NEEDED, HORIZONTAL_SCROLLBAR_NEVER) {

    init {
        border = null
        preferredSize = Dimension(WIDTH, 0)
        setViewportView(buildContent())
    }

    /** Function to build the four sections and the reset button; also used to refresh every control after a reset. */
    private fun buildContent(): JComponent {
        val content = panel {
            filtersSection(settings)
            groupsSection(settings, legend)
            displaySection(settings)
            forcesSection(settings)
            separator()
            row {
                button("Reset settings") { confirmAndReset() }.applyToComponent {
                    toolTipText = "Restore every filter, group, display and force setting to its default."
                }
            }
        }
        return WidthTrackingPanel(content)
    }

    /** Function to ask for confirmation, restore the defaults and rebuild the controls so they show the new values. */
    private fun confirmAndReset() {
        val answer = Messages.showYesNoDialog(
            "Restore all Class Graph settings to their defaults?",
            "Reset Settings",
            Messages.getQuestionIcon(),
        )
        if (answer != Messages.YES) return
        settings.resetToDefaults()
        setViewportView(buildContent())
    }

    private companion object {
        const val WIDTH = 330
    }
}
