package com.jjkay03.classgraph.filter

import com.jjkay03.classgraph.model.ClassEdge
import com.jjkay03.classgraph.model.ClassNode

/** The subset of a graph currently on screen, plus undirected adjacency for highlighting. */
class VisibleGraph(
    val nodes: List<ClassNode>,
    val edges: List<ClassEdge>,
    val adjacency: Map<ClassNode, Set<ClassNode>>,
) {
    companion object {
        val EMPTY = VisibleGraph(emptyList(), emptyList(), emptyMap())
    }
}
