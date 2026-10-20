package com.jjkay03.classgraph.model

import com.intellij.ui.JBColor

/** Relationship types. An edge always points from the dependent class to the class it depends on. */
enum class EdgeKind(val label: String, val color: JBColor, val defaultEnabled: Boolean) {
    INHERITANCE("Inheritance", JBColor(0x4FC3F7, 0x4FC3F7), true),
    FIELD("Fields", JBColor(0x81C784, 0x81C784), true),
    SIGNATURE("Signatures", JBColor(0xBA68C8, 0xBA68C8), false),
    INSTANTIATION("Instantiation", JBColor(0xFFB74D, 0xFFB74D), true),
    CALL("Calls", JBColor(0x90A4AE, 0x90A4AE), false),
}
