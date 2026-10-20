package com.jjkay03.classgraph.layout

import com.jjkay03.classgraph.model.ClassEdge
import com.jjkay03.classgraph.model.ClassNode
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * d3-force style simulation: alpha decays toward `alphaTarget`, so while a node is being dragged
 * (target 0.3) the whole neighbourhood keeps reacting live, and settles after release.
 */
class ForceLayout {
    private var alpha = 1.0
    private var alphaTarget = 0.0

    val settled: Boolean get() = alpha < MIN_ALPHA && alphaTarget == 0.0

    /** Function to start over with full simulation energy. */
    fun restart() { alpha = 1.0 }

    /** Function to make sure at least [energy] is left, e.g. after a rescan or force change. */
    fun reheat(energy: Double = 0.5) { alpha = max(alpha, energy) }

    /** Keep the simulation hot while the user drags a node. */
    fun setDragging(dragging: Boolean) {
        alphaTarget = if (dragging) DRAG_ALPHA else 0.0
        if (dragging) reheat(DRAG_ALPHA)
    }

    /** Function to advance the simulation one frame: repulsion, springs, gravity, then movement. */
    fun step(nodes: List<ClassNode>, edges: List<ClassEdge>, forces: Forces) {
        if (nodes.isEmpty() || settled) return
        alpha += (alphaTarget - alpha) * ALPHA_DECAY
        applyRepulsion(nodes, forces.repel)
        applySprings(edges, forces)
        integrate(nodes, forces.center)
    }

    /** Function to push every nearby pair of nodes apart. */
    private fun applyRepulsion(nodes: List<ClassNode>, repel: Double) {
        val strength = REPULSION * repel * alpha
        for (i in nodes.indices) {
            val a = nodes[i]
            for (j in i + 1 until nodes.size) {
                val b = nodes[j]
                val dx = b.x - a.x
                val dy = b.y - a.y
                val d2 = dx * dx + dy * dy + 0.01
                if (d2 > REPULSION_RANGE_SQ) continue
                val d = sqrt(d2)
                val f = strength / d2
                val fx = dx / d * f
                val fy = dy / d * f
                a.vx -= fx; a.vy -= fy
                b.vx += fx; b.vy += fy
            }
        }
    }

    /** Function to pull linked nodes toward the preferred link distance. */
    private fun applySprings(edges: List<ClassEdge>, forces: Forces) {
        for (e in edges) {
            val a = e.from
            val b = e.to
            val dx = b.x - a.x
            val dy = b.y - a.y
            val d = sqrt(dx * dx + dy * dy + 0.01)
            val k = SPRING * forces.link / min(a.degree, b.degree).coerceAtLeast(1)
            val f = (d - forces.linkDistance) * k * alpha
            val fx = dx / d * f
            val fy = dy / d * f
            a.vx += fx; a.vy += fy
            b.vx -= fx; b.vy -= fy
        }
    }

    /** Function to apply center gravity and damping, then move every node that is not pinned. */
    private fun integrate(nodes: List<ClassNode>, center: Double) {
        val gravity = center * GRAVITY_SCALE * alpha
        for (n in nodes) {
            n.vx -= n.x * gravity
            n.vy -= n.y * gravity
            if (n.pinned) continue
            n.vx *= VELOCITY_DECAY
            n.vy *= VELOCITY_DECAY
            n.x += n.vx
            n.y += n.vy
        }
    }

    private companion object {
        const val MIN_ALPHA = 0.001
        const val ALPHA_DECAY = 0.0228
        const val DRAG_ALPHA = 0.3
        const val REPULSION = 1500.0
        const val REPULSION_RANGE_SQ = 250_000.0
        const val SPRING = 0.3
        const val GRAVITY_SCALE = 0.1
        const val VELOCITY_DECAY = 0.6
    }
}
