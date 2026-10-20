package com.jjkay03.classgraph.model

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.JBColor
import kotlin.math.sqrt

class ClassNode(
    val id: String,
    val name: String,
    val pkg: String,
    val module: String,
    val kind: NodeKind,
    val filePath: String,
    val pointer: SmartPsiElementPointer<PsiElement>?,
) {
    // Layout state
    var x = 0.0
    var y = 0.0
    var vx = 0.0
    var vy = 0.0
    var pinned = false

    // View state
    var degree = 0

    /** Group/auto color; null means the default node color. */
    var color: JBColor? = null

    /** Function to compute the radius in world units; hubs are bigger. [sizeFactor] is the user's node-size slider. */
    fun radius(sizeFactor: Double = 1.0): Double = (4.0 + sqrt(degree.toDouble()) * 2.2) * sizeFactor
}
