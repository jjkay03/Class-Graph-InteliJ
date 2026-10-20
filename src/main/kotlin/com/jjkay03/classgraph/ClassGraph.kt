package com.jjkay03.classgraph

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jjkay03.classgraph.ui.ClassGraphView

class ClassGraph : ToolWindowFactory, DumbAware {
    /** Function to build the tool window: create the graph view, register it for disposal and start the first scan. */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val view = ClassGraphView(project)
        val content = ContentFactory.getInstance().createContent(view, "", false)
        Disposer.register(toolWindow.disposable, view)
        toolWindow.contentManager.addContent(content)
        view.reload()
    }
}
