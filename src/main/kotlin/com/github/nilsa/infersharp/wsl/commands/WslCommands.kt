package com.github.nilsa.infersharp.wsl.commands

import com.github.nilsa.infersharp.toolWindow.InferSharpWindow
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessRunner
import com.intellij.execution.process.ProcessHandlerFactory
import com.intellij.execution.process.ProcessOutput
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.wsl.WSLCommandLineOptions
import com.intellij.execution.wsl.WSLDistribution
import com.intellij.execution.wsl.WSLUtil
import com.intellij.execution.wsl.WslDistributionManager
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory


class WslCommands(private val project: Project) {
    private val log = Logger.getInstance(WslCommands::class.java)
    private val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(InferSharpWindow.ToolWindowId)!!
    private val distributionManager = NotNullLazyValue.createValue {
        val instance = WslDistributionManager.getInstance()
        instance
    }
    private val wsl = NotNullLazyValue.createValue {
        if (!WSLUtil.isSystemCompatible()) {
            throw IllegalArgumentException("System has no support for WSL.")
        }
        val distribution = distributionManager.value.getOrCreateDistributionByMsId(Constants.wslDistribution)
        val existingDistributions = distributionManager.value.installedDistributions
        if (!existingDistributions.contains(distribution)) {
            throw IllegalArgumentException("Distribution '${Constants.wslDistribution}' is not available.")
        }
        distribution
    }

    fun clear() {
        ApplicationManager.getApplication().invokeAndWait {
            toolWindow.contentManager.removeAllContents(false)
        }
    }

    fun checkInferSharp(show: Boolean = false, indicator: ProgressIndicator): Boolean {
        val displayName = if (show) "Checking" else ""
        indicator.text = "Checking Infer# installation"
        indicator.text2 = "System compatability"
        if (!WSLUtil.isSystemCompatible()) {
            Notifications.Bus.notify(
                Notification(
                    Constants.notificationGroup,
                    "Infer#: System has no support for WSL.",
                    NotificationType.WARNING
                ), project
            )

            return false
        }
        indicator.text2 = "WSL distribution: ${Constants.wslDistribution}"
        val hasWSLDistribution = distributionManager.value.installedDistributions.any {
            it.msId.equals(Constants.wslDistribution, true)
        }
        if (!hasWSLDistribution) {
            Notifications.Bus.notify(
                Notification(
                    Constants.notificationGroup,
                    "Infer#: WSL distribution '${Constants.wslDistribution}' is not available.",
                    NotificationType.WARNING
                ), project
            )
            return false
        }

        val output = run(displayName, sequence {
            yield(DoInTerminal(commandLine("ls", Constants.folderName).inWsl()))
        }, indicator)

        return output?.checkSuccess(log) ?: false
    }


    fun installInferSharp(indicator: ProgressIndicator) {
        if (!WSLUtil.isSystemCompatible()) {
            throw IllegalArgumentException("System has no support for WSL.")
        }
        indicator.text = "Installing Infer# into WSL"
        val wslExe = WSLDistribution.findWslExe()?.toString()
            ?: throw IllegalArgumentException("Unable to find wsl.exe!")
        val hasWSLDistribution = distributionManager.value.installedDistributions.any {
            it.msId.equals(Constants.wslDistribution, true)
        }

        run(
            "Installing Infer# in WSL", sequence
            {
                if (!hasWSLDistribution) {
                    // install ubuntu
                    yield(
                        DoInTerminal(
                            "Installing ${Constants.wslDistribution} now. This might result in a new window opening...",
                            GeneralCommandLine()
                                .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
                                .withExePath(wslExe)
                                .withParameters("--install", "--distribution", Constants.wslDistribution),
                        )
                    )
                    yield(
                        DoInTerminal("Installation of ${Constants.wslDistribution} complete.")
                    )
                } else {
                    yield(
                        DoInTerminal("WSL distribution ${Constants.wslDistribution} is available.")
                    )
                }

                // since WSL is ready for use now, we can use the wsl field now.
                val inferSharpTempFile = "infersharp.tar.gz"
                val inRootFolder: (WSLCommandLineOptions) -> Unit = { it.remoteWorkingDirectory = "/root" }

                yield(
                    DoInTerminal(
                        commandLine(
                            "rm",
                            "-rf",
                            inferSharpTempFile,
                            "infersharp",
                            Constants.folderName
                        ).inWsl(wslCommandOptionModification = inRootFolder)
                    )
                )
                // download binary release
                yield(
                    DoInTerminal(
                        commandLine(
                            "wget",
                            "--retry-connrefused",
                            "--waitretry=5",
                            "--read-timeout=60",
                            "--timeout=90",
                            "--tries=10",
                            "--show-progress",
                            "--progress=dot",
                            "--output-document=$inferSharpTempFile",
                            Constants.binaryDownloadUrl,
                        ).inWsl(wslCommandOptionModification = inRootFolder)
                    )
                )

                yield(
                    DoInTerminal(
                        commandLine(
                            "tar",
                            "-xvzf",
                            inferSharpTempFile
                        ).inWsl(wslCommandOptionModification = inRootFolder)
                    )
                )
                yield(
                    DoInTerminal(
                        commandLine(
                            "mv",
                            "infersharp",
                            Constants.folderName
                        ).inWsl(wslCommandOptionModification = inRootFolder)
                    )
                )
                yield(
                    DoInTerminal(
                        commandLine(
                            "rm",
                            "-r",
                            inferSharpTempFile
                        ).inWsl(wslCommandOptionModification = inRootFolder)
                    )
                )
                yield(DoInTerminal("Setup complete."))
            }, indicator
        )
    }


