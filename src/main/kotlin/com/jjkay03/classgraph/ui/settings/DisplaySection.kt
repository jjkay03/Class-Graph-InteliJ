package com.jjkay03.classgraph.ui.settings

import com.intellij.ui.dsl.builder.Panel
import com.jjkay03.classgraph.settings.GraphSettings

/** Function to add the Display section: auto-refresh, arrows, icons, link colors, text fade, sizes. */
internal fun Panel.displaySection(settings: GraphSettings) {
    val s = settings.values
    collapsibleGroup("Display") {
        toggleRow("Auto-refresh", "Rescan automatically a moment after you edit, add or delete Java/Kotlin files. Paused while the Class Graph tab is closed; it catches up when you open it.", s.autoRefresh) {
            s.autoRefresh = it; settings.changed()
        }
        toggleRow("Arrows", "Draw an arrowhead on each link, pointing at the class that is depended on.", s.showArrows) {
            s.showArrows = it; settings.changed()
        }
        toggleRow("Icons in nodes", "Draw the IDE's class / interface / enum icon inside each node.", s.showIcons) {
            s.showIcons = it; settings.changed()
        }
        toggleRow("Color links by type", "Color each link by its relationship type (see the link colors under Filters) instead of grey.", s.colorLinks) {
            s.colorLinks = it; settings.changed()
        }
        sliderRow("Text fade threshold", "How far you must zoom in before class names appear. Higher means names show later.", 0.0, 1.0, 0.05, s.textFade) {
            s.textFade = it; settings.changed()
        }
        sliderRow("Node size", "Scales every node.", 0.3, 3.0, 0.05, s.nodeSize) { s.nodeSize = it; settings.changed() }
        sliderRow("Link thickness", "Scales the width of every link.", 0.5, 5.0, 0.1, s.linkThickness) { s.linkThickness = it; settings.changed() }
    }
}
