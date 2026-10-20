package com.jjkay03.classgraph.render

import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.jjkay03.classgraph.filter.VisibleGraph
import com.jjkay03.classgraph.model.ClassNode
import com.jjkay03.classgraph.settings.GraphSettingsState
import java.awt.AlphaComposite
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.Polygon
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

/** Paints a [VisibleGraph] the way Obsidian does: hovering fades everything except the node and its links. */
class GraphRenderer {

    /** Everything needed to draw one frame. */
    class Frame(
        val graph: VisibleGraph,
        val viewport: Viewport,
        val hovered: ClassNode?,
        val active: ClassNode?,
        val display: GraphSettingsState,
    )

    /** Function to draw links then nodes. There is no background fill: the IDE's panel background shows through. */
    fun paint(g: Graphics2D, width: Int, height: Int, frame: Frame) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        paintEdges(g, frame)
        paintNodes(g, frame, width, height)
    }

    /** Function to draw every link between circle edges, highlighting the ones touching the hovered node. */
    private fun paintEdges(g: Graphics2D, frame: Frame) {
        val vp = frame.viewport
        val d = frame.display
        val focus = frame.hovered

        for (e in frame.graph.edges) {
            val touches = focus != null && (e.from === focus || e.to === focus)
            val base = when {
                d.colorLinks -> e.kind.color
                touches -> ACCENT
                else -> LINK
            }
            val alpha = when {
                touches -> 235
                focus != null -> 14
                else -> 100
            }
            val cx1 = vp.screenX(e.from.x)
            val cy1 = vp.screenY(e.from.y)
            val cx2 = vp.screenX(e.to.x)
            val cy2 = vp.screenY(e.to.y)
            val r1 = e.from.radius(d.nodeSize) * vp.scale
            val r2 = e.to.radius(d.nodeSize) * vp.scale
            val dist = hypot(cx2 - cx1, cy2 - cy1)
            if (dist <= r1 + r2) continue // circles overlap: nothing to draw between them

            // Start and end on the circle edges so lines and arrowheads never run inside a node.
            val ux = (cx2 - cx1) / dist
            val uy = (cy2 - cy1) / dist
            val x1 = cx1 + ux * r1
            val y1 = cy1 + uy * r1
            val x2 = cx2 - ux * r2
            val y2 = cy2 - uy * r2

            g.color = withAlpha(base, alpha)
            g.stroke = BasicStroke((d.linkThickness * (if (touches) 1.6 else 1.0)).toFloat())
            g.drawLine(x1.roundToInt(), y1.roundToInt(), x2.roundToInt(), y2.roundToInt())
            if (d.showArrows) paintArrowHead(g, x1, y1, x2, y2)
        }
    }

    /** Function to draw node circles, kind icons, the active-file ring and zoom-faded labels. */
    private fun paintNodes(g: Graphics2D, frame: Frame, width: Int, height: Int) {
        val vp = frame.viewport
        val d = frame.display
        val focus = frame.hovered
        val neighbours = focus?.let { frame.graph.adjacency[it] }.orEmpty()
        val fadeStart = d.textFade * 2.0

        g.font = g.font.deriveFont(11f)
        for (n in frame.graph.nodes) {
            val r = n.radius(d.nodeSize) * vp.scale
            val sx = vp.screenX(n.x)
            val sy = vp.screenY(n.y)
            if (sx < -50 || sy < -50 || sx > width + 50 || sy > height + 50) continue

            val lit = focus == null || n === focus || n in neighbours
            g.color = withAlpha(n.color ?: DEFAULT_NODE, if (lit) NODE_ALPHA else NODE_FADED_ALPHA)
            g.fill(Ellipse2D.Double(sx - r, sy - r, r * 2, r * 2))

            if (d.showIcons && r >= MIN_ICON_RADIUS) paintKindIcon(g, n, sx, sy, r, lit)

            if (n === frame.active) {
                g.color = ACCENT
                g.stroke = BasicStroke(2f)
                g.draw(Ellipse2D.Double(sx - r - 2.5, sy - r - 2.5, r * 2 + 5, r * 2 + 5))
            }

            // Labels fade in with zoom (Obsidian's "text fade threshold"); hovering shows the neighbourhood's labels.
            val labelOpacity = when {
                focus != null -> if (lit) 1.0 else 0.0
                else -> ((vp.scale - fadeStart) / 0.35).coerceIn(0.0, 1.0)
            }
            if (labelOpacity > 0.02) {
                g.color = withAlpha(LABEL, (labelOpacity * 255).roundToInt())
                val w = g.fontMetrics.stringWidth(n.name)
                g.drawString(n.name, (sx - w / 2).toFloat(), (sy + r + 12).toFloat())
            }
        }
    }

    /** Function to draw the IDE's class/interface/enum icon, scaled to sit inside the node circle. */
    private fun paintKindIcon(g: Graphics2D, n: ClassNode, sx: Double, sy: Double, r: Double, lit: Boolean) {
        val icon = NodeIcons.forKind(n.kind)
        val scale = r * ICON_FILL / icon.iconWidth
        val oldTransform = g.transform
        val oldComposite = g.composite
        g.translate(sx, sy)
        g.scale(scale, scale)
        if (!lit) g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f)
        icon.paintIcon(null, g, -icon.iconWidth / 2, -icon.iconHeight / 2)
        g.transform = oldTransform
        g.composite = oldComposite
    }

    /** Function to draw an arrowhead whose tip is exactly at (x2, y2), the edge of the target circle. */
    private fun paintArrowHead(g: Graphics2D, x1: Double, y1: Double, x2: Double, y2: Double) {
        val angle = atan2(y2 - y1, x2 - x1)
        val size = 7.0
        g.fill(
            Polygon(
                intArrayOf(x2.roundToInt(), (x2 - size * cos(angle - 0.4)).roundToInt(), (x2 - size * cos(angle + 0.4)).roundToInt()),
                intArrayOf(y2.roundToInt(), (y2 - size * sin(angle - 0.4)).roundToInt(), (y2 - size * sin(angle + 0.4)).roundToInt()),
                3,
            )
        )
    }

    /** Function to make [color] translucent: [alpha] is 0..255. */
    private fun withAlpha(color: JBColor, alpha: Int) = ColorUtil.withAlpha(color, alpha.coerceIn(0, 255) / 255.0)

    private companion object {
        // JBColor(light theme, dark theme) so everything follows the IDE theme.
        val ACCENT = JBColor(0x8F6BF0, 0xA88BFA)
        val LINK = JBColor(0x888888, 0x8A8A8A)
        val DEFAULT_NODE = JBColor(0x6B6B6B, 0xA8A8A8)
        val LABEL = JBColor(0x222222, 0xDDDDDD)
        const val NODE_ALPHA = 128 // 50% opacity circles
        const val NODE_FADED_ALPHA = 20
        const val MIN_ICON_RADIUS = 5.0
        const val ICON_FILL = 1.5 // icon diameter as a multiple of the node radius
    }
}
