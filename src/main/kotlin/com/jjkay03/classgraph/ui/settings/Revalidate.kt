package com.jjkay03.classgraph.ui.settings

import java.awt.Container
import javax.swing.JComponent

/** Dynamic rows change a component's size; the DSL-built parents must re-layout too. */
internal fun JComponent.revalidateAncestors() {
    var c: Container? = this
    while (c != null) {
        c.revalidate()
        c = c.parent
    }
    repaint()
}
