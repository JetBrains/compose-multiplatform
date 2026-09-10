/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Records why each browser declaration is emitted or skipped.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.isConstructor
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.squareup.kotlinpoet.TypeName

/** What kind of source surface a coverage entry describes. */
internal enum class CoverageKind {
    CLASSIFIER,
    MEMBER,
    PARAMETER,
    CONSTRUCTOR,
    COMPANION,
    COMPANION_MEMBER,
    TOP_LEVEL_EXTENSION,
    FACTORY,
    NESTED_DECLARATION,
}

/** Reasons for deliberate omissions; [UNEXPLAINED] aborts generation. */
internal enum class SkipReason {
    CALLBACK_TYPE,
    COMPANION,
    GENERIC_MEMBER,
    GENERIC_TYPE,
    IMPLICIT_CONSTRUCTOR,
    MISSING_NAME,
    MISSING_TYPE,
    NESTED_DECLARATION,
    NO_COMMON_PARAMETERS,
    IDENTITY_ONLY_DEPENDENCY,
    TOP_LEVEL_EXTENSION,
    TYPE_ALIAS,
    TYPE_PARAMETER,
    UNRESOLVED_TYPE,
    UNSELECTED_CLASSIFIER,
    UNSUPPORTED_DECLARATION_KIND,
    UNSUPPORTED_PACKAGE,
}

internal fun KSDeclaration.coverageSubject(ownerName: String): String = "$ownerName#${coverageSignature()}"

internal fun KSDeclaration.coverageSignature(): String = when (this) {
    is KSPropertyDeclaration -> (if (isMutable) "var " else "val ") + simpleName.asString()
    is KSFunctionDeclaration -> {
        val name = if (isConstructor()) "constructor" else "fun ${simpleName.asString()}"
        parameters.joinToString(prefix = "$name(", postfix = ")") { parameter ->
            val parameterName = parameter.name?.asString() ?: "_"
            "$parameterName: ${parameter.type}"
        }
    }
    is KSClassDeclaration -> "${classKind.name.lowercase()} ${simpleName.asString()}"
    else -> "${this::class.simpleName}:${simpleName.asString()}"
}

/** Renders a stable single-line signature type. */
internal fun TypeName.signature(): String =
    toString().lineSequence().joinToString("", transform = String::trim).replace(",)", ")")

private enum class CoverageStatus {
    PORTED,
    SKIPPED,
}

private data class CoverageEntry(
    val status: CoverageStatus,
    val kind: CoverageKind,
    val subject: String,
    val reason: SkipReason? = null,
    val detail: String = "",
)

private data class CoverageKey(
    val kind: CoverageKind,
    val subject: String,
)

/** What the ledger decided about one source declaration; see [CoverageLedger.decisionsBySource]. */
internal data class CoverageDecision(
    val ported: Boolean,
    val reason: SkipReason?,
    val detail: String,
)

/** A deterministic ledger of every declaration/member decision made while building the facade. */
internal class CoverageLedger {
    private val entries = linkedMapOf<CoverageKey, CoverageEntry>()

    fun ported(kind: CoverageKind, subject: String, detail: String = "") {
        record(CoverageEntry(CoverageStatus.PORTED, kind, subject, detail = detail))
    }

    fun skipped(kind: CoverageKind, subject: String, reason: SkipReason, detail: String = "") {
        record(CoverageEntry(CoverageStatus.SKIPPED, kind, subject, reason, detail))
    }

    val portedCount: Int
        get() = entries.values.count { it.status == CoverageStatus.PORTED }

    val skippedCount: Int
        get() = entries.values.count { it.status == CoverageStatus.SKIPPED }

    /**
     * Re-keys target-prefixed decisions onto their source declarations for [ApiManifest].
     * When a declaration has several decisions, any ported result takes precedence.
     */
    fun decisionsBySource(): Map<Pair<CoverageKind, String>, CoverageDecision> {
        val decisions = mutableMapOf<Pair<CoverageKind, String>, CoverageDecision>()
        entries.values.forEach { entry ->
            val key = entry.kind to entry.subject.substringAfter(" <= ")
            val decision = CoverageDecision(
                ported = entry.status == CoverageStatus.PORTED,
                reason = entry.reason,
                detail = entry.detail,
            )
            val previous = decisions[key]
            if (previous == null || (decision.ported && !previous.ported)) decisions[key] = decision
        }
        return decisions
    }

