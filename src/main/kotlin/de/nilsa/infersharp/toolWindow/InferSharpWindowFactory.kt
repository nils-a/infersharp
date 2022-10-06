package de.nilsa.infersharp.toolWindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class InferSharpWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val view = InferSharpWindow()
        val content = contentFactory.createContent(view, "", false)
        toolWindow.contentManager.addContent(content)
    }

    override fun init(toolWindow: ToolWindow) {
        super.init(toolWindow)
        toolWindow.stripeTitle = "Infer#"
    }
}