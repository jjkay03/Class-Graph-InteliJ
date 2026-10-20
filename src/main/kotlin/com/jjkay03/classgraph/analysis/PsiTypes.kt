package com.jjkay03.classgraph.analysis

import com.intellij.psi.*

internal object PsiTypes {
    /** Every class mentioned by [type], including generic arguments, arrays and wildcard bounds. */
    fun classesIn(type: PsiType?, depth: Int = 0, out: MutableSet<PsiClass> = LinkedHashSet()): Set<PsiClass> {
        when (type) {
            is PsiClassType -> {
                type.resolve()?.let { out.add(it) }
                if (depth < 4) type.parameters.forEach { classesIn(it, depth + 1, out) }
            }
            is PsiArrayType -> classesIn(type.componentType, depth, out)
            is PsiWildcardType -> classesIn(type.bound, depth, out)
        }
        return out
    }
}
