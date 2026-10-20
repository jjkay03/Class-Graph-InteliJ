package com.jjkay03.classgraph.ui

import com.intellij.openapi.util.text.HtmlChunk

/** Class names can contain `<`/`>` (generics, Kotlin backticks); escape them so tooltips never render as HTML. */
internal fun safeTooltip(text: String): String = HtmlChunk.text(text).wrapWith(HtmlChunk.html()).toString()
