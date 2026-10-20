package com.jjkay03.classgraph.model

enum class NodeKind(val label: String) {
    CLASS("Class"),
    ABSTRACT("Abstract"),
    INTERFACE("Interface"),
    ENUM("Enum"),
    ANNOTATION("Annotation"),
    FILE("File"), // a Kotlin file: top-level declarations only, or nothing at all
}
