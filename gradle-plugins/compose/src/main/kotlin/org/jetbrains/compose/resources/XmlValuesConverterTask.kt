package org.jetbrains.compose.resources

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.w3c.dom.Node
import java.io.File
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

internal data class ValueResourceRecord(
    val type: ResourceType,
    val key: String,
    val content: String
) {
    fun getAsString(): String {
        return listOf(type.typeName, key, content).joinToString("#")
    }

    companion object {
        fun createFromString(string: String): ValueResourceRecord {
            val parts = string.split("#")
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
    }

    @get:InputFiles
    abstract val originalResourcesDir: Property<File>

    @get:OutputDirectory
    abstract val outputDir: Property<File>

    @TaskAction
    fun run() {
        val dir = outputDir.get()
        dir.deleteRecursively()
        originalResourcesDir.get().listNotHiddenFiles().forEach { dir ->
            if (dir.isDirectory && dir.name.startsWith("values")) {
                dir.listNotHiddenFiles().forEach { f ->
                    if (f.extension.equals("xml", true)) {
                        val output = dir
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
        converted.writeText(records.sorted().joinToString("\n"))
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