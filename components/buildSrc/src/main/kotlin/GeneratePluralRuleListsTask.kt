/*
 * Copyright 2020-2024 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import groovy.util.Node
import groovy.xml.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*

/**
 * Reads a pluralization rules XML file from Unicode's CLDR and generates a Kotlin file that holds the XML content as
 * arrays. This Task is required for quantity string resource support.
 */
@CacheableTask
abstract class GeneratePluralRuleListsTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val pluralsFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generatePluralRuleLists() {
        val outputDir = outputDir.get().asFile
        if (outputDir.exists()) {
            outputDir.deleteRecursively()
        }
        outputDir.mkdirs()

        val pluralRuleLists = parsePluralRuleLists()
        val pluralRuleListIndexByLocale = pluralRuleLists.flatMapIndexed { idx, pluralRuleList ->
            pluralRuleList.locales.map { locale ->
                locale to idx
            }
        }

        val fileContent = """
            package org.jetbrains.compose.resources.intl
            
            internal val cldrPluralRuleListIndexByLocale = mapOf(
                ${pluralRuleListIndexByLocale.joinToString { (locale, idx) -> "\"$locale\" to $idx" }}
            )
            
            internal val cldrPluralRuleLists = arrayOf(
                ${
            pluralRuleLists.joinToString { pluralRuleList ->
                """
                        arrayOf(${
                    pluralRuleList.rules.joinToString { rule ->
                        "PluralCategory.${rule.count.uppercase()} to \"${rule.rule}\""
                    }
                })
                    """.trimIndent()
            }
        }
            )
        """.trimIndent()

        outputDir.resolve("generated.kt").writeText(fileContent)
    }

    private fun parsePluralRuleLists(): List<PluralRuleList> {
        val parser = XmlParser(false, false).apply {
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
        }
        val supplementalData = parser.parse(pluralsFile.get().asFile)
        val pluralRuleLists = supplementalData.children().filterIsInstance<Node>().first { it.name() == "plurals" }

        return pluralRuleLists.children().filterIsInstance<Node>().map { pluralRules ->
            val locales = pluralRules.attribute("locales").toString().split(' ')
            PluralRuleList(
                locales,
                pluralRules.children().filterIsInstance<Node>().map { pluralRule ->
                    PluralRule(
                        pluralRule.attribute("count").toString(),
                        // trim samples as not needed
                        pluralRule.text().split('@')[0].trim(),
                    )
                }
            )
        }
    }
}

private data class PluralRuleList(
    val locales: List<String>,
    val rules: List<PluralRule>,
)

private data class PluralRule(
    val count: String,
    val rule: String,
)