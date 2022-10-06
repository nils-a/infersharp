package de.nilsa.infersharp.wsl.commands

object Constants {
    // TODO: some of this could be settings, right?
    private const val inferSharpVersion = "1.2"
    const val wslDistribution = "ubuntu"
    const val folderName = "/opt/infersharp$inferSharpVersion"
    const val workDirectory = "/root"
    const val binaryDownloadUrl =
        "https://github.com/microsoft/infersharp/releases/download/"+
                "v$inferSharpVersion/infersharp-linux64-v$inferSharpVersion.tar.gz"

    const val notificationGroup = "Infer#"
}