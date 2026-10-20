package com.jjkay03.classgraph.filter

import com.jjkay03.classgraph.model.ClassGraph
import com.jjkay03.classgraph.model.ClassNode

object GraphFilterer {

    /** Applies [filter] to [graph] and refreshes each node's `degree` to match the visible edges. */
    fun apply(graph: ClassGraph, filter: GraphFilter): VisibleGraph {
        val matching = graph.nodes.filter { NodeQuery.matches(it, filter.search) }
        val matchingSet = matching.toHashSet()
        val edges = graph.edges.filter { it.kind in filter.kinds && it.from in matchingSet && it.to in matchingSet }

        val adjacency = HashMap<ClassNode, MutableSet<ClassNode>>()
        for (e in edges) {
            adjacency.getOrPut(e.from) { HashSet() }.add(e.to)
            adjacency.getOrPut(e.to) { HashSet() }.add(e.from)
        }

        val focus = filter.focus
        val keep = focus?.let { neighbourhood(it, adjacency, filter.focusDepth) }
        val nodes = matching.filter { node ->
            (filter.showOrphans || adjacency.containsKey(node) || node === focus) &&
                (keep == null || node in keep)
        }

        val nodeSet = nodes.toHashSet()
        val visibleEdges = edges.filter { it.from in nodeSet && it.to in nodeSet }

        graph.nodes.forEach { it.degree = 0 }
        visibleEdges.forEach { it.from.degree++; it.to.degree++ }

        val visibleAdjacency = adjacency.mapValues { (_, v) -> v.filter { it in nodeSet }.toSet() }
        return VisibleGraph(nodes, visibleEdges, visibleAdjacency)
    }

    /** Function to collect every node within [depth] links of [start], including [start]. */
    private fun neighbourhood(start: ClassNode, adjacency: Map<ClassNode, Set<ClassNode>>, depth: Int): Set<ClassNode> {
        val seen = HashSet<ClassNode>().apply { add(start) }
        var frontier = setOf(start)
        repeat(depth) {
            frontier = frontier.flatMap { adjacency[it].orEmpty() }.filter { seen.add(it) }.toSet()
        }
        return seen
    }
}
