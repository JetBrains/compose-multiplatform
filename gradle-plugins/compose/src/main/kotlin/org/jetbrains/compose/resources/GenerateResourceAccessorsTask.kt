package org.jetbrains.compose.resources

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Path
import kotlin.io.path.relativeTo

internal abstract class GenerateResourceAccessorsTask : CodeGenerationTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val sourceSetName: Property<String>

    @get:Input
    @get:Optional
    abstract val packagingDir: Property<File>

    @get:Input
    abstract val shouldGenerateCode: Property<Boolean>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: Property<File>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    @TaskAction
    fun generate() {
        try {
            val kotlinDir = codeDir.get().asFile
            val rootResDir = resDir.get()
            val sourceSet = sourceSetName.get()

            logger.info("Clean directory $kotlinDir")
            kotlinDir.deleteRecursively()
            kotlinDir.mkdirs()

            if (shouldGenerateCode.get()) {
                logger.info("Generate accessors for $rootResDir")

                //get first level dirs
                val dirs = rootResDir.listNotHiddenFiles()

                dirs.forEach { f ->
                    if (!f.isDirectory) {
                        error("${f.name} is not directory! Raw files should be placed in '${rootResDir.name}/files' directory.")
                    }
                }

                //type -> id -> resource item
                val resources: Map<ResourceType, Map<String, List<ResourceItem>>> = dirs
                    .flatMap { dir ->
                        dir.listNotHiddenFiles()
                            .mapNotNull { it.fileToResourceItems(rootResDir.toPath()) }
                            .flatten()
                    }
                    .groupBy { it.type }
                    .mapValues { (_, items) -> items.groupBy { it.name } }

                val pkgName = packageName.get()
                val moduleDirectory = packagingDir.getOrNull()?.let { it.invariantSeparatorsPath + "/" } ?: ""
                val isPublic = makeAccessorsPublic.get()
                getAccessorsSpecs(
                    resources, pkgName, sourceSet, moduleDirectory, isPublic
                ).forEach { it.writeTo(kotlinDir) }
            } else {
                logger.info("Generation accessors for $rootResDir is disabled")
            }
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: $name task was failed:", e)
        }
    }

    private fun File.fileToResourceItems(
        relativeTo: Path
    ): List<ResourceItem>? {
        val file = this
        val dirName = file.parentFile.name ?: return null
        val typeAndQualifiers = dirName.split("-")
        if (typeAndQualifiers.isEmpty()) return null

        val typeString = typeAndQualifiers.first().lowercase()
        val qualifiers = typeAndQualifiers.takeLast(typeAndQualifiers.size - 1)
        val path = file.toPath().relativeTo(relativeTo)


        if (typeString == "string") {
            error("Forbidden directory name '$dirName'! String resources should be declared in 'values/strings.xml'.")
        }

        if (typeString == "files") {
            if (qualifiers.isNotEmpty()) error("The 'files' directory doesn't support qualifiers: '$dirName'.")
            return null
        }

        if (typeString == "values" && file.extension.equals(XmlValuesConverterTask.CONVERTED_RESOURCE_EXT, true)) {
            return getValueResourceItems(file, qualifiers, path)
        }

        val type = ResourceType.fromString(typeString) ?: error("Unknown resource type: '$typeString'.")
        return listOf(ResourceItem(type, qualifiers, file.nameWithoutExtension.asUnderscoredIdentifier(), path))
    }

    private fun getValueResourceItems(dataFile: File, qualifiers: List<String>, path: Path): List<ResourceItem> {
        val result = mutableListOf<ResourceItem>()
        dataFile.bufferedReader().use { f ->
            var offset = 0L
            var line: String? = f.readLine()
            while (line != null) {
                val size = line.encodeToByteArray().size

                //first line is meta info
                if (offset > 0) {
                    result.add(getValueResourceItem(line, offset, size.toLong(), qualifiers, path))
                }

                offset += size + 1 // "+1" for newline character
                line = f.readLine()
            }
        }
        return result
    }

    private fun getValueResourceItem(
        recordString: String,
        offset: Long,
        size: Long,
        qualifiers: List<String>,
        path: Path
    ): ResourceItem {
        val record = ValueResourceRecord.createFromString(recordString)
        return ResourceItem(record.type, qualifiers, record.key.asUnderscoredIdentifier(), path, offset, size)
    }
}

internal fun File.listNotHiddenFiles(): List<File> =
    listFiles()?.filter { !it.isHidden }.orEmpty()

internal fun String.asUnderscoredIdentifier(): String =
    replace('-', '_')
        .let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }
