package com.jjkay03.classgraph.model

class ClassGraph(val nodes: List<ClassNode>, val edges: List<ClassEdge>) {
    companion object {
        val EMPTY = ClassGraph(emptyList(), emptyList())
    }
}
