// Reads and writes the deterministic ledger format used by generated reports.
package prototype.dom.generator

import java.io.File
import java.io.Writer

/** The shared on-disk representation used by coverage, model, and API-manifest reports. */
public data class LedgerFile(
    val header: Map<String, String>,
    val sections: List<Section>,
) {
    public data class Section(
        val fields: List<String>,
        val lines: List<String> = emptyList(),
    )

    public fun writeTo(writer: Writer) {
        header.forEach { (name, value) -> writer.appendLine("$name=$value") }
        sections.forEach { section ->
            writer.appendLine(section.fields.joinToString("|", transform = String::ledgerField))
            section.lines.forEach { writer.append("  ").appendLine(it) }
        }
    }

    public companion object {
        public fun read(file: File): LedgerFile {
            val header = linkedMapOf<String, String>()
            val sections = mutableListOf<Section>()
            var fields: List<String>? = null
            val lines = mutableListOf<String>()

            fun flush() {
                fields?.let { sections += Section(it, lines.toList()) }
                lines.clear()
            }

            file.forEachLine { line ->
                when {
                    line.isBlank() -> Unit
                    line.startsWith("  ") -> lines += line.removePrefix("  ")
                    '|' in line -> {
                        flush()
                        fields = line.split(FIELD).map { it.replace("\\|", "|") }
                    }
                    else -> header[line.substringBefore('=')] = line.substringAfter('=')
                }
            }
            flush()
            return LedgerFile(header, sections)
        }

        private val FIELD = Regex("""(?<!\\)\|""")
    }
}

internal val Enum<*>.slug: String
    get() = name.lowercase().replace('_', '-')

private fun String.ledgerField(): String =
    replace("|", "\\|").replace('\n', ' ').replace('\r', ' ')
