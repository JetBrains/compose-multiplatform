package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.io.RandomAccessFile
import java.nio.file.Path
import kotlin.io.path.relativeTo

/**
 * This task should be FAST and SAFE! Because it is being run during IDE import.
 */
internal abstract class GenerateResClassTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    abstract val moduleDir: Property<File>

    @get:Input
    abstract val shouldGenerateResClass: Property<Boolean>

    @get:Input
    abstract val makeResClassPublic: Property<Boolean>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: Property<File>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val convertedXmlValuesDir: Property<File>

    @get:OutputDirectory
    abstract val codeDir: Property<File>

    @TaskAction
    fun generate() {
        try {
            val kotlinDir = codeDir.get()
            logger.info("Clean directory $kotlinDir")
            kotlinDir.deleteRecursively()
            kotlinDir.mkdirs()

            if (shouldGenerateResClass.get()) {
                val rootResDir = resDir.get()
                logger.info("Generate resources for $rootResDir")

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
                getResFileSpecs(
                    resources,
                    packageName.get(),
                    moduleDir.getOrNull()?.let { it.invariantSeparatorsPath + "/" } ?: "",
                    makeResClassPublic.get()
                ).forEach { it.writeTo(kotlinDir) }
            } else {
                logger.info("Generation Res class is disabled")
            }
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: GenerateResClassTask was failed:", e)
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

        if (typeString == "values" && file.extension.equals("xml", true)) {
            val converted = convertedXmlValuesDir.get()
                .resolve(file.parentFile.name)
                .resolve(file.nameWithoutExtension + ".${XmlValuesConverterTask.CONVERTED_RESOURCE_EXT}")
            return getValueResourceItems(converted, qualifiers, path.parent.resolve(converted.name))
        }

        val type = ResourceType.fromString(typeString) ?: error("Unknown resource type: '$typeString'.")
        return listOf(ResourceItem(type, qualifiers, file.nameWithoutExtension.asUnderscoredIdentifier(), path))
    }

    private fun getValueResourceItems(dataFile: File, qualifiers: List<String>, path: Path) : List<ResourceItem> {
        val result = mutableListOf<ResourceItem>()
        RandomAccessFile(dataFile, "r").use { f ->
            var offset: Long = 0
            var line: String? = f.readLine()
            while (line != null) {
                val size = line.encodeToByteArray().size.toLong()
                result.add(getValueResourceItem(line, offset, size, qualifiers, path))
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
    ) : ResourceItem {
        val record = ValueResourceRecord.createFromString(recordString)
        return ResourceItem(record.type, qualifiers, record.key.asUnderscoredIdentifier(), path.resolve("$offset-$size"))
    }
}

internal fun File.listNotHiddenFiles(): List<File> =
    listFiles()?.filter { !it.isHidden }.orEmpty()

internal fun String.asUnderscoredIdentifier(): String =
    replace('-', '_')
        .let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }