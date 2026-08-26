/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Parses generated ledgers for JVM verification tests.
import java.io.File
import org.jetbrains.compose.web.browser.generator.LedgerFile

private fun reportFile(property: String): File {
    val path = requireNotNull(System.getProperty(property)) {
        "The $property system property is not set; see build.gradle.kts"
    }
    return File(path)
}

/** Generator decisions, including structured reasons for omissions. */
internal data class GeneratedCoverageReport(val entries: List<Entry>) {
    data class Entry(
        val status: String,
        val kind: String,
        val subject: String,
        val reason: String?,
        val detail: String,
    ) {
        val ported: Boolean get() = status == "PORTED"
    }

    fun of(kind: String): List<Entry> = entries.filter { it.kind == kind }
    fun entry(kind: String, subject: String): Entry = of(kind).single { it.subject == subject }

    companion object {
        fun read(): GeneratedCoverageReport = GeneratedCoverageReport(
            LedgerFile.read(reportFile("portableDomCoverage")).sections.map { section ->
                val fields = section.fields
                val skipped = fields[0] == "SKIPPED"
                Entry(
                    fields[0],
                    fields[1],
                    fields[2],
                    fields.getOrNull(3).takeIf { skipped },
                    fields.getOrElse(if (skipped) 4 else 3) { "" },
                )
            },
        )
    }
}

/** Exhaustive account of every declaration in the configured browser input files. */
internal data class GeneratedApiManifest(val header: Map<String, String>, val entries: List<Entry>) {
    data class Entry(
        val status: String,
        val kind: String,
        val subject: String,
        val reason: String,
        val detail: String,
    ) {
        val emitted: Boolean get() = status == "EMITTED"
        val owner: String get() = subject.substringBefore('#').removeSuffix(".Companion")
    }

    fun count(key: String): Int = header.getValue(key).toInt()
    fun of(kind: String): List<Entry> = entries.filter { it.kind == kind }

    companion object {
        fun read(): GeneratedApiManifest = parse(reportFile("portableDomApiManifest"))
        fun lines(): List<String> = reportFile("portableDomApiManifest").readLines()
        fun baselineLines(): List<String> = reportFile("portableDomApiManifestBaseline").readLines()

        private fun parse(file: File): GeneratedApiManifest {
            val ledger = LedgerFile.read(file)
            return GeneratedApiManifest(
                ledger.header,
                ledger.sections.map {
                    val fields = it.fields
                    Entry(
                        fields[0],
                        fields[1],
                        fields[2],
                        fields.getOrElse(3) { "" },
                        fields.getOrElse(4) { "" },
                    )
                },
            )
        }
    }
}

/** The emitted classifier model and its nested declaration lines. */
internal data class GeneratedModelReport(
    val counts: Map<String, Int>,
    val declarations: List<Declaration>,
) {
    data class Declaration(
        val name: String,
        val parent: String?,
        val superinterfaces: List<String>,
        val kind: String,
        val memberCount: Int,
        val origin: String,
        val members: List<String>,
        val constructors: List<String>,
        val values: List<String>,
    ) {
        val simpleName: String get() = name.substringAfterLast('.')
        val portableName: String get() {
            val browserPackage = name.substringBeforeLast('.')
            val portablePackage = if (browserPackage.startsWith("org.w3c.")) {
                browserPackage.replaceFirst("org.w3c", "kotlinx.browser")
            } else {
                PORTABLE_PACKAGES.getValue(browserPackage)
            }
            return "$portablePackage.$simpleName"
        }
    }

    val byName: Map<String, Declaration> = declarations.associateBy(Declaration::name)
    val order: List<String> = declarations.map(Declaration::name)
    val selected: List<Declaration> = declarations.filter { it.origin != "supertype" }

    fun signatureTypes(declaration: Declaration): List<String> =
        (declaration.members + declaration.constructors).flatMap { signature ->
            QUALIFIED_NAME.findAll(signature).map { it.value }.toList()
        }

    companion object {
        private val PORTABLE_PACKAGES = mapOf(
            "org.khronos.webgl" to "kotlinx.browser.webgl",
        )
        val INTEROP_TYPES = setOf(
            "kotlinx.browser.JsAny",
            "kotlinx.browser.JsString",
            "kotlinx.browser.JsNumber",
            "kotlinx.browser.JsDouble",
            "kotlinx.browser.JsArray",
            "kotlinx.browser.Promise",
        )
        private val QUALIFIED_NAME = Regex("""[a-z][\w.]*\.[A-Z]\w*""")
        private val CONSTRUCTOR = Regex("""^(primary|secondary) constructor\(""")
        private const val VALUE = "value "

        fun read(): GeneratedModelReport {
            val ledger = LedgerFile.read(reportFile("portableDomModel"))
            val declarations = ledger.sections.map { section ->
                val fields = section.fields
                val lines = section.lines
                Declaration(
                    fields[0],
                    fields[1].ifEmpty { null },
                    fields[2].split(',').filter(String::isNotEmpty),
                    fields[3],
                    fields[4].toInt(),
                    fields[5],
                    lines.filterNot(::isMetadata),
                    lines.filter(CONSTRUCTOR::containsMatchIn),
                    lines.withPrefix(VALUE),
                )
            }
            return GeneratedModelReport(ledger.header.mapValues { it.value.toInt() }, declarations)
        }

        private fun List<String>.withPrefix(prefix: String): List<String> =
            filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }

        private fun isMetadata(line: String): Boolean =
            CONSTRUCTOR.containsMatchIn(line) ||
                line.startsWith(VALUE)
    }
}
