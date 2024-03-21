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
    abstract val mainDir: DirectoryProperty

    @get:OutputDirectory
    abstract val testDir: DirectoryProperty

    @TaskAction
    fun generatePluralRuleLists() {
        generateDirectories()

        val pluralRuleLists = parsePluralRuleLists()

        val mainContent = generateMainContent(pluralRuleLists)
        mainDir.get().asFile.resolve("cldr.kt").writeText(mainContent)

        val testContent = generateTestContent(pluralRuleLists)
        testDir.get().asFile.resolve("cldr.test.kt").writeText(testContent)
    }

    private fun generateDirectories() {
        for (directoryProperty in arrayOf(mainDir, testDir)) {
            val directory = directoryProperty.get().asFile
            if (directory.exists()) {
                directory.deleteRecursively()
            }
            directory.mkdirs()
        }
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
                    val rule = pluralRule.text().split('@')
                    PluralRule(
                        pluralRule.attribute("count").toString(),
                        // trim samples as not needed
                        rule[0].trim(),
                        rule.firstOrNull { it.startsWith("integer") }?.substringAfter("integer")?.trim() ?: "",
                        rule.firstOrNull { it.startsWith("decimal") }?.substringAfter("decimal")?.trim() ?: "",
                    )
                }
            )
        }
    }

    private fun generateMainContent(pluralRuleLists: List<PluralRuleList>): String {
        val pluralRuleListIndexByLocale = pluralRuleLists.flatMapIndexed { idx, pluralRuleList ->
            pluralRuleList.locales.map { locale ->
                locale to idx
            }
        }

        return """
            package org.jetbrains.compose.resources.intl
            
            internal val cldrPluralRuleListIndexByLocale = mapOf(
                ${pluralRuleListIndexByLocale.joinToString { (locale, idx) -> "\"$locale\" to $idx" }}
            )
            
            internal val cldrPluralRuleLists = arrayOf(${
                pluralRuleLists.joinToString { pluralRuleList ->
                    """
                        arrayOf(${
                            pluralRuleList.rules.joinToString { rule ->
                                "PluralCategory.${rule.count.uppercase()} to \"${rule.rule}\""
                            }
                        })
                    """.trimIndent()
                }
            })
        """.trimIndent()
    }

    private fun generateTestContent(pluralRuleLists: List<PluralRuleList>): String {
        val pluralRuleIntegerSamplesByLocale = pluralRuleLists.flatMap { pluralRuleList ->
            pluralRuleList.locales.map { locale ->
                locale to pluralRuleList.rules.map { it.count to it.integerSample }
            }
        }

        return """
            package org.jetbrains.compose.resources
            
            import org.jetbrains.compose.resources.intl.PluralCategory
            
            internal val cldrPluralRuleIntegerSamples = arrayOf(${
                pluralRuleIntegerSamplesByLocale.joinToString { (locale, samples) ->
                    """"$locale" to arrayOf(${
                        samples.joinToString { (count, sample) ->
                            "PluralCategory.${count.uppercase()} to \"$sample\""
                        }
                    } )""".trimIndent()
                }
            })
        """.trimIndent()
    }
}

private data class PluralRuleList(
    val locales: List<String>,
    val rules: List<PluralRule>,
)

private data class PluralRule(
    val count: String,
    val rule: String,
    val integerSample: String,
    val decimalSample: String,
)