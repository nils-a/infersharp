package com.github.nilsa.infersharp.wsl.actions

import com.github.nilsa.infersharp.wsl.commands.Constants
import com.github.nilsa.infersharp.wsl.commands.WslCommands
import com.intellij.execution.wsl.WSLUtil
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile


abstract class WslAction : AnAction() {
    abstract val title: String

    abstract fun doWslAction(cmd: WslCommands, indicator: ProgressIndicator)
    protected open fun doBeforeWslAction(project: Project) { }
    protected open fun doAfterWslAction(project: Project) { }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        doBeforeWslAction(project)

        val task = object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                val cmd = WslCommands(project)
                cmd.clear()
                doWslAction(cmd, indicator)

                doAfterWslAction(project)
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
            val fileChooserDescriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            fileChooserDescriptor.title = "Select Binary Folder"
            fileChooserDescriptor.description = "Select only the output folder for the binaries."
            entryPoint = FileChooser.chooseFile(
                fileChooserDescriptor,
                project,
                project.guessProjectDir())
        }

        override fun doWslAction(cmd: WslCommands, indicator: ProgressIndicator) {
            val dir = entryPoint ?: return

            indicator.isIndeterminate = true
            cmd.analyze(dir.path, indicator)
        }

        override fun doAfterWslAction(project: Project) {
            val dir = entryPoint ?: return
            val report = VfsUtilCore.findRelativeFile("infer-out/report.txt", dir)

            if (report == null) {
                Notifications.Bus.notify(
                    Notification(
                        Constants.notificationGroup,
                        "Could not find report.txt under $dir.",
                        NotificationType.ERROR
                    ), project
                )
                return
            }

            // open report in editor
            ApplicationManager.getApplication().invokeLater {
                FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptor(project, report),
                    true
                )
            }

            // Visualize using ExternalAnnotator, see
            // https://github.com/SonarSource/sonarlint-intellij/blob/dd908d205876c1a79981399bcf0c03b8d146544b/src/main/java/org/sonarlint/intellij/editor/SonarExternalAnnotator.java
        }

    }
}


