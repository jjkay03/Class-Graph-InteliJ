package com.jjkay03.classgraph.ui.settings

import com.intellij.icons.AllIcons
import com.intellij.ui.ColorUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBTextField
import com.jjkay03.classgraph.settings.ColorGroup
import com.jjkay03.classgraph.settings.GraphSettings
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JColorChooser
import javax.swing.JPanel
import javax.swing.event.DocumentEvent

/** Editable list of query → color rows, like Obsidian's Groups. */
class ColorGroupsEditor(private val settings: GraphSettings) : JPanel() {
    private val groups get() = settings.values.groups

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        rebuild()
    }

    /** Function to redraw every group row plus the "New group" button after the list changed. */
    private fun rebuild() {
        removeAll()
        groups.forEach { add(groupRow(it)) }
        add(JPanel(FlowLayout(FlowLayout.LEFT, 0, 4)).apply {
            add(JButton("New group").apply {
                addActionListener {
                    groups.add(ColorGroup())
                    rebuild()
                }
            })
        })
        revalidateAncestors()
    }

    /** Function to build one editable row: color swatch, query field and a remove button. */
    private fun groupRow(group: ColorGroup): JPanel = JPanel(BorderLayout(4, 0)).apply {
        border = BorderFactory.createEmptyBorder(2, 0, 2, 0)
        val query = JBTextField(group.query).apply {
            emptyText.text = "Query, e.g. package:model"
            toolTipText = "Classes matching this query get this color; the first matching group wins. " +
                "Same syntax as search: plain text, or module:, package:, name:, kind:."
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    group.query = text
                    settings.changed()
                }
            })
        }
        val swatch = JButton().apply {
            preferredSize = Dimension(28, 24)
            background = JBColor(group.rgb, group.rgb)
            toolTipText = "Pick color #${ColorUtil.toHex(background)}"
            addActionListener {
                JColorChooser.showDialog(this, "Group color", JBColor(group.rgb, group.rgb))?.let { picked ->
                    group.rgb = picked.rgb and 0xFFFFFF
                    background = JBColor(group.rgb, group.rgb)
                    settings.changed()
                }
            }
        }
        val remove = JButton(AllIcons.Actions.Close).apply {
            preferredSize = Dimension(28, 24)
            addActionListener {
                groups.remove(group)
                settings.changed()
                rebuild()
            }
        }
        add(swatch, BorderLayout.WEST)
        add(query, BorderLayout.CENTER)
        add(remove, BorderLayout.EAST)
    }
}
