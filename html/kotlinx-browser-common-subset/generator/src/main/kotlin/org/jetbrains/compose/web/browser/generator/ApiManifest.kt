/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Builds and validates the manifest that accounts for every selected browser declaration.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.symbol.Variance

/** Browser declaration kinds kept separate so manifest totals can be checked against input files. */
internal enum class ManifestKind(val coverageKind: CoverageKind) {
    CLASSIFIER(CoverageKind.CLASSIFIER),
    CONSTRUCTOR(CoverageKind.CONSTRUCTOR),
    MEMBER(CoverageKind.MEMBER),
    COMPANION(CoverageKind.COMPANION),
    COMPANION_MEMBER(CoverageKind.COMPANION_MEMBER),
    NESTED_DECLARATION(CoverageKind.NESTED_DECLARATION),
    FACTORY(CoverageKind.FACTORY),
    // The coverage ledger groups both under top-level extensions; their signatures distinguish them.
    OPERATOR(CoverageKind.TOP_LEVEL_EXTENSION),
    VALUE(CoverageKind.TOP_LEVEL_EXTENSION);

    /** The header label, which is the slug counted: `classifiers`, `factories`, `companion-members`. */
    val plural = if (slug.endsWith('y')) slug.dropLast(1) + "ies" else slug + "s"
}

/** Manifest outcome for one source declaration; [UNACCOUNTED] aborts generation. */
internal enum class ManifestStatus {
    EMITTED,
    EXCLUDED,
    UNACCOUNTED,
}

/** One source declaration and what the facade did with it. */
internal data class ManifestEntry(
    val kind: ManifestKind,
    /** The ledger key: the declaring classifier, `#`, and [KSDeclaration.coverageSignature]. */
    val key: String,
    /** The reviewable form of [key], with parameter types, defaults and result types spelled out. */
    val subject: String,
    val status: ManifestStatus,
    val reason: String,
    val detail: String,
)

/** Explicit exclusions; classifier entries cover their declarations and unused entries are stale. */
internal class ManifestExclusions(private val reasonBySubject: Map<String, String>) {
    private val used = mutableSetOf<String>()

    /** The reason covering [key], from the key itself or from the classifier it is declared in. */
    fun reasonFor(key: String): String? {
        val subject = reasonBySubject.keys.filter { key.isCoveredBy(it) }.maxByOrNull(String::length)
            ?: return null
        used += subject
        return reasonBySubject.getValue(subject)
    }

    /** Entries that covered nothing. A stale exclusion is as much a defect as a missing one. */
    val unused: List<String>
        get() = (reasonBySubject.keys - used).sorted()

    companion object {
        /** `key` is `Owner`, `Owner#member`, or `Owner.Companion#member`; all three are covered. */
        private fun String.isCoveredBy(subject: String): Boolean =
            this == subject || startsWith("$subject#") || startsWith("$subject.")

        fun parse(lines: List<String>): ManifestExclusions {
            val reasons = linkedMapOf<String, String>()
            lines.forEach { line ->
                // Subjects contain spaces, so split only the one-word reason.
                val separator = line.indexOf(' ')
                require(separator > 0) {
                    "Malformed manifest exclusion, expected `<reason> <subject>`: $line"
                }
                val reason = line.take(separator)
                val subject = line.substring(separator + 1).trim()
                require(subject.isNotEmpty()) {
                    "Manifest exclusion `$reason` names no subject: $line"
                }
                // Reject ambiguous duplicates instead of choosing one reason.
                val previous = reasons.put(subject, reason)
                require(previous == null || previous == reason) {
                    "Conflicting manifest exclusions for $subject: `$previous` and `$reason`"
                }
            }
            return ManifestExclusions(reasons)
        }
    }
}

