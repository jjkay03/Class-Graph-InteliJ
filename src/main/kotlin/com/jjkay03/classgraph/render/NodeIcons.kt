package com.jjkay03.classgraph.render

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.ui.JBColor
import com.intellij.util.ui.ImageUtil
import com.jjkay03.classgraph.model.NodeKind
import java.awt.image.BufferedImage
import javax.swing.Icon
import kotlin.math.max
import kotlin.math.min

/** The IDE's own class-kind icons, so nodes look like the Project tree. */
object NodeIcons {
    private const val MIN_OPACITY = 200
    private const val MIN_SATURATION = 0.25

    /** Function to get the IDE icon that represents [kind]. */
    fun forKind(kind: NodeKind): Icon = when (kind) {
        NodeKind.CLASS -> AllIcons.Nodes.Class
        NodeKind.ABSTRACT -> AllIcons.Nodes.AbstractClass
        NodeKind.INTERFACE -> AllIcons.Nodes.Interface
        NodeKind.ENUM -> AllIcons.Nodes.Enum
        NodeKind.ANNOTATION -> AllIcons.Nodes.Annotationtype
        NodeKind.FILE -> FileTypeManager.getInstance().getFileTypeByExtension("kt").icon ?: AllIcons.FileTypes.Any_type
    }

    /**
     * Function to find the icon's dominant colour in the current theme, so "Kind" colouring matches what the
     * icons look like. Computed on each call (cheap: one 16px render) so it follows theme changes.
     */
    fun colorOf(kind: NodeKind): JBColor {
        val icon = forKind(kind)
        val w = icon.iconWidth.coerceAtLeast(1)
        val h = icon.iconHeight.coerceAtLeast(1)
        val image = ImageUtil.createImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        icon.paintIcon(null, graphics, 0, 0)
        graphics.dispose()

        // Bucket saturated, mostly-opaque pixels by coarse colour and take the biggest bucket's average.
        class Bucket { var count = 0; var red = 0L; var green = 0L; var blue = 0L }
        val buckets = HashMap<Int, Bucket>()
        for (y in 0 until h) for (x in 0 until w) {
            val argb = image.getRGB(x, y)
            if ((argb ushr 24) < MIN_OPACITY) continue
            val red = (argb shr 16) and 0xFF
            val green = (argb shr 8) and 0xFF
            val blue = argb and 0xFF
            if (saturation(red, green, blue) < MIN_SATURATION) continue
            val key = (red shr 5 shl 6) or (green shr 5 shl 3) or (blue shr 5)
            buckets.getOrPut(key) { Bucket() }.apply { count++; this.red += red; this.green += green; this.blue += blue }
        }
        val best = buckets.values.maxByOrNull { it.count } ?: return JBColor.GRAY
        val rgb = ((best.red / best.count).toInt() shl 16) or ((best.green / best.count).toInt() shl 8) or (best.blue / best.count).toInt()
        return JBColor(rgb, rgb)
    }

    /** Function to compute HSB saturation (0..1) of an RGB colour. */
    private fun saturation(red: Int, green: Int, blue: Int): Double {
        val hi = max(red, max(green, blue))
        val lo = min(red, min(green, blue))
        return if (hi == 0) 0.0 else (hi - lo).toDouble() / hi
    }
}
