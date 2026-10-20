package com.jjkay03.classgraph.filter

import com.jjkay03.classgraph.model.ClassNode
import com.jjkay03.classgraph.model.EdgeKind

/** What to show. [focus] (the local graph's center) limits the graph to nodes within [focusDepth] links. */
data class GraphFilter(
    val kinds: Set<EdgeKind>,
    val showOrphans: Boolean,
    val search: String,
    val focus: ClassNode?,
    val focusDepth: Int,
    val minLinks: Int = 0,
)