/** What a manifest build produced, plus what it could not account for. */
internal class ApiManifest(
    val entries: List<ManifestEntry>,
    /** The input files the manifest was built from, by simple name. */
    val files: List<String>,
    val staleExclusions: List<String>,
) {
    /** Declarations that are neither emitted nor explained. Their presence aborts generation. */
    val unaccounted = entries.filter { it.status == ManifestStatus.UNACCOUNTED }

    fun ledgerFile(): LedgerFile = LedgerFile(
        buildMap {
            put("files", files.joinToString(","))
            ManifestKind.entries.forEach { kind ->
                put(kind.plural, this@ApiManifest.entries.count { it.kind == kind }.toString())
            }
            ManifestStatus.entries.forEach { status ->
                put(status.slug, this@ApiManifest.entries.count { it.status == status }.toString())
            }
        },
        // Group each classifier's declarations for stable, reviewable diffs.
        entries.sortedWith(compareBy(ManifestEntry::subject, { it.kind })).map { entry ->
            LedgerFile.Section(
                buildList {
                    add(entry.status.name)
                    add(entry.kind.slug)
                    add(entry.subject)
                    if (entry.status != ManifestStatus.EMITTED) add(entry.reason)
                    if (entry.detail.isNotEmpty()) add(entry.detail)
                },
            )
        },
    )
}

/**
 * Accounts for every declaration in [inputFiles] using coverage decisions or explicit [exclusions].
 * Declarations matched by neither are unaccounted and abort generation.
 */
internal fun buildApiManifest(
    files: List<KSFile>,
    coverage: CoverageLedger,
    exclusions: ManifestExclusions,
): ApiManifest {
    // Only expect-side files define the browser API being ported.
    val inputs = files.sortedBy(KSFile::fileName)

    val decisions = coverage.decisionsBySource()
    val entries = mutableListOf<ManifestEntry>()

    fun record(kind: ManifestKind, key: String, subject: String) {
        val decision = decisions[kind.coverageKind to key]
        entries += when {
            // Omit target details so emission-location changes do not churn the manifest.
            decision?.ported == true ->
                ManifestEntry(kind, key, subject, ManifestStatus.EMITTED, reason = "", detail = "")
            // Structured skip reasons need no checked-in exclusion.
            decision != null -> ManifestEntry(
                kind,
                key,
                subject,
                ManifestStatus.EXCLUDED,
                reason = checkNotNull(decision.reason).slug,
                detail = decision.detail,
            )
            else -> when (val reason = exclusions.reasonFor(key)) {
                null -> ManifestEntry(kind, key, subject, ManifestStatus.UNACCOUNTED, reason = "", detail = "")
                else -> ManifestEntry(kind, key, subject, ManifestStatus.EXCLUDED, reason, detail = "")
            }
        }
    }

    inputs.forEach { file ->
        file.declarations.forEach { declaration ->
            when (declaration) {
                is KSClassDeclaration -> recordClassifier(declaration, ::record)
                is KSPropertyDeclaration -> record(
                    ManifestKind.VALUE,
                    declaration.topLevelExtensionSubject(),
                    declaration.topLevelSubject(),
                )
                is KSFunctionDeclaration ->
                    // Dictionary factory decisions are keyed by dictionary, not by function signature.
                    if (declaration.extensionReceiver == null) {
                        val owner = declaration.qualifiedName?.asString().orEmpty()
                        record(ManifestKind.FACTORY, "$owner#factory", "$owner#${declaration.manifestSignature()}")
                    } else {
                        record(
                            ManifestKind.OPERATOR,
                            declaration.topLevelExtensionSubject(),
                            declaration.topLevelSubject(),
                        )
                    }
                else -> {
                    val name = declaration.qualifiedName?.asString() ?: declaration.simpleName.asString()
                    entries += ManifestEntry(
                        ManifestKind.NESTED_DECLARATION,
                        name,
                        name,
                        ManifestStatus.UNACCOUNTED,
                        reason = "",
                        detail = "unsupported top-level declaration kind",
                    )
                }
            }
        }
    }

    return ApiManifest(
        entries = entries,
        files = inputs.map(KSFile::fileName),
        staleExclusions = exclusions.unused,
    )
}

