package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import javax.inject.Inject

@DisableCachingByDefault(because = "There is no logic, just copy files")
internal abstract class AssembleTargetResourcesTask : DefaultTask() {

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceDirectories: ConfigurableFileCollection

    @get:Input
    abstract val relativeResourcePlacement: Property<File>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun action() {
        val outputDirectoryFile = outputDirectory.get().asFile
        if (outputDirectoryFile.exists()) {
            outputDirectoryFile.deleteRecursively()
        }
        outputDirectoryFile.mkdirs()

        fileSystem.copy { copy ->
            resourceDirectories.files.forEach { dir ->
                copy.from(dir)
            }
            copy.into(outputDirectoryFile.resolve(relativeResourcePlacement.get()))
            copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }

        if (outputDirectoryFile.listFiles()?.isEmpty() != false) {
            // Output an empty directory for the zip task
            outputDirectoryFile.resolve(relativeResourcePlacement.get()).mkdirs()
        }
    }
}