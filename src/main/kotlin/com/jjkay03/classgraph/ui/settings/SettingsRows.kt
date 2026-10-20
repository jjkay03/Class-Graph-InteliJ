package com.jjkay03.classgraph.ui.settings

import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.util.ui.ColorIcon
import com.intellij.ui.JBColor
import java.awt.Dimension
import javax.swing.JSlider
import kotlin.math.roundToInt

/** Function to add a toggle row with a hover explanation ([tip]) and an optional color swatch. */
internal fun Panel.toggleRow(
    text: String,
    tip: String,
    initial: Boolean,
    swatch: JBColor? = null,
    onToggle: (Boolean) -> Unit,
) {
    row {
        checkBox(text).applyToComponent {
            isSelected = initial
            toolTipText = tip
            addActionListener { onToggle(isSelected) }
        }
        if (swatch != null) icon(ColorIcon(10, swatch)).applyToComponent { toolTipText = tip }
    }
}

/** Function to add a labelled slider mapping [min]..[max] in increments of [step], like Obsidian's. Hovering the label or slider shows [tip]. */
internal fun Panel.sliderRow(
    label: String,
    tip: String,
    min: Double,
    max: Double,
    step: Double,
    initial: Double,
    onChange: (Double) -> Unit,
) {
    row {
        label(label).applyToComponent { toolTipText = tip }.gap(RightGap.SMALL)
        val slider = JSlider(0, ((max - min) / step).roundToInt(), ((initial - min) / step).roundToInt()).apply {
            toolTipText = tip
            preferredSize = Dimension(110, preferredSize.height)
        }
        cell(slider).align(AlignX.FILL).resizableColumn().applyToComponent {
            addChangeListener { onChange(min + value * step) }
        }
    }
}
