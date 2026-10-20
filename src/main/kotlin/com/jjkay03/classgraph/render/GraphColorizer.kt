package com.jjkay03.classgraph.render

import com.intellij.ui.JBColor
import com.jjkay03.classgraph.filter.NodeQuery
import com.jjkay03.classgraph.model.ClassNode
import com.jjkay03.classgraph.model.NodeKind
import com.jjkay03.classgraph.settings.ColorGroup
import kotlin.math.abs
import kotlin.math.floor

object GraphColorizer {
    private const val GOLDEN_RATIO = 0.61803398875
    private const val AUTO_SATURATION = 0.55
    private const val AUTO_BRIGHTNESS = 0.95

    /**
     * Function to color [visible] nodes: first matching [groups] entry wins, then the [mode] auto color,
     * else null (default color). [all] is used to assign auto hues so a group keeps its color when filters hide others.
     */
    fun apply(all: List<ClassNode>, visible: List<ClassNode>, mode: ColorMode, groups: List<ColorGroup>) {
        val activeGroups = groups.filter { it.query.isNotBlank() }
        val key = keyOf(mode)
        val hueIndex = key?.let { k -> indexByValue(all, k) }
        val kindColors = if (mode == ColorMode.KIND) NodeKind.entries.associateWith { NodeIcons.colorOf(it) } else emptyMap()

        for (n in visible) {
            val group = activeGroups.firstOrNull { NodeQuery.matches(n, it.query) }
            n.color = when {
                group != null -> JBColor(group.rgb, group.rgb)
                mode == ColorMode.KIND -> kindColors[n.kind]
                key != null && hueIndex != null -> autoColor(hueIndex[key(n)] ?: 0)
                else -> null
            }
        }
    }

    /** Function to list what each color currently means: custom groups first, then auto-color values with class counts. */
    fun legend(all: List<ClassNode>, mode: ColorMode, groups: List<ColorGroup>): List<LegendEntry> {
        val entries = groups
            .filter { it.query.isNotBlank() }
            .mapTo(ArrayList()) { LegendEntry(it.query, JBColor(it.rgb, it.rgb)) }

        val key = keyOf(mode)
        when {
            mode == ColorMode.KIND -> {
                val counts = all.groupingBy { it.kind }.eachCount()
                counts.entries.sortedBy { it.key.ordinal }
                    .forEach { entries.add(LegendEntry("${it.key.label} (${it.value})", NodeIcons.colorOf(it.key))) }
            }
            key != null -> {
                val index = indexByValue(all, key)
                val counts = all.groupingBy(key).eachCount()
                index.entries.sortedBy { it.value }
                    .forEach { entries.add(LegendEntry("${it.key} (${counts[it.key]})", autoColor(it.value))) }
            }
        }
        return entries
    }

    /** Function to pick which node attribute the auto-color mode groups by, or null when it doesn't group. */
    private fun keyOf(mode: ColorMode): ((ClassNode) -> String)? = when (mode) {
        ColorMode.PACKAGE -> { n -> n.pkg }
        ColorMode.MODULE -> { n -> n.module }
        else -> null
    }

    /** Function to number each distinct value of [key] (sorted), so every value gets a stable hue. */
    private fun indexByValue(all: List<ClassNode>, key: (ClassNode) -> String): Map<String, Int> =
        all.map(key).distinct().sorted().withIndex().associate { it.value to it.index }

    /** Function to turn a group number into a well-spread hue using the golden ratio. */
    private fun autoColor(index: Int): JBColor {
        val rgb = hsbToRgb((index * GOLDEN_RATIO) % 1.0, AUTO_SATURATION, AUTO_BRIGHTNESS)
        return JBColor(rgb, rgb)
    }

    /** Function to convert HSB (each 0..1) to a packed 0xRRGGBB value. */
    private fun hsbToRgb(hue: Double, saturation: Double, brightness: Double): Int {
        val h = (hue - floor(hue)) * 6.0
        val f = h - floor(h)
        val p = brightness * (1 - saturation)
        val q = brightness * (1 - saturation * f)
        val t = brightness * (1 - saturation * (1 - f))
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(brightness, t, p)
            1 -> Triple(q, brightness, p)
            2 -> Triple(p, brightness, t)
            3 -> Triple(p, q, brightness)
            4 -> Triple(t, p, brightness)
            else -> Triple(brightness, p, q)
        }
        fun channel(v: Double) = (abs(v) * 255 + 0.5).toInt().coerceIn(0, 255)
        return (channel(r) shl 16) or (channel(g) shl 8) or channel(b)
    }
}
