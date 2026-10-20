package com.jjkay03.classgraph.settings

import com.jjkay03.classgraph.model.EdgeKind
import com.jjkay03.classgraph.render.ColorMode

/** Persisted graph settings, mirroring Obsidian's Filters / Groups / Display / Forces sections. */
class GraphSettingsState {
    // Filters
    var search: String = ""
    var showOrphans: Boolean = true
    var kinds: MutableList<String> = EdgeKind.entries.filter { it.defaultEnabled }.map { it.name }.toMutableList()
    var localGraph: Boolean = false
    var localDepth: Int = 1

    // Groups
    var colorBy: ColorMode = ColorMode.NONE
    var groups: MutableList<ColorGroup> = mutableListOf()

    // Display
    var autoRefresh: Boolean = false
    var showArrows: Boolean = true
    var showIcons: Boolean = true
    var colorLinks: Boolean = false
    var textFade: Double = 0.75
    var nodeSize: Double = 1.0
    var linkThickness: Double = 1.0

    // Forces
    var centerForce: Double = 0.1
    var repelForce: Double = 1.0
    var linkForce: Double = 1.0
    var linkDistance: Double = 70.0

    /** Function to get the link types currently switched on. */
    fun enabledKinds(): Set<EdgeKind> = kinds.mapNotNull { name -> EdgeKind.entries.find { it.name == name } }.toSet()

    /** Function to switch one link type on or off. */
    fun setKind(kind: EdgeKind, enabled: Boolean) {
        kinds.remove(kind.name)
        if (enabled) kinds.add(kind.name)
    }
}
