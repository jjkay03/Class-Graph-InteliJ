package com.jjkay03.classgraph.ui.settings

import com.intellij.ui.dsl.builder.Panel
import com.jjkay03.classgraph.settings.GraphSettings

/** Function to add the Forces section: center, repel, link force and link distance. */
internal fun Panel.forcesSection(settings: GraphSettings) {
    val s = settings.values
    collapsibleGroup("Forces") {
        sliderRow("Center force", "Pulls every node toward the middle. Lower lets the graph spread out.", 0.0, 1.0, 0.01, s.centerForce) {
            s.centerForce = it; settings.changed()
        }
        sliderRow("Repel force", "How strongly nodes push each other apart.", 0.0, 3.0, 0.05, s.repelForce) {
            s.repelForce = it; settings.changed()
        }
        sliderRow("Link force", "How strongly links pull the classes they connect together.", 0.0, 3.0, 0.05, s.linkForce) {
            s.linkForce = it; settings.changed()
        }
        sliderRow("Link distance", "Preferred length of a link, in pixels.", 20.0, 300.0, 5.0, s.linkDistance) {
            s.linkDistance = it; settings.changed()
        }
    }
}
