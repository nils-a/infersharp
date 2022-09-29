package com.github.nilsa.infersharp.wsl.actions

import com.github.nilsa.infersharp.wsl.commands.WslCommands
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task

class CheckAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val task = object : Task.Backgroundable(project, "Checking InferSharp (WSL)", false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val cmd = WslCommands(project)
                cmd.clear()
                cmd.check(true)
            }
        }

        ProgressManager.getInstance().run(task);
    }
}