package org.jetbrains.compose.resources

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.internal.IdeaImportTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.w3c.dom.Node
import org.xml.sax.SAXParseException
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

internal fun Project.registerPrepareComposeResourcesTask(
    sourceSet: KotlinSourceSet,
    config: Provider<ResourcesExtension>
): TaskProvider<PrepareComposeResourcesTask> {
    val userComposeResourcesDir: Provider<Directory> = config.flatMap { ext ->
        ext.customResourceDirectories[sourceSet.name] ?: provider {
            //default path
            layout.projectDirectory.dir("src/${sourceSet.name}/$COMPOSE_RESOURCES_DIR")
        }
    }

    val preparedComposeResourcesDir = layout.buildDirectory.dir(
        "$RES_GEN_DIR/preparedResources/${sourceSet.name}/$COMPOSE_RESOURCES_DIR"
    )

    val convertXmlValueResources = tasks.register(
        "convertXmlValueResourcesFor${sourceSet.name.uppercaseFirstChar()}",
        XmlValuesConverterTask::class.java
    ) { task ->
        task.fileSuffix.set(sourceSet.name)
        task.originalResourcesDir.set(userComposeResourcesDir)
        task.outputDir.set(preparedComposeResourcesDir)
    }

    val copyNonXmlValueResources = tasks.register(
        "copyNonXmlValueResourcesFor${sourceSet.name.uppercaseFirstChar()}",
        CopyNonXmlValueResourcesTask::class.java
    ) { task ->
        task.originalResourcesDir.set(userComposeResourcesDir)
        task.outputDir.set(preparedComposeResourcesDir)
    }

    val prepareComposeResourcesTask = tasks.register(
        getPrepareComposeResourcesTaskName(sourceSet),
        PrepareComposeResourcesTask::class.java
    ) { task ->
        task.convertedXmls.set(convertXmlValueResources.map { it.realOutputFiles.get() })
        task.copiedNonXmls.set(copyNonXmlValueResources.map { it.realOutputFiles.get() })
        task.outputDir.set(preparedComposeResourcesDir)
    }

    return prepareComposeResourcesTask
}

internal fun Project.getPreparedComposeResourcesDir(sourceSet: KotlinSourceSet): Provider<File> = tasks
    .named(
        getPrepareComposeResourcesTaskName(sourceSet),
        PrepareComposeResourcesTask::class.java
    )
    .flatMap { it.outputDir.asFile }

private fun getPrepareComposeResourcesTaskName(sourceSet: KotlinSourceSet) =
    "prepareComposeResourcesTaskFor${sourceSet.name.uppercaseFirstChar()}"

internal abstract class CopyNonXmlValueResourcesTask : IdeaImportTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Internal
    abstract val originalResourcesDir: DirectoryProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    val realInputFiles = originalResourcesDir.map { dir ->
        dir.asFileTree.matching { it.exclude("values*/*.xml") }
    }

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:OutputFiles
    val realOutputFiles = outputDir.map { dir ->
        dir.asFileTree.matching { it.exclude("values*/*.${XmlValuesConverterTask.CONVERTED_RESOURCE_EXT}") }
    }

    override fun safeAction() {
        realOutputFiles.get().forEach { f -> f.delete() }
        fileSystem.copy { copy ->
            copy.includeEmptyDirs = false
            copy.from(originalResourcesDir) {
                it.exclude("values*/*.xml")
            }
            copy.into(outputDir)
        }
    }
}

internal abstract class PrepareComposeResourcesTask : IdeaImportTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    abstract val convertedXmls: Property<FileTree>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    abstract val copiedNonXmls: Property<FileTree>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    override fun safeAction() = Unit
}

internal data class ValueResourceRecord(
    val type: ResourceType,
    val key: String,
    val content: String
) {
    fun getAsString(): String {
        return listOf(type.typeName, key, content).joinToString(SEPARATOR)
    }

    companion object {
        private const val SEPARATOR = "|"
        fun createFromString(string: String): ValueResourceRecord {
            val parts = string.split(SEPARATOR)
            return ValueResourceRecord(
                ResourceType.fromString(parts[0])!!,
                parts[1],
                parts[2]
            )
        }
    }
}

internal abstract class XmlValuesConverterTask : IdeaImportTask() {
    companion object {
        const val CONVERTED_RESOURCE_EXT = "cvr" //Compose Value Resource
        private const val FORMAT_VERSION = 0
    }

    @get:Input
    abstract val fileSuffix: Property<String>

    @get:Internal
    abstract val originalResourcesDir: DirectoryProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    val realInputFiles = originalResourcesDir.map { dir ->
        dir.asFileTree.matching { it.include("values*/*.xml") }
    }

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:OutputFiles
    val realOutputFiles = outputDir.map { dir ->
        val suffix = fileSuffix.get()
        dir.asFileTree.matching { it.include("values*/*.$suffix.$CONVERTED_RESOURCE_EXT") }
    }

    override fun safeAction() {
        val outDir = outputDir.get().asFile
        val suffix = fileSuffix.get()
        realOutputFiles.get().forEach { f -> f.delete() }
        originalResourcesDir.get().asFile.listNotHiddenFiles().forEach { valuesDir ->
            if (valuesDir.isDirectory && valuesDir.name.startsWith("values")) {
                valuesDir.listNotHiddenFiles().forEach { f ->
                    if (f.extension.equals("xml", true)) {
                        val output = outDir
                            .resolve(f.parentFile.name)
                            .resolve(f.nameWithoutExtension + ".$suffix.$CONVERTED_RESOURCE_EXT")
                        output.parentFile.mkdirs()
                        try {
                            convert(f, output)
                        } catch (e: SAXParseException) {
                            error("XML file ${f.absolutePath} is not valid. Check the file content.")
                        } catch (e: Exception) {
                            error("XML file ${f.absolutePath} is not valid. ${e.message}")
                        }
                    }
                }
            }
        }
    }

    private fun convert(original: File, converted: File) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(original)
        val items = doc.getElementsByTagName("resources").item(0).childNodes
        val records = List(items.length) { items.item(it) }
            .filter { it.hasAttributes() }
            .map { getItemRecord(it) }

        //check there are no duplicates type + key
        records.groupBy { it.key }
            .filter { it.value.size > 1 }
            .forEach { (key, records) ->
                val allTypes = records.map { it.type }
                require(allTypes.size == allTypes.toSet().size) { "Duplicated key '$key'." }
            }

        val fileContent = buildString {
            appendLine("version:$FORMAT_VERSION")
            records.map { it.getAsString() }.sorted().forEach { appendLine(it) }
        }
        converted.writeText(fileContent)
    }

    private fun getItemRecord(node: Node): ValueResourceRecord {
        val type = ResourceType.fromString(node.nodeName) ?: error("Unknown resource type: '${node.nodeName}'.")
        val key = node.attributes.getNamedItem("name")?.nodeValue ?: error("Attribute 'name' not found.")
        val value: String
        when (type) {
            ResourceType.STRING -> {
                val content = handleSpecialCharacters(node.textContent)
                value = content.asBase64()
            }

            ResourceType.STRING_ARRAY -> {
                val children = node.childNodes
                value = List(children.length) { children.item(it) }
                    .filter { it.nodeName == "item" }
                    .joinToString(",") { child ->
                        val content = handleSpecialCharacters(child.textContent)
                        content.asBase64()
                    }
            }

            ResourceType.PLURAL_STRING -> {
                val children = node.childNodes
                value = List(children.length) { children.item(it) }
                    .filter { it.nodeName == "item" }
                    .joinToString(",") { child ->
                        val content = handleSpecialCharacters(child.textContent)
                        val quantity = child.attributes.getNamedItem("quantity").nodeValue
                        quantity.uppercase() + ":" + content.asBase64()
                    }
            }

            else -> error("Unknown string resource type: '$type'.")
        }
        return ValueResourceRecord(type, key, value)
    }

    private fun String.asBase64() =
        Base64.getEncoder().encode(this.encodeToByteArray()).decodeToString()
}

//https://developer.android.com/guide/topics/resources/string-resource#escaping_quotes
/**
 * Replaces
 *
 * '\n' -> new line
 *
 * '\t' -> tab
 *
 * '\uXXXX' -> unicode symbol
 *
 * '\\' -> '\'
 *
 * @param string The input string to handle.
 * @return The string with special characters replaced according to the logic.
 */
internal fun handleSpecialCharacters(string: String): String {
    val unicodeNewLineTabRegex = Regex("""\\u[a-fA-F\d]{4}|\\n|\\t""")
    val doubleSlashRegex = Regex("""\\\\""")
    val doubleSlashIndexes = doubleSlashRegex.findAll(string).map { it.range.first }
    val handledString = unicodeNewLineTabRegex.replace(string) { matchResult ->
        if (doubleSlashIndexes.contains(matchResult.range.first - 1)) matchResult.value
        else when (matchResult.value) {
            "\\n" -> "\n"
            "\\t" -> "\t"
            else -> matchResult.value.substring(2).toInt(16).toChar().toString()
        }
    }.replace("""\\""", """\""")
    return handledString
}