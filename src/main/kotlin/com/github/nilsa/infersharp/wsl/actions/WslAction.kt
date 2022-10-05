package com.github.nilsa.infersharp.wsl.actions

import com.github.nilsa.infersharp.wsl.commands.WslCommands
import com.intellij.execution.wsl.WSLUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import java.nio.file.FileSystems
import java.nio.file.spi.FileSystemProvider

abstract class WslAction : AnAction() {
    abstract val title: String

    abstract fun doWslAction(cmd: WslCommands, indicator: ProgressIndicator)
    protected open fun doBeforeWslAction(project: Project) { }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        doBeforeWslAction(project)

        val task = object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val cmd = WslCommands(project)
                cmd.clear()
                doWslAction(cmd, indicator)
            }
        }

        ProgressManager.getInstance().run(task)
    }

    override fun update(e: AnActionEvent) {
        if(!SystemInfo.isWindows) {
            e.presentation.isEnabledAndVisible = false
        } else if (!WSLUtil.isSystemCompatible()) {
            e.presentation.isEnabled = false
        }
        super.update(e)
    }

    class CheckAction : WslAction() {
        override val title: String
            get() = "Checking InferSharp (WSL)"

        override fun doWslAction(cmd: WslCommands, indicator: ProgressIndicator) {
            indicator.isIndeterminate=true
            cmd.checkInferSharp(true, indicator)
        }
    }


    class InstallAction : WslAction() {
        override val title: String
            get() = "Installing InferSharp (WSL)"

        override fun doWslAction(cmd: WslCommands, indicator: ProgressIndicator) {
            indicator.isIndeterminate = true
            cmd.installInferSharp(indicator)
        }
    }

    class AnalyzeAction : WslAction() {
        override val title: String
            get() = "Analyzing project using InferSharp (WSL)"

        private var entryPoint: VirtualFile? = null

        override fun doBeforeWslAction(project: Project) {
            entryPoint = FileChooser.chooseFile(FileChooserDescriptor(
                false,
                true,
                false,
                false,
                false,
                false
            ), project, project.guessProjectDir())

        }

        override fun doWslAction(cmd: WslCommands, indicator: ProgressIndicator) {
            val dir = entryPoint ?: return

            indicator.isIndeterminate = true
            cmd.analyze(dir.path, indicator)
        }
    }
}


