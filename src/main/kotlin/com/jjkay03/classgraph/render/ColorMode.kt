package com.jjkay03.classgraph.render

enum class ColorMode(val label: String) {
    NONE("None"),
    PACKAGE("Package"),
    MODULE("Module"),
    KIND("Kind");

    /** Function to show the readable label in combo boxes. */
    override fun toString() = label
}
