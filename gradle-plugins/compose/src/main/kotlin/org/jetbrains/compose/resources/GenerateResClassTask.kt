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

    @get:OutputDirectory
    abstract val codeDir: DirectoryProperty

    @TaskAction
    fun generate() {
        try {
            val kotlinDir = codeDir.get().asFile
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

        if (typeString == "values" && file.name.equals("strings.xml", true)) {
            return getStringResources(file).mapNotNull { (typeName, strId) ->
                val type = when(typeName) {
                    "string", "string-array" -> ResourceType.STRING
                    "plurals" -> ResourceType.PLURAL_STRING
                    else -> return@mapNotNull null
                }
                ResourceItem(type, qualifiers, strId.asUnderscoredIdentifier(), path)
            }
        }

        val type = ResourceType.fromString(typeString)
        return listOf(ResourceItem(type, qualifiers, file.nameWithoutExtension.asUnderscoredIdentifier(), path))
    }

    //type -> id
    private val stringTypeNames = listOf("string", "string-array", "plurals")
    private fun getStringResources(stringsXml: File): List<Pair<String, String>> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsXml)
        val items = doc.getElementsByTagName("resources").item(0).childNodes
        return List(items.length) { items.item(it) }
            .filter { it.nodeName in stringTypeNames }
            .map { it.nodeName to it.attributes.getNamedItem("name").nodeValue }
    }

    private fun File.listNotHiddenFiles(): List<File> =
        listFiles()?.filter { !it.isHidden }.orEmpty()
}

internal fun String.asUnderscoredIdentifier(): String =
    replace('-', '_')
        .let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }