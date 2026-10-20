package com.jjkay03.classgraph.ui

import com.intellij.pom.Navigatable
import com.jjkay03.classgraph.model.ClassNode
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import javax.swing.SwingUtilities
import kotlin.math.abs
import kotlin.math.pow

/**
 * Obsidian controls: drag a node (neighbours follow live), drag background to pan,
 * wheel to zoom, hover to highlight connections, click a node to open its source.
 */
class GraphMouseHandler(private val canvas: GraphCanvas) : MouseAdapter() {
    private var dragged: ClassNode? = null
    private var pressX = 0
    private var pressY = 0
    private var lastX = 0
    private var lastY = 0

    /** Function to attach this handler to the canvas mouse, motion and wheel events. */
    fun install() {
        canvas.addMouseListener(this)
        canvas.addMouseMotionListener(this)
        canvas.addMouseWheelListener(this)
    }

    /** Function to start dragging the node under the cursor, or start panning when there is none. */
    override fun mousePressed(e: MouseEvent) {
        pressX = e.x; pressY = e.y
        lastX = e.x; lastY = e.y
        val node = canvas.nodeAt(e.x, e.y)
        dragged = node.takeIf { SwingUtilities.isLeftMouseButton(e) }
        if (node != null && dragged != null) {
            node.pinned = true
            canvas.layout.setDragging(true)
            canvas.hover(node)
        }
    }

    /** Function to move the dragged node (neighbours follow live) or pan the view. */
    override fun mouseDragged(e: MouseEvent) {
        val node = dragged
        if (node != null) {
            node.x = canvas.viewport.worldX(e.x.toDouble())
            node.y = canvas.viewport.worldY(e.y.toDouble())
            node.vx = 0.0
            node.vy = 0.0
        } else {
            canvas.viewport.panBy(e.x - lastX, e.y - lastY)
        }
        lastX = e.x; lastY = e.y
        canvas.repaint()
    }

    /** Function to drop the node and open its source if the press was a click rather than a drag. */
    override fun mouseReleased(e: MouseEvent) {
        val node = dragged ?: return
        dragged = null
        node.pinned = false
        canvas.layout.setDragging(false)
        val isClick = abs(e.x - pressX) < CLICK_SLOP && abs(e.y - pressY) < CLICK_SLOP
        if (isClick && SwingUtilities.isLeftMouseButton(e)) {
            (node.pointer?.element as? Navigatable)?.navigate(true)
        }
    }

    /** Function to highlight the node under the cursor. */
    override fun mouseMoved(e: MouseEvent) {
        canvas.hover(canvas.nodeAt(e.x, e.y))
    }

    /** Function to zoom around the cursor position. */
    override fun mouseWheelMoved(e: MouseWheelEvent) {
        canvas.viewport.zoomAt(e.x, e.y, 1.1.pow(-e.preciseWheelRotation))
        canvas.repaint()
    }

    private companion object {
        const val CLICK_SLOP = 4
    }
}