    fun analyze(entryPoint: String, indicator: ProgressIndicator) {
        if (!checkInferSharp(false, indicator)) {
            return
        }
        indicator.text = "Analyzing $entryPoint using Infer#"
        val wslDir = wsl.value.getWslPath(entryPoint)
        if (wslDir == null) {
            Notifications.Bus.notify(
                Notification(
                    Constants.notificationGroup,
                    "Could not find WSL directory for: ${entryPoint}.",
                    NotificationType.WARNING
                ), project
            )
            return
        }
        val inWslDir: (WSLCommandLineOptions) -> Unit = { it.remoteWorkingDirectory = wslDir }
        run("Analyzing $entryPoint", sequence {
            yield(
                DoInTerminal(
                    "Translating...", commandLine(
                        "${Constants.folderName}/Cilsil/Cilsil",
                        "translate",
                        wslDir,
                        "--outcfg", "$wslDir/cfg.json",
                        "--outtenv", "$wslDir/tenv.json",
                        "--extprogress"
                    ).inWsl(wslCommandOptionModification = inWslDir)
                )
            )
            yield(
                DoInTerminal(
                    "Capturing...", commandLine(
                        "${Constants.folderName}/infer/lib/infer/infer/bin/infer",
                        "capture"
                    ).inWsl(wslCommandOptionModification = inWslDir)
                )
            )
            yield(
                DoInTerminal(
                    "Analyzing...", commandLine(
                        "${Constants.folderName}/infer/lib/infer/infer/bin/infer",
                        "analyzejson",
                        "--debug-level", "1",
                        "--pulse",
                        "--sarif",
                        "--disable-issue-type", "PULSE_UNINITIALIZED_VALUE",
                        "--disable-issue-type", "MEMORY_LEAK",
                        "--disable-issue-type", "UNINITIALIZED_VALUE",
                        "--cfg-json", "$wslDir/cfg.json",
                        "--tenv-json", "$wslDir/tenv.json"
                    ).inWsl(wslCommandOptionModification = inWslDir)
                )
            )
            yield(DoInTerminal("Done."))
        }, indicator)

        // VIsualize using ExternalAnnotator, see
    // https://github.com/SonarSource/sonarlint-intellij/blob/dd908d205876c1a79981399bcf0c03b8d146544b/src/main/java/org/sonarlint/intellij/editor/SonarExternalAnnotator.java

    }

    private fun commandLine(
        program: String,
        vararg parameters: String,
        commandLineModification: ((GeneralCommandLine) -> Unit)? = null
    ): GeneralCommandLine {
        val command = GeneralCommandLine()
            .withParentEnvironmentType(GeneralCommandLine.ParentEnvironmentType.CONSOLE)
            .withExePath(program)
            .withParameters(parameters.toMutableList())
        commandLineModification?.invoke(command)
        return command
    }

    private fun GeneralCommandLine.inWsl(
        wslDistribution: WSLDistribution = wsl.value,
        wslCommandOptionModification: ((WSLCommandLineOptions) -> Unit)? = null
    ): GeneralCommandLine {
        val wslCommandOptions = WSLCommandLineOptions()
        wslCommandOptions.isSudo = true
        wslCommandOptions.isExecuteCommandInShell = true
        wslCommandOptions.isLaunchWithWslExe = true
        wslCommandOptions.remoteWorkingDirectory = Constants.workDirectory

        wslCommandOptionModification?.invoke(wslCommandOptions)
        return wslDistribution.patchCommandLine(this, project, wslCommandOptions)
    }

    private fun run(
        displayName: String,
        actionsInTerminal: Sequence<DoInTerminal>,
        indicator: ProgressIndicator
    ): ProcessOutput? {
        val procHandlerFactory = ProcessHandlerFactory.getInstance()
        val term = TerminalExecutionConsole(project, null)
        term.withConvertLfToCrlfForNonPtyProcess(true)
        Disposer.register(toolWindow.contentManager, term)

        if (displayName.isNotEmpty()) {
            ApplicationManager.getApplication().invokeAndWait {
                val content = createContent(term.terminalWidget, displayName)
                toolWindow.contentManager.addContent(content)
                content.setDisposer(toolWindow.contentManager)
                toolWindow.show()
            }
        }

        var out: ProcessOutput? = null
        run breaking@{
            actionsInTerminal.forEach { action ->
                if (indicator.isCanceled) {
                    return@breaking
                }
                if (action.output != null) {
                    val outputLine = "${action.output}${System.lineSeparator()}"
                    term.print(outputLine, ConsoleViewContentType.NORMAL_OUTPUT)
                }

                val commandLine = action.commandLine ?: return@forEach

                log.trace("infer# command: ${commandLine.commandLineString}")
                indicator.text2 = commandLine.commandLineString

                val procHandler = procHandlerFactory.createProcessHandler(commandLine)
                term.attachToProcess(procHandler)

                val runner = CapturingProcessRunner(procHandler)
                out = runner.runProcess()
                if (!out!!.checkSuccess(log)) {
                    return@breaking
                }
            }
        }

        return out
    }

    private fun createContent(
        terminalWidget: JBTerminalWidget,
        displayName: String
    ): Content {
        return ContentFactory.SERVICE.getInstance().createContent(
            terminalWidget.component, displayName, false
        )
    }

    private class DoInTerminal(val output: String?, val commandLine: GeneralCommandLine?) {
        constructor(commandLine: GeneralCommandLine) : this(null, commandLine)
        constructor(output: String) : this(output, null)
    }
}

