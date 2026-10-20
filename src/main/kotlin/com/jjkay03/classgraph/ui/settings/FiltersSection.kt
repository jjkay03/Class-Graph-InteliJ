package com.jjkay03.classgraph.ui.settings

import com.intellij.ui.DocumentAdapter
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import com.jjkay03.classgraph.model.EdgeKind
import com.jjkay03.classgraph.settings.GraphSettings
import javax.swing.event.DocumentEvent

private val linkTips = mapOf(
    EdgeKind.INHERITANCE to "A class extends or implements another class or interface.",
    EdgeKind.FIELD to "A class holds another class as a field or property type, including generic arguments.",
    EdgeKind.SIGNATURE to "A class appears in another class's method parameters or return types.",
    EdgeKind.INSTANTIATION to "A class creates an instance of another with a constructor call.",
    EdgeKind.CALL to "A class calls methods of another class.",
)

/** Function to add the Filters section: search, orphans, link types and local graph. */
internal fun Panel.filtersSection(settings: GraphSettings) {
    val s = settings.values
    collapsibleGroup("Filters") {
        row {
            textField().align(AlignX.FILL).applyToComponent {
                emptyText.text = "Search classes…"
                toolTipText = "Only show classes whose qualified name contains this text. " +
                    "Prefixes narrow it: module:, package:, name:, kind: (e.g. kind:interface)."
                text = s.search
                document.addDocumentListener(object : DocumentAdapter() {
                    /** Function to apply the search text as you type. */
                    override fun textChanged(e: DocumentEvent) {
                        s.search = text
                        settings.changed()
                    }
                })
            }
        }
        toggleRow("Orphans", "Show classes that have no links to other classes under the link types selected below.", s.showOrphans) {
            s.showOrphans = it
            settings.changed()
        }
        EdgeKind.entries.forEach { kind ->
            toggleRow(kind.label, linkTips.getValue(kind), kind in s.enabledKinds(), kind.color) {
                s.setKind(kind, it)
                settings.changed()
            }
        }
        separator()
        toggleRow(
            "Local graph (follow open file)",
            "Show only the class of the file open in the editor and the classes linked to it, up to the chosen depth.",
            s.localGraph,
        ) { s.localGraph = it; settings.changed() }
        sliderRow("Depth", "How many links away from the open class to include in the local graph.", 1.0, 5.0, 1.0, s.localDepth.toDouble()) {
            s.localDepth = it.toInt()
            settings.changed()
        }
    }.expanded = true
}