/** The classifier itself, everything it declares, and everything its companion declares. */
private fun recordClassifier(
    declaration: KSClassDeclaration,
    record: (ManifestKind, String, String) -> Unit,
) {
    val owner = declaration.qualifiedName?.asString().orEmpty()
    record(ManifestKind.CLASSIFIER, owner, owner)

    declaration.declarations.forEach { member ->
        when (member) {
            is KSPropertyDeclaration ->
                record(ManifestKind.MEMBER, "$owner#${member.coverageSignature()}", "$owner#${member.manifestSignature()}")
            is KSFunctionDeclaration -> record(
                if (member.isConstructor()) ManifestKind.CONSTRUCTOR else ManifestKind.MEMBER,
                "$owner#${member.coverageSignature()}",
                "$owner#${member.manifestSignature()}",
            )
            is KSClassDeclaration -> {
                val nested = member.qualifiedName?.asString() ?: "$owner.${member.simpleName.asString()}"
                val kinds = if (member.isCompanionObject) {
                    ManifestKind.COMPANION to ManifestKind.COMPANION_MEMBER
                } else {
                    ManifestKind.NESTED_DECLARATION to ManifestKind.NESTED_DECLARATION
                }
                record(kinds.first, nested, nested)
                member.declarations.forEach { inner ->
                    record(
                        kinds.second,
                        "$nested#${inner.coverageSignature()}",
                        "$nested#${inner.manifestSignature()}",
                    )
                }
            }
            else -> Unit
        }
    }
}

private fun KSDeclaration.topLevelSubject(): String =
    "${extensionReceiverClassifierName()}#${manifestSignature()}"

/** Renders a reviewable signature, keeping parameters with the declaration they modify. */
private fun KSDeclaration.manifestSignature(): String = when (this) {
    is KSPropertyDeclaration ->
        (if (isMutable) "var " else "val ") + simpleName.asString() + ": " + type.manifestType()
    is KSFunctionDeclaration -> {
        val name = if (isConstructor()) "constructor" else "fun ${simpleName.asString()}"
        val result = if (isConstructor()) "" else returnType?.let { ": ${it.manifestType()}" }.orEmpty()
        val declaresDefaults = Modifier.OVERRIDE !in modifiers
        parameters.joinToString(prefix = "$name(", postfix = ")$result") {
            it.manifestForm(declaresDefaults)
        }
    }
    is KSClassDeclaration -> qualifiedName?.asString() ?: simpleName.asString()
    else -> coverageSignature()
}

/** Records whether a default exists; the model ledger tracks reconstructed expressions. */
private fun KSValueParameter.manifestForm(includeDefault: Boolean): String {
    val prefix = if (isVararg) "vararg " else ""
    val suffix = if (includeDefault && hasDefault) " = ..." else ""
    return "$prefix${name?.asString() ?: "_"}: ${type.manifestType()}$suffix"
}

/**
 * Renders a resolved type recursively while preserving typealiases and variance.
 * This avoids KSP reference-rendering differences between declaration contexts.
 */
private fun KSTypeReference.manifestType(): String = resolve().manifestType()

private fun KSType.manifestType(): String {
    val name = declaration.qualifiedName?.asString()?.substringAfterLast('.')
        ?: declaration.simpleName.asString()
    val arguments = arguments.joinToString(", ") { argument ->
        val type = argument.type?.manifestType()
        when {
            argument.variance == Variance.STAR || type == null -> "*"
            argument.variance == Variance.INVARIANT -> type
            else -> "${argument.variance.label} $type"
        }
    }
    val parameterized = if (arguments.isEmpty()) name else "$name<$arguments>"
    return if (isMarkedNullable) "$parameterized?" else parameterized
}
