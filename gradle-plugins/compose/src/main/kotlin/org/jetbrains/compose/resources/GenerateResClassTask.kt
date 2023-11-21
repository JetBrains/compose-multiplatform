package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import java.io.File
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.relativeTo

/**
 * This task should be FAST and SAFE! Because it is being run during IDE import.
 */
abstract class GenerateResClassTask : DefaultTask() {
    @get:Input
    abstract val packageName: Property<String>
    
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resDir: DirectoryProperty

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

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
            val resources: Map<ResourceType, Map<String, List<ResourceItem>>> = dirs
                .flatMap { dir ->
                    dir.listFiles { f -> !f.isDirectory }
                        .orEmpty()
                        .mapNotNull { it.fileToResourceItems(rootResDir.parentFile.toPath()) }
                        .flatten()
                }
                .groupBy { it.type }
                .mapValues { (_, items) -> items.groupBy { it.name } }

            val kotlinDir = codeDir.get().asFile
            kotlinDir.deleteRecursively()
            kotlinDir.mkdirs()
            getResFileSpec(resources, packageName.get()).writeTo(kotlinDir)
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

        val typeString = typeAndQualifiers.first().lowercase()
        val qualifiers = typeAndQualifiers.takeLast(typeAndQualifiers.size - 1).map { it.lowercase() }.toSet()
        val path = file.toPath().relativeTo(relativeTo)

        return if (typeString == "values" && file.name.equals("strings.xml", true)) {
            val stringIds = getStringIds(file)
            stringIds.map { strId ->
                ResourceItem(ResourceType.STRING, qualifiers, strId.lowercase(), path)
            }
        } else {
            val type = try {
                ResourceType.fromString(typeString)
            } catch (e: Exception) {
                logger.error("e: Error: $path", e)
                return null
            }
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