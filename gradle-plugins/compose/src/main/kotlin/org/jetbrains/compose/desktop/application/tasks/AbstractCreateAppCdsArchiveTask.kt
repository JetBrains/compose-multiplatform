package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.jetbrains.compose.desktop.application.dsl.AppCdsMode
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject

abstract class AbstractCreateAppCdsArchiveTask @Inject constructor(
    createDistributable: TaskProvider<AbstractJPackageTask>
) : AbstractComposeDesktopTask() {
    @get:Internal
    internal val appImageRootDir: Provider<Directory> = createDistributable.flatMap { it.destinationDir }

    @get:Input
    internal val packageName: Provider<String> = createDistributable.flatMap { it.packageName }

    @get:Input
    internal val appCdsMode: Property<AppCdsMode> = objects.notNullProperty()

    @Suppress("unused")
    @OutputFile
    val appCdsArchiveFile: Provider<File> = provider {
        val packagedAppRootDir = packagedAppRootDir(appImageRootDir.get())
        appCdsMode.get().appClassesArchiveFile(packagedAppRootDir)
    }

    // This is needed to correctly describe the dependencies to Gradle.
    // Can't just use appImageRootDir because the AppCDS archive needs to be excluded
    @Suppress("unused")
    @get:InputFiles
    internal val dependencyFiles: Provider<FileTree> = provider {
        // If the app image root directory doesn't exist, return an empty file tree
        appImageRootDir.get().let {
            if (!it.asFile.isDirectory) {
                return@provider it.asFileTree
            }
        }

        val appCdsArchiveFile = appCdsArchiveFile.get().relativeTo(appImageRootDir.get().asFile).path
        appImageRootDir.get().asFileTree.matching { it.exclude(appCdsArchiveFile) }
    }

    @TaskAction
    fun run() {
        // Before running the app, replace the 'java-options' corresponding to
        // AppCdsMode.runtimeJvmArgs with AppCdsMode.appClassesArchiveCreationJvmArgs
        // This must be done because, for example, -XX:SharedArchiveFile and
        // -XX:ArchiveClassesAtExit can't be used at the same time
        val packagedRootDir = packagedAppRootDir(appImageRootDir.get())
        val appDir = packagedAppJarFilesDir(packagedRootDir)
        val cfgFile = appDir.resolve("${packageName.get()}.cfg")
        val cfgFileTempCopy = File(cfgFile.parentFile, "${cfgFile.name}.tmp")

        // Save the original cfg file
        Files.copy(cfgFile.toPath(), cfgFileTempCopy.toPath(), StandardCopyOption.REPLACE_EXISTING)
        try {
            // Edit the cfg file
            cfgFile.outputStream().bufferedWriter().use { output ->
                // Copy lines, filtering the AppCdsMode's runtime options
                val runtimeOptionCfgLines = appCdsMode.get().runtimeJvmArgs()
                    .mapTo(mutableSetOf()) { "java-options=$it" }
                cfgFileTempCopy.useLines { lines ->
                    lines.forEach { line ->
                        if (line !in runtimeOptionCfgLines) {
                            output.appendLine(line)
                        }
                    }
                }

                // Add the AppCdsMode's archive creation options
                val archiveCreationOptions = appCdsMode.get().appClassesArchiveCreationJvmArgs().toSet()
                for (arg in archiveCreationOptions) {
                    output.appendLine("java-options=$arg")
                }
            }

            // Run the app to create the AppCDS archive
            execOperations.executePackagedApp(
                appImageRootDir = appImageRootDir.get(),
                packageName = packageName.get()
            )
        } finally {
            // Restore the original cfg file
            Files.move(cfgFileTempCopy.toPath(), cfgFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }
    }
}