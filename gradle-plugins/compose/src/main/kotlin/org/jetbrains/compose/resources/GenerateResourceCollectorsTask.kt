package org.jetbrains.compose.resources

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.jetbrains.compose.internal.IdeaImportTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar

internal abstract class GenerateExpectResourceCollectorsTask : IdeaImportTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resClassName: Property<String>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    override fun safeAction() {
        val kotlinDir = codeDir.get().asFile

        logger.info("Clean directory $kotlinDir")
        kotlinDir.deleteRecursively()
        kotlinDir.mkdirs()

        logger.info("Generate expect ResourceCollectors for $kotlinDir")

        val pkgName = packageName.get()
        val resClassName = resClassName.get()
        val isPublic = makeAccessorsPublic.get()
        val spec = getExpectResourceCollectorsFileSpec(
            packageName = pkgName,
            fileName = "ExpectResourceCollectors",
            resClassName = resClassName,
            isPublic = isPublic
        )
        spec.writeTo(kotlinDir)
    }
}

internal abstract class GenerateActualResourceCollectorsTask : IdeaImportTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resClassName: Property<String>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:Input
    abstract val useActualModifier: Property<Boolean>

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
        logger.info("Generate actual ResourceCollectors for $kotlinDir")
        val funNames = inputFiles.mapNotNull { inputFile ->
            if (inputFile.nameWithoutExtension.contains('.')) {
                val (fileName, suffix) = inputFile.nameWithoutExtension.split('.')
                val type = ResourceType.entries.firstOrNull { fileName.startsWith(it.accessorName, true) }
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
        }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sorted() }

        val pkgName = packageName.get()
        val resClassName = resClassName.get()
        val isPublic = makeAccessorsPublic.get()
        val useActual = useActualModifier.get()
        val spec = getActualResourceCollectorsFileSpec(
            packageName = pkgName,
            fileName = "ActualResourceCollectors",
            resClassName = resClassName,
            isPublic = isPublic,
            useActualModifier = useActual,
            typeToCollectorFunctions = funNames
        )
        spec.writeTo(kotlinDir)
    }
}
