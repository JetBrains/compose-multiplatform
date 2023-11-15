package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.relativeTo

private const val RES_FILE = "Res"
private const val INDEX_FILE = "resources.index"

/**
 * This task should be FAST and SAFE! Because it is being run during IDE import.
 */
@CacheableTask
abstract class GenerateResClassTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>
    
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: DirectoryProperty

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    @get:OutputDirectory
    abstract val indexDir: DirectoryProperty

    init {
        this.onlyIf { resDir.asFile.get().exists() }
    }

    @TaskAction
    fun generate() {
        try {
            val rootResDir = resDir.get().asFile
            logger.info("Generate resources for $rootResDir")

            //get first level dirs
            val dirs = rootResDir.listFiles { f -> f.isDirectory }.orEmpty()

            //type -> id -> resource item
            val resources: Map<String, Map<String, List<ResourceItem>>> = dirs
                .flatMap { dir ->
                    dir.listFiles { f -> !f.isDirectory }
                        .orEmpty()
                        .mapNotNull { it.fileToResourceItems(rootResDir.toPath()) }
                        .flatten()
                }
                .groupBy { it.type }
                .mapValues { (_, items) -> items.groupBy { it.id } }

            val kotlinDir = codeDir.get().asFile
            kotlinDir.mkdirs()
            getResFileSpec(resources, packageName.get(), RES_FILE).writeTo(kotlinDir)

            val outIndexFile = indexDir.get().asFile
            outIndexFile.mkdirs()
            outIndexFile.resolve(INDEX_FILE).writeText(generateResourceIndex(resources))
        } catch (e: Exception) {
            //message must contain two ':' symbols to be parsed by IDE UI!
            logger.error("e: GenerateResClassTask was failed:", e)
        }
    }

    private fun File.fileToResourceItems(
        relativeTo: Path
    ): List<ResourceItem>? {
        val file = this
        if (file.isDirectory) return null
        val dirName = file.parentFile.name ?: return null
        val typeAndQualifiers = dirName.lowercase().split("-")
        if (typeAndQualifiers.isEmpty()) return null

        val type = typeAndQualifiers.first().lowercase()
        val qualifiers = typeAndQualifiers.takeLast(typeAndQualifiers.size - 1).map { it.lowercase() }.toSet()
        val path = file.toPath().relativeTo(relativeTo)

        return if (type == "values" && file.name.equals("strings.xml", true)) {
            val stringIds = getStringIds(file)
            stringIds.map { strId ->
                ResourceItem("strings", qualifiers, strId.lowercase(), path)
            }
        } else {
            listOf(ResourceItem(type, qualifiers, file.nameWithoutExtension.lowercase(), path))
        }
    }

    private val stringTypeNames = listOf("string", "string-array")
    private fun getStringIds(stringsXml: File): Set<String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsXml)
        val items = doc.getElementsByTagName("resources").item(0).childNodes
        val ids = List(items.length) { items.item(it) }
            .filter { it.nodeName in stringTypeNames }
            .map { it.attributes.getNamedItem("name").nodeValue }
        return ids.toSet()
    }
}