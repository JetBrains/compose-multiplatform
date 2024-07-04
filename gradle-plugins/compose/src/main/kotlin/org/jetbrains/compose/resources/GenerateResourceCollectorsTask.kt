package org.jetbrains.compose.resources

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.jetbrains.compose.internal.IdeaImportTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar

internal abstract class GenerateResourceCollectorsTask : IdeaImportTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val shouldGenerateCode: Property<Boolean>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceAccessorDirs: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    override fun safeAction() {
        val kotlinDir = codeDir.get().asFile
        val inputDirs = resourceAccessorDirs.files

        logger.info("Clean directory $kotlinDir")
        kotlinDir.deleteRecursively()
        kotlinDir.mkdirs()

        val inputFiles = inputDirs.flatMap { dir ->
            dir.walkTopDown().filter { !it.isHidden && it.isFile && it.extension == "kt" }.toList()
        }

        if (shouldGenerateCode.get()) {
            logger.info("Generate ResourceCollectors for $kotlinDir")
            val funNames = inputFiles.mapNotNull { inputFile ->
                if (inputFile.nameWithoutExtension.contains('.')) {
                    val (fileName, suffix) = inputFile.nameWithoutExtension.split('.')
                    val type = ResourceType.values().firstOrNull { fileName.startsWith(it.accessorName, true) }
                    val name = "_collect${suffix.uppercaseFirstChar()}${fileName}Resources"

                    if (type == null) {
                        logger.warn("Unknown resources type: `$inputFile`")
                        null
                    } else if (!inputFile.readText().contains(name)) {
                        logger.warn("A function '$name' is not found in the `$inputFile` file!")
                        null
                    } else {
                        logger.info("Found collector function: `$name`")
                        type to name
                    }
                } else {
                    logger.warn("Unknown file name: `$inputFile`")
                    null
                }
            }.groupBy({ it.first }, { it.second })

            val pkgName = packageName.get()
            val isPublic = makeAccessorsPublic.get()
            val spec = getResourceCollectorsFileSpec("ResourceCollectors", pkgName, isPublic, funNames)
            spec.writeTo(kotlinDir)
        } else {
            logger.info("Generation ResourceCollectors for $kotlinDir is disabled")
        }
    }
}
