package com.jjkay03.classgraph.ui

import com.jjkay03.classgraph.filter.GraphFilter
import com.jjkay03.classgraph.filter.GraphFilterer
import com.jjkay03.classgraph.filter.VisibleGraph
import com.jjkay03.classgraph.layout.ForceLayout
import com.jjkay03.classgraph.layout.Forces
import com.jjkay03.classgraph.model.ClassGraph
import com.jjkay03.classgraph.model.ClassNode
import com.jjkay03.classgraph.render.GraphColorizer
import com.jjkay03.classgraph.render.GraphRenderer
import com.jjkay03.classgraph.render.LegendEntry
import com.jjkay03.classgraph.render.Viewport
import com.jjkay03.classgraph.settings.GraphSettings
import java.awt.Cursor
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.JPanel
import javax.swing.Timer
import kotlin.random.Random

/** Holds graph/view state and wires settings → filter → layout → renderer. Input lives in [GraphMouseHandler]. */
class GraphCanvas(private val settings: GraphSettings) : JPanel() {

    val viewport = Viewport()
    val layout = ForceLayout()
    private val renderer = GraphRenderer()
    private val timer = Timer(16) { tick() }

    private var graph = ClassGraph.EMPTY
    private var visible = VisibleGraph.EMPTY
    private var hovered: ClassNode? = null
    private var activePath: String? = null
    private var activeNode: ClassNode? = null
    private var gentleRestart = false
    private var lastFilter: GraphFilter? = null
    private var lastForces: Forces? = null

    var onStatsChanged: (() -> Unit)? = null
    var onGraphLoaded: (() -> Unit)? = null
    val stats: String get() = "${visible.nodes.size} classes · ${visible.edges.size} links"

    val nodeSize: Double get() = settings.values.nodeSize

    private companion object {
        const val NEW_NODE_SPREAD = 80.0
        const val GENTLE_ENERGY = 0.3
    }

    init {
        isOpaque = false
        toolTipText = ""
        GraphMouseHandler(this).install()
    }

    /** Function to start the animation timer when the canvas becomes part of a window. */
    override fun addNotify() { super.addNotify(); timer.start() }
    /** Function to stop the animation timer when the canvas leaves its window. */
    override fun removeNotify() { timer.stop(); super.removeNotify() }

    /**
     * Shows a (re)scanned graph. On a rescan nodes keep their positions and the view stays put, so the graph
     * updates in place while you work instead of jumping.
     */
    fun setGraph(newGraph: ClassGraph) {
        val rescan = graph.nodes.isNotEmpty()
        if (rescan) {
            carryOverLayout(graph, newGraph)
            hovered = hovered?.let { h -> newGraph.nodes.firstOrNull { it.id == h.id } }
        } else {
            hovered = null
            viewport.resetCentering()
        }
        graph = newGraph
        activeNode = resolveActiveNode()
        gentleRestart = rescan
        lastFilter = null
        onSettingsChanged()
        onGraphLoaded?.invoke()
    }

    /** Function to copy positions of nodes that still exist and spawn new ones near the middle. */
    private fun carryOverLayout(old: ClassGraph, new: ClassGraph) {
        val previous = old.nodes.associateBy { it.id }
        for (n in new.nodes) {
            val p = previous[n.id]
            if (p != null) {
                n.x = p.x; n.y = p.y; n.vx = p.vx; n.vy = p.vy
            } else {
                // Brand new class: appear near the middle and let the forces place it.
                n.x = Random.nextDouble(-NEW_NODE_SPREAD, NEW_NODE_SPREAD)
                n.y = Random.nextDouble(-NEW_NODE_SPREAD, NEW_NODE_SPREAD)
            }
        }
    }

    /** What each node color currently means, for the settings legend. */
    fun legendEntries(): List<LegendEntry> =
        GraphColorizer.legend(graph.nodes, settings.values.colorBy, settings.values.groups)

    /** The file open in the editor; drives the highlighted node and the local graph. */
    fun setActiveFile(path: String?) {
        activePath = path
        val node = resolveActiveNode()
        if (node === activeNode) return
        activeNode = node
        onSettingsChanged()
    }

    /** Re-reads settings: refilters only if the filter changed, otherwise recolors/repaints. */
    fun onSettingsChanged() {
        val filter = currentFilter()
        if (filter != lastFilter) {
            lastFilter = filter
            visible = GraphFilterer.apply(graph, filter)
            if (gentleRestart) layout.reheat(GENTLE_ENERGY) else layout.restart()
            gentleRestart = false
            onStatsChanged?.invoke()
        }
        recolor()
        val forces = currentForces()
        if (forces != lastForces) {
            if (lastForces != null) layout.reheat()
            lastForces = forces
        }
        repaint()
    }

    /** Function to find the topmost node at a screen point, or null when there is none. */
    fun nodeAt(screenX: Int, screenY: Int): ClassNode? {
        for (n in visible.nodes.asReversed()) {
            val r = n.radius(nodeSize) * viewport.scale + 3
            val dx = viewport.screenX(n.x) - screenX
            val dy = viewport.screenY(n.y) - screenY
            if (dx * dx + dy * dy <= r * r) return n
        }
        return null
    }

    /** Function to highlight [node] (null clears it) and update the tooltip and mouse cursor. */
    fun hover(node: ClassNode?) {
        if (node === hovered) return
        hovered = node
        toolTipText = node?.let { safeTooltip("${it.name} · ${it.degree} links") }
        cursor = if (node != null) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()
        repaint()
    }

    /** Function to build the graph filter from the current settings. */
    private fun currentFilter(): GraphFilter {
        val s = settings.values
        return GraphFilter(s.enabledKinds(), s.showOrphans, s.search.trim(), if (s.localGraph) activeNode else null, s.localDepth)
    }

    /** Function to read the force sliders from the current settings. */
    private fun currentForces(): Forces {
        val s = settings.values
        return Forces(s.centerForce, s.repelForce, s.linkForce, s.linkDistance)
    }

    /** Function to recolor the visible nodes from the groups and the auto-color mode. */
    private fun recolor() {
        val s = settings.values
        GraphColorizer.apply(graph.nodes, visible.nodes, s.colorBy, s.groups)
    }

    /** Function to find the node of the file open in the editor. */
    private fun resolveActiveNode(): ClassNode? {
        val path = activePath ?: return null
        val inFile = graph.nodes.filter { it.filePath == path }
        val fileName = path.substringAfterLast('/').substringBeforeLast('.')
        return inFile.firstOrNull { it.name == fileName } ?: inFile.firstOrNull()
    }

    /** Function to advance the physics one frame while the canvas is visible and not yet settled. */
    private fun tick() {
        if (!isShowing || layout.settled) return
        layout.step(visible.nodes, visible.edges, currentForces())
        repaint()
    }

    /** Function to draw the current frame. */
    override fun paintComponent(g: Graphics) {
        viewport.centerOnce(width, height)
        renderer.paint(g as Graphics2D, width, height, GraphRenderer.Frame(visible, viewport, hovered, activeNode, settings.values))
    }
}
