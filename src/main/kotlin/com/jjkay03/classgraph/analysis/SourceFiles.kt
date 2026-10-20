package com.jjkay03.classgraph.analysis

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileFilter

internal object SourceFiles {
    private val extensions = setOf("java", "kt")

    /** Folders that never contain the project's own sources but can hold hundreds of thousands of files. */
    private val skippedDirectories = setOf(".intellijPlatform", ".gradle", ".git", ".idea", "build", "out", "node_modules")

    /** Java/Kotlin files that are part of the project's source roots. */
    fun find(project: Project): List<VirtualFile> {
        val index = ProjectFileIndex.getInstance(project)
        val files = mutableListOf<VirtualFile>()
        index.iterateContent(
            { vf ->
                if (!vf.isDirectory && vf.extension in extensions && index.isInSourceContent(vf)) files.add(vf)
                true
            },
            VirtualFileFilter { vf -> !vf.isDirectory || vf.name !in skippedDirectories },
        )
        return files
    }
}
