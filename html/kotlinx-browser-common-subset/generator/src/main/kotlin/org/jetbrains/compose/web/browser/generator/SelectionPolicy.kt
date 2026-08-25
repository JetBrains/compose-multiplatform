/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Defines and parses the facade selection policy.
package org.jetbrains.compose.web.browser.generator

/** How a browser classifier named by [SelectionPolicy] participates in the facade. */
internal enum class ClassifierDisposition {
    /** Emit the named classifier and every portable declaration on it. */
    EMIT,

    /** Leave the classifier and everything declared under it for a named future expansion. */
    DEFER,
}

internal data class ClassifierSelection(
    val disposition: ClassifierDisposition,
    /** The manifest exclusion reason for [ClassifierDisposition.DEFER], empty for every other state. */
    val reason: String = "",
)

/**
 * The one source of truth for the facade boundary.
 *
 * Every top-level classifier in [inputFiles] defaults to [ClassifierDisposition.EMIT]. Entries in
 * [classifiers] override that default by deferring a classifier to a future expansion.
 * [signatureOnlyPackages] allow bare identities reached from signatures and make package classifiers
 * eligible for normal emission when reached through inheritance.
 */
internal class SelectionPolicy private constructor(
    val inputFiles: Set<String>,
    val signatureOnlyPackages: Set<String>,
    val classifiers: Map<String, ClassifierSelection>,
) {
    /** Explicit omissions that signature discovery must not silently select again. */
    val excludedFromClosure: Set<String> = classifiers
        .filterValues { it.disposition == ClassifierDisposition.DEFER }
        .keys

    /** Classifier-level manifest exclusions, in the format [ManifestExclusions.parse] consumes. */
    val deferredExclusionLines: List<String> = classifiers
        .filterValues { it.disposition == ClassifierDisposition.DEFER }
        .map { (qualifiedName, selection) -> "${selection.reason} $qualifiedName" }
        .sorted()

    fun dispositionOf(qualifiedName: String): ClassifierDisposition =
        classifiers[qualifiedName]?.disposition ?: ClassifierDisposition.EMIT

    /** The exact-file classifiers selected as named facade declarations. */
    fun emittedClassifiers(inputClassifiers: Set<String>): Set<String> = inputClassifiers
        .filterTo(sortedSetOf()) { dispositionOf(it) == ClassifierDisposition.EMIT }

    /** Policy entries must name a classifier in an input file. */
    fun validationErrors(inputClassifiers: Set<String>): List<String> = buildList {
        classifiers.forEach { (qualifiedName, selection) ->
            if (qualifiedName !in inputClassifiers) {
                add("${selection.disposition.name.lowercase()} classifier is not declared by an input file: $qualifiedName")
            }
        }
    }.sorted()

    companion object {
        private val REASON = Regex("[a-z0-9-]+")
        /**
         * Lines are one of:
         *
         * ```text
         * input <file-name>
         * signature-only-package <browser-package>
         * defer <reason> <qualified-classifier>
         * ```
         */
        fun parse(lines: List<String>): SelectionPolicy {
            val inputFiles = linkedSetOf<String>()
            val signatureOnlyPackages = linkedSetOf<String>()
            val classifiers = linkedMapOf<String, ClassifierSelection>()

            lines.forEach { line ->
                val fields = line.trim().split(Regex("\\s+"))
                require(fields.isNotEmpty()) { "Empty selection-policy line" }

                if (fields.first() == "input") {
                    require(fields.size == 2) { "Malformed input selection, expected `input <file-name>`: $line" }
                    require(inputFiles.add(fields[1])) { "Duplicate input file in selection policy: ${fields[1]}" }
                    return@forEach
                }
                if (fields.first() == "signature-only-package") {
                    require(fields.size == 2) { "Malformed signature-only package selection: $line" }
                    require('.' in fields[1]) { "Selection policy names an unqualified package: ${fields[1]}" }
                    require(signatureOnlyPackages.add(fields[1])) {
                        "Duplicate signature-only package in selection policy: ${fields[1]}"
                    }
                    return@forEach
                }

                val directive = fields.first()
                val (qualifiedName, selection) = when (directive) {
                    "defer" -> {
                        require(fields.size == 3) {
                            "Malformed deferred selection, expected `defer <reason> <classifier>`: $line"
                        }
                        require(fields[1].matches(REASON)) { "Malformed deferred-selection reason `${fields[1]}`: $line" }
                        fields[2] to ClassifierSelection(ClassifierDisposition.DEFER, fields[1])
                    }
                    else -> error("Unknown selection-policy directive `$directive`: $line")
                }

                require('.' in qualifiedName) { "Selection policy names an unqualified classifier: $qualifiedName" }
                val previous = classifiers.put(qualifiedName, selection)
                require(previous == null) { "Duplicate classifier in selection policy: $qualifiedName" }
            }

            require(inputFiles.isNotEmpty()) { "Selection policy names no input files" }
            return SelectionPolicy(
                inputFiles.toSortedSet(),
                signatureOnlyPackages.toSortedSet(),
                classifiers.toSortedMap(),
            )
        }
    }
}
