package com.github.nilsa.infersharp.wsl.commands

import com.github.nilsa.infersharp.toolWindow.InferSharpWindow
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessRunner
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessOutput
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory

class WslCommands(private val project: Project) {
    private val log = Logger.getInstance(WslCommands::class.java)
    private val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(InferSharpWindow.ToolWindowId)!!

    fun clear() {
        ApplicationManager.getApplication().invokeAndWait {
            toolWindow.contentManager.removeAllContents(true)
        }
    }

    fun check(show: Boolean = false): Boolean {
        val displayName = if (show) "Checking" else ""
        return runWsl(displayName, "ls", Constants.folderName).checkSuccess(log)
    }

    private fun runWsl(displayName: String, vararg parameters: String): ProcessOutput {
        val commandLine = GeneralCommandLine()
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withExePath("wsl.exe")
            .withParameters(listOf("--cd", "~", "-u", "root", "--").union(parameters.toList()).toMutableList())

        val procHandler = ProcessHandlerFactory.getInstance().createProcessHandler(commandLine)
        val term = TerminalExecutionConsole(project, procHandler)
        term.attachToProcess(procHandler)
        commandLine.withRedirectErrorStream(false)

        if (displayName.isNotEmpty()) {
            ApplicationManager.getApplication().invokeAndWait {
                toolWindow.contentManager.addContent(createContent(term.terminalWidget, displayName))
                toolWindow.show()
            }
        }

        val runner = CapturingProcessRunner(procHandler)
        return runner.runProcess()
    }

    private fun createContent(
        terminalWidget: JBTerminalWidget,
        displayName: String
    ): Content {
        return ContentFactory.SERVICE.getInstance().createContent(
            terminalWidget.component, displayName, false
        )
    }
}

