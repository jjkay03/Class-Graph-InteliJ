package com.jjkay03.classgraph.ui.settings

import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.jjkay03.classgraph.render.ColorMode
import com.jjkay03.classgraph.settings.GraphSettings

/** Function to add the Groups section: auto-color mode, custom color groups and the legend. */
internal fun Panel.groupsSection(settings: GraphSettings, legend: ColorLegend) {
    val s = settings.values
    collapsibleGroup("Groups") {
        row("Auto-color:") {
            comboBox(ColorMode.entries).applyToComponent {
                selectedItem = s.colorBy
                toolTipText = "Color every class by its package, module or kind. Custom groups below take priority."
                addActionListener {
                    s.colorBy = selectedItem as ColorMode
                    settings.changed()
                }
            }
        }
        row { cell(ColorGroupsEditor(settings)).align(AlignX.FILL) }
        row { cell(legend).align(AlignX.FILL) }
    }
}