    private fun record(entry: CoverageEntry) {
        val key = CoverageKey(entry.kind, entry.subject)
        val previous = entries.putIfAbsent(key, entry)
        check(previous == null || previous == entry) {
            "Conflicting coverage decisions for ${entry.kind.name.lowercase()} ${entry.subject}: " +
                "$previous vs $entry"
        }
    }

    fun ledgerFile(): LedgerFile = LedgerFile(
        linkedMapOf(
            "ported" to portedCount.toString(),
            "skipped" to skippedCount.toString(),
        ),
        entries.values.sortedWith(
            compareBy<CoverageEntry>(CoverageEntry::status)
                .thenBy(CoverageEntry::kind)
                .thenBy(CoverageEntry::subject)
                .thenBy { it.reason }
                .thenBy(CoverageEntry::detail),
        ).map { entry ->
            LedgerFile.Section(
                buildList {
                    add(entry.status.name)
                    add(entry.kind.slug)
                    add(entry.subject)
                    if (entry.status == CoverageStatus.SKIPPED) add(checkNotNull(entry.reason).slug)
                    if (entry.detail.isNotEmpty()) add(entry.detail)
                },
            )
        },
    )
}

/** Records members omitted because only [declaration]'s classifier identity is emitted. */
internal fun CoverageLedger.auditIdentityOnly(declaration: KSClassDeclaration) {
    val owner = declaration.qualifiedName?.asString().orEmpty()
    val detail = "only the classifier identity of an identity-only dependency is emitted"
    (declaration.getDeclaredProperties() + declaration.getDeclaredFunctions()).forEach { member ->
        skipped(
            if (member is KSFunctionDeclaration && member.isConstructor()) {
                CoverageKind.CONSTRUCTOR
            } else {
                CoverageKind.MEMBER
            },
            member.coverageSubject(owner),
            SkipReason.IDENTITY_ONLY_DEPENDENCY,
            detail,
        )
    }
    auditNested(declaration, owner, includeCompanions = true) { _, _ ->
        SkipReason.IDENTITY_ONLY_DEPENDENCY to detail
    }
}

internal fun CoverageLedger.auditNestedDeclarations(declaration: KSClassDeclaration, targetName: String) =
    auditNested(declaration, targetName, includeCompanions = false) { companion, memberOfIt ->
        when {
            companion && memberOfIt -> SkipReason.COMPANION to "member of an omitted companion"
            companion -> SkipReason.COMPANION to "companion objects are not emitted"
            memberOfIt -> SkipReason.NESTED_DECLARATION to "member of an omitted nested declaration"
            else -> SkipReason.NESTED_DECLARATION to "nested declarations are not emitted"
        }
    }

/** Records [skip] for every nested classifier and member of [declaration]. */
private inline fun CoverageLedger.auditNested(
    declaration: KSClassDeclaration,
    targetName: String,
    includeCompanions: Boolean,
    skip: (companion: Boolean, memberOfIt: Boolean) -> Pair<SkipReason, String>,
) {
    declaration.declarations.filterIsInstance<KSClassDeclaration>().forEach { nested ->
        val owner = declaration.qualifiedName?.asString().orEmpty()
        val nestedName = nested.qualifiedName?.asString() ?: "$owner.${nested.simpleName.asString()}"
        val companion = nested.isCompanionObject
        if (companion && !includeCompanions) return@forEach
        val (reason, detail) = skip(companion, false)
        skipped(
            if (companion) CoverageKind.COMPANION else CoverageKind.NESTED_DECLARATION,
            "$targetName <= $nestedName",
            reason,
            detail,
        )
        nested.declarations.forEach { member ->
            val (memberReason, memberDetail) = skip(companion, true)
            skipped(
                if (companion) CoverageKind.COMPANION_MEMBER else CoverageKind.NESTED_DECLARATION,
                "$targetName <= $nestedName#${member.coverageSignature()}",
                memberReason,
                memberDetail,
            )
        }
    }
}
