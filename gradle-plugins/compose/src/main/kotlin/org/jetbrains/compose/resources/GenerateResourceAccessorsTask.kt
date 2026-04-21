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
        val qualifiers = parseComposeResourceLocaleQualifiers(dirName) ?: return null

        val typeString = dirName.substringBefore("-").lowercase()
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

    /**
     * Extracts qualifiers from a resource folder name.
     * Handles standard Android qualifiers and BCPF (BCP 47) locale format.
     *
     * values-en-rUS         -> ["en", "rUS"]
     * values-b+sr+Latn      -> ["sr", "Latn"]
     * values-b+zh+Hant-dark -> ["zh", "Hant", "dark"]
     */
    private fun parseComposeResourceLocaleQualifiers(dirName: String): List<String>? {
        val parts = dirName.split("-")
        if (parts.isEmpty()) return null
        if (parts.first().lowercase().isEmpty()) return null

        val expanded = mutableListOf<String>()
        for ((index, q) in parts.drop(1).withIndex()) {
            if (q.startsWith("b+") && index == 0) {
                // Malformed segments pass through so addQualifiers reports "unknown qualifier"
                expanded.addAll(expandBcpQualifier(q) ?: listOf(q))
            } else {
                expanded.add(q)
            }
        }
        return expanded
    }

    /**
     * Expands an Android BCPF `b+lang[+Script][+REGION]` segment into
     * individual qualifier tokens. Returns null for malformed segments.
     *
     * b+sr+Latn+RS -> ["sr", "Latn", "rRS"]
     * b+es+419     -> ["es", "r419"]
     */
    private fun expandBcpQualifier(segment: String): List<String>? {
        val result = mutableListOf<String>()
        for (subtag in segment.removePrefix("b+").split("+")) {
            when {
                subtag.matches(Regex("[a-z]{2,3}")) -> result.add(subtag)
                // Region codes get "r" prefix per Android convention (rUS, r419)
                subtag.matches(Regex("[A-Z]{2}")) -> result.add("r$subtag")
                subtag.matches(Regex("[0-9]{3}")) -> result.add("r$subtag")
                // Script codes stay bare (Latn, Hans, Hant)
                subtag.matches(Regex("[A-Z][a-z]{3}")) -> result.add(subtag)
                else -> return null
            }
        }
        return result
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
