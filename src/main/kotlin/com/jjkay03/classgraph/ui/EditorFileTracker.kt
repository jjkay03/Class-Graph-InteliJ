package com.jjkay03.classgraph.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project

/** Reports the path of the file open in the editor, now and whenever the selected editor tab changes. */
class EditorFileTracker(project: Project, parent: Disposable, private val onFileChanged: (String?) -> Unit) {

    init {
        project.messageBus.connect(parent).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            object : FileEditorManagerListener {
                /** Function to report the newly selected editor file. */
                override fun selectionChanged(event: FileEditorManagerEvent) = onFileChanged(event.newFile?.path)
            },
        )
        onFileChanged(FileEditorManager.getInstance(project).selectedFiles.firstOrNull()?.path)
    }
}
