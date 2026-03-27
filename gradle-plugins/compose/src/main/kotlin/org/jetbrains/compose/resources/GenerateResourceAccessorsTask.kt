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
import org.jetbrains.compose.internal.IdeaImportTask
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import java.io.File
import java.nio.file.Path
import kotlin.io.path.relativeTo

internal abstract class GenerateResourceAccessorsTask : IdeaImportTask() {
    @get:Input
    abstract val packageName: Property<String>

    @get:Input
    abstract val resClassName: Property<String>

    @get:Input
    abstract val sourceSetName: Property<String>

    @get:Input
    @get:Optional
    abstract val packagingDir: Property<File>

    @get:Input
    abstract val makeAccessorsPublic: Property<Boolean>

    @get:Input
    abstract val disableResourceContentHashGeneration: Property<Boolean>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: Property<File>

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    override fun safeAction() {
        val kotlinDir = codeDir.get().asFile
        val rootResDir = resDir.get()
        val sourceSet = sourceSetName.get()

        logger.info("Clean directory $kotlinDir")
        kotlinDir.deleteRecursively()
        kotlinDir.mkdirs()

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
        val resClassName = resClassName.get()
        val isPublic = makeAccessorsPublic.get()
        val generateResourceContentHashAnnotation = !disableResourceContentHashGeneration.get()
        getAccessorsSpecs(
            resources,
            pkgName,
            sourceSet,
            moduleDirectory,
            resClassName,
            isPublic,
            generateResourceContentHashAnnotation
        ).forEach { it.writeTo(kotlinDir) }
    }

    private fun File.isTextResourceFile(): Boolean =
        path.endsWith(".xml", true) || path.endsWith(".svg", true)

    private fun File.resourceContentHash(): Int {
        if ((currentOS == OS.Windows) && isTextResourceFile()) {
            // Windows has different line endings in comparison with Unixes,
            // thus text resource files binary differ there, so we need to handle this.
            return readText().replace("\r\n", "\n").toByteArray().contentHashCode()
        } else {
            return readBytes().contentHashCode()
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
        return listOf(
            ResourceItem(
                type,
                qualifiers,
                file.nameWithoutExtension.asUnderscoredIdentifier(),
                path,
                file.resourceContentHash()
            )
        )
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
        return ResourceItem(
            record.type,
            qualifiers,
            record.key.asUnderscoredIdentifier(),
            path,
            record.content.hashCode(),
            offset,
            size
        )
    }
}

internal fun File.listNotHiddenFiles(): List<File> =
    listFiles()?.filter { !it.isHidden }.orEmpty()

internal fun String.asUnderscoredIdentifier(): String =
    replace('-', '_')
        .let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }
