package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.w3c.dom.Node
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

internal fun Project.registerPrepareComposeResourcesTask(
    userComposeResourcesDir: File,
    preparedComposeResourcesDir: Provider<Directory>
): TaskProvider<PrepareComposeResourcesTask> {
    val convertXmlValueResources = tasks.register(
        "convertXmlValueResources",
        XmlValuesConverterTask::class.java
    ) { task ->
        task.originalResourcesDir.set(userComposeResourcesDir)
        task.outputDir.set(preparedComposeResourcesDir)
    }

    val copyNonXmlValueResources = tasks.register(
        "copyNonXmlValueResources",
        CopyNonXmlValueResourcesTask::class.java
    ) { task ->
        task.originalResourcesDir.set(userComposeResourcesDir)
        task.outputDir.set(preparedComposeResourcesDir)
    }

    val prepareComposeResourcesTask = tasks.register(
        "prepareComposeResourcesTask",
        PrepareComposeResourcesTask::class.java
    ) { task ->
        task.convertedXmls.set(convertXmlValueResources.map { it.realOutputFiles.get() })
        task.copiedNonXmls.set(copyNonXmlValueResources.map { it.realOutputFiles.get() })
        task.outputDir.set(preparedComposeResourcesDir.map { it.asFile })
    }

    return prepareComposeResourcesTask
}

internal abstract class CopyNonXmlValueResourcesTask : DefaultTask() {
    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @get:Internal
    abstract val originalResourcesDir: DirectoryProperty

    @get:InputFiles
    val realInputFiles = originalResourcesDir.map { dir ->
        dir.asFileTree.matching { it.exclude("values*/*.xml") }
    }

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:OutputFiles
    val realOutputFiles = outputDir.map { dir ->
        dir.asFileTree.matching { it.exclude("values*/*.${XmlValuesConverterTask.CONVERTED_RESOURCE_EXT}") }
    }

    @TaskAction
    fun run() {
        realOutputFiles.get().forEach { f -> f.delete() }
        fileSystem.copy {  copy ->
            copy.includeEmptyDirs = false
            copy.from(originalResourcesDir) {
                it.exclude("values*/*.xml")
            }
            copy.into(outputDir)
        }
    }
}

internal abstract class PrepareComposeResourcesTask : DefaultTask() {
    @get:InputFiles
    abstract val convertedXmls: Property<FileTree>

    @get:InputFiles
    abstract val copiedNonXmls: Property<FileTree>

    @get:OutputDirectory
    abstract val outputDir: Property<File>

    @TaskAction
    fun run() {}
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

internal abstract class XmlValuesConverterTask : DefaultTask() {
    companion object {
        const val CONVERTED_RESOURCE_EXT = "cvr" //Compose Value Resource
        private const val FORMAT_VERSION = 0
    }

    @get:Internal
    abstract val originalResourcesDir: DirectoryProperty

    @get:InputFiles
    val realInputFiles = originalResourcesDir.map { dir ->
        dir.asFileTree.matching { it.include("values*/*.xml") }
    }

    @get:Internal
    abstract val outputDir: DirectoryProperty

    @get:OutputFiles
    val realOutputFiles = outputDir.map { dir ->
        dir.asFileTree.matching { it.include("values*/*.$CONVERTED_RESOURCE_EXT") }
    }

    @TaskAction
    fun run() {
        val outDir = outputDir.get().asFile
        realOutputFiles.get().forEach { f -> f.delete() }
        originalResourcesDir.get().asFile.listNotHiddenFiles().forEach { valuesDir ->
            if (valuesDir.isDirectory && valuesDir.name.startsWith("values")) {
                valuesDir.listNotHiddenFiles().forEach { f ->
                    if (f.extension.equals("xml", true)) {
                        val output = outDir
                            .resolve(f.parentFile.name)
                            .resolve(f.nameWithoutExtension + ".$CONVERTED_RESOURCE_EXT")
                        output.parentFile.mkdirs()
                        convert(f, output)
                    }
                }
            }
        }
    }

    private fun convert(original: File, converted: File) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(original)
        val items = doc.getElementsByTagName("resources").item(0).childNodes
        val records = List(items.length) { items.item(it) }.mapNotNull { getItemRecord(it)?.getAsString() }
        val fileContent = buildString {
            appendLine("version:$FORMAT_VERSION")
            records.sorted().forEach { appendLine(it) }
        }
        converted.writeText(fileContent)
    }

    private fun getItemRecord(node: Node): ValueResourceRecord? {
        val type = ResourceType.fromString(node.nodeName) ?: return null
        val key = node.attributes.getNamedItem("name").nodeValue
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
            else -> error("Unknown string resource type: '$type'")
        }
        return ValueResourceRecord(type, key, value)
    }

    private fun String.asBase64() =
        Base64.getEncoder().encode(this.encodeToByteArray()).decodeToString()

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
    private fun handleSpecialCharacters(string: String): String {
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
}