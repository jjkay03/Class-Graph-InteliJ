package com.jjkay03.classgraph.ui.settings

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Rectangle
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.Scrollable

/** Wraps [content] so a scroll pane gives it exactly the viewport width: it wraps instead of being cut off. */
class WidthTrackingPanel(content: JComponent) : JPanel(BorderLayout()), Scrollable {
    init {
        add(content, BorderLayout.NORTH)
    }

    /** Function to report the preferred viewport size. */
    override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
    /** Function to set how far one scroll tick moves. */
    override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 16
    /** Function to set how far a page scroll moves. */
    override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) = 64
    /** Function to make the content match the viewport width so nothing is cut off on the right. */
    override fun getScrollableTracksViewportWidth() = true
    /** Function to let the content grow taller than the viewport and scroll. */
    override fun getScrollableTracksViewportHeight() = false
}
