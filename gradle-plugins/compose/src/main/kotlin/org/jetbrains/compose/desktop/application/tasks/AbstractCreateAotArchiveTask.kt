package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.desktop.application.dsl.AotConfiguration
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

@DisableCachingByDefault(because = "Uses platform-specific JDK tools whose output depends on local JDK installation")
abstract class AbstractCreateAotArchiveTask @Inject constructor(
    createDistributable: TaskProvider<AbstractJPackageTask>
) : AbstractComposeDesktopTask() {
    @get:Internal
    internal val appImageRootDir: Provider<Directory> = createDistributable.flatMap { it.destinationDir }

    @get:Input
    internal val packageName: Provider<String> = createDistributable.flatMap { it.packageName }

    @get:Input
    internal abstract val aotConfig: Property<AotConfiguration>

    @get:OutputFile
    val aotArchiveFile: Provider<RegularFile> = provider {
        val appImageRootDir = appImageRootDir.get()
        val packageName = packageName.get()
        val packagedAppRootDir = packagedAppRootDir(appImageRootDir, packageName)
        aotConfig.get().mode.trainingRunClassArchive(packagedAppRootDir)
    }

    // This is needed to correctly describe the dependencies to Gradle.
    // Can't just use appImageRootDir because the AOT archive needs to be excluded
    @Suppress("unused")
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    internal val dependencyFiles: FileTree get() {
        // If the app image root directory doesn't exist, return an empty file tree
        appImageRootDir.get().let {
            if (!it.asFile.isDirectory) {
                return it.asFileTree
            }
        }

        val aotArchiveFile = aotArchiveFile.ioFile.relativeTo(appImageRootDir.get().asFile).path
        return appImageRootDir.get().asFileTree.matching { it.exclude(aotArchiveFile) }
    }

    @TaskAction
    fun run() {
        // Before running the app, replace the 'java-options' corresponding to
        // AotMode.runtimeJvmArgs with AotMode.trainingRunJvmArgs
        // This must be done because, for example, -XX:SharedArchiveFile and
        // -XX:ArchiveClassesAtExit can't be used at the same time
        val packagedRootDir = packagedAppRootDir(appImageRootDir.get(), packageName = packageName.get())
        val appDir = packagedAppJarFilesDir(packagedRootDir)
        val cfgFile = appDir.asFile.resolve("${packageName.get()}.cfg")
        val cfgFileTempCopy = File(cfgFile.parentFile, "${cfgFile.name}.tmp")

        // Save the cfg file before making changes for the archive-creating run
        Files.copy(cfgFile.toPath(), cfgFileTempCopy.toPath(), StandardCopyOption.REPLACE_EXISTING)
        try {
            // Edit the cfg file
            cfgFile.outputStream().bufferedWriter().use { output ->
                // Copy lines, filtering the mode's runtime options
                val runtimeOptionCfgLines = aotConfig.get().runtimeJvmArgs
                    .mapTo(mutableSetOf()) { "java-options=$it" }
                cfgFileTempCopy.useLines { lines ->
                    lines.forEach { line ->
                        if (line !in runtimeOptionCfgLines) {
                            output.appendLine(line)
                        }
                    }
                }

                // Append the AOT mode's training-run options
                val trainingRunArgs = aotConfig.get().mode.trainingRunJvmArgs().toSet()
                for (arg in trainingRunArgs) {
                    output.appendLine("java-options=$arg")
                }
            }

            // Run the app to create the archive
            execOperations.executePackagedApp(
                appImageRootDir = appImageRootDir.get(),
                packageName = packageName.get()
            )
        } finally {
            // Restore the cfg file
            Files.move(cfgFileTempCopy.toPath(), cfgFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}