package com.jjkay03.classgraph.filter

import com.jjkay03.classgraph.model.ClassNode

/**
 * Query syntax shared by search and color groups: plain text matches the qualified class name;
 * `module:`, `package:` (or `path:`), `name:` and `kind:` target one attribute.
 */
object NodeQuery {

    /** Function to check whether [node] matches [rawQuery]; a blank query matches everything. */
    fun matches(node: ClassNode, rawQuery: String): Boolean {
        val query = rawQuery.trim().lowercase()
        if (query.isEmpty()) return true
        val colon = query.indexOf(':')
        if (colon > 0) {
            val value = query.substring(colon + 1).trim()
            when (query.substring(0, colon)) {
                "module" -> return node.module.lowercase().contains(value)
                "package", "path" -> return node.pkg.lowercase().contains(value)
                "name" -> return node.name.lowercase().contains(value)
                "kind" -> return node.kind.label.lowercase() == value
            }
        }
        return node.id.lowercase().contains(query)
    }
}
