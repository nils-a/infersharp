package de.nilsa.infersharp.toolWindow

import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBPanelWithEmptyText

class InferSharpWindow : SimpleToolWindowPanel(true, true) {

    companion object {
        const val ToolWindowId = "INFER_SHARP_TOOLWIN"
    }

    init {
        val panel = JBPanelWithEmptyText()
        setContent(panel)
    }

}


