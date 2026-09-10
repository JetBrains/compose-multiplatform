/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Resolves the transitive set of browser classifiers required by the facade.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

/** The classifiers the facade emits, together with the reason each of them is in. */
internal class CommonClosure(
    /** Classifiers selected directly from the policy's exact input files. */
    val inputs: Set<String>,
    /** The classifiers a member/operator signature or a dictionary factory parameter asked for, sorted. */
    val dependencies: List<String>,
    /** Bare identities discovered through signature-only package mappings, sorted. */
    val identityOnly: Set<String>,
    /** Every emitted classifier, supertypes first. */
    val declarations: Map<String, KSClassDeclaration>,
) {
    /** Inputs, discovered dependencies, and identity-only declarations. */
    val selected = inputs + dependencies + identityOnly

    /** All resolved classifiers except bare identities. */
    val ported = declarations.keys - identityOnly
}

/** The retained analysis of the final closure; provisional rounds are discarded. */
internal data class CommonAnalysis(
    val closure: CommonClosure,
    val coverage: CoverageLedger,
    val model: List<CommonClass>,
    val extensions: List<CommonExtensionFunction>,
    val values: List<CommonExtensionValue>,
    val requestedDependencies: Set<String>,
)

/**
 * Expands [inputs] to a fixpoint over common signature dependencies, excluding policy omissions.
 * Recomputing the closure each round keeps output ordering deterministic.
 */
internal fun resolveCommonAnalysis(
    index: DeclarationIndex,
    inputs: Set<String>,
    signatureOnlyPackages: Set<String>,
    excluded: Set<String>,
): CommonAnalysis {
    val dependencies = sortedSetOf<String>()
    val identityOnly = sortedSetOf<String>()
    var declarations = selectDeclarations(index, inputs, identityOnly, excluded)
    while (true) {
        val closure = CommonClosure(inputs, dependencies.toList(), identityOnly, declarations)
        val analysis = analyzeCommonClosure(index, closure, signatureOnlyPackages)
        val discovered = analysis.requestedDependencies
            // Ignore classifiers whose source declarations cannot be inspected.
            .filter { it !in declarations && it !in excluded && index.isSourceDeclaration(it) }
        if (discovered.isEmpty()) return analysis
        identityOnly += discovered.filter { it.browserPackage() in signatureOnlyPackages }
        dependencies += discovered.filterNot { it.browserPackage() in signatureOnlyPackages }
        declarations = selectDeclarations(index, inputs + dependencies + identityOnly, identityOnly, excluded)
    }
}

/** Orders selected classifiers after every mapped supertype that can be emitted normally. */
private fun selectDeclarations(
    index: DeclarationIndex,
    selected: Set<String>,
    identityOnly: Set<String>,
    excluded: Set<String>,
): Map<String, KSClassDeclaration> {
    val closure = linkedMapOf<String, KSClassDeclaration>()

    fun visit(declaration: KSClassDeclaration) {
        val qualifiedName = declaration.qualifiedName?.asString()
        if (qualifiedName == null || qualifiedName in closure) return
        val hierarchy = index.hierarchy(declaration)

        // Identity-only declarations keep only edges already required by the closure.
        if (qualifiedName in identityOnly) {
            hierarchy.parent
                ?.takeIf { it.qualifiedName in selected }
                ?.declaration
                ?.let(::visit)
            hierarchy.interfaces
                .filter { it.qualifiedName in selected }
                .map(CommonSupertype::declaration)
                .forEach(::visit)
            closure[qualifiedName] = declaration
            return
        }

        val retainedParent = hierarchy.parent?.takeUnless { it.qualifiedName in excluded }
        val retainedInterfaces = hierarchy.interfaces.filterNot { it.qualifiedName in excluded }
        requireRetainedSupertypes(
            qualifiedName,
            hierarchy.classSupertypes.map(CommonSupertype::qualifiedName),
            buildSet {
                retainedParent?.qualifiedName?.let(::add)
                retainedInterfaces.mapNotNullTo(this, CommonSupertype::qualifiedName)
            },
        )

        retainedParent
            ?.declaration
            ?.let(::visit)
        retainedInterfaces
            .map(CommonSupertype::declaration)
            .forEach(::visit)

        closure[qualifiedName] = declaration
    }

    // Sorted, so the emitted order is decided by the selected set and not by discovery order.
    selected.sorted().forEach { qualifiedName -> index.declarationFor(qualifiedName)?.let(::visit) }
    return closure
}

/** Fails rather than silently copying members from a supertype the facade cannot retain. */
internal fun requireRetainedSupertypes(
    target: String,
    declared: List<String?>,
    retained: Set<String>,
) {
    check(declared.none { it == null }) { "Cannot emit $target: a direct supertype has no qualified name" }
    val omitted = declared.filterNotNull()
        .filterNot { it in retained || it in IGNORED_SUPERTYPES }
        .distinct()
        .sorted()
    check(omitted.isEmpty()) {
        "Cannot emit $target: direct ${if (omitted.size == 1) "supertype" else "supertypes"} " +
            "${omitted.joinToString()} ${if (omitted.size == 1) "is" else "are"} omitted from the facade closure. " +
            "Every direct supertype must be emitted normally; make it eligible in the facade selection " +
            "and package mappings."
    }
}

/** Builds one closure round while collecting every classifier its emitted signatures still require. */
private fun analyzeCommonClosure(
    index: DeclarationIndex,
    closure: CommonClosure,
    signatureOnlyPackages: Set<String>,
): CommonAnalysis {
    val coverage = CoverageLedger()
    val requestedDependencies = linkedSetOf<String>()
    val mapper = CommonTypeMapper(
        closure.declarations.keys.associateWith { ClassName.bestGuess(it).toCommonName() },
        signatureOnlyPackages,
    )
    val signatures = SignatureAnalyzer(mapper)
    val indexed = index.topLevelExtensionsFor(closure.ported)
    val extensions = indexed
        .filterNot { it is KSPropertyDeclaration }
        .mapNotNull { scanTopLevelExtension(it, signatures, coverage, requestedDependencies) }
    val values = indexed
        .filterIsInstance<KSPropertyDeclaration>()
        .mapNotNull { scanTopLevelExtensionValue(it, mapper, coverage) }
    val model = buildCommonModel(
        index,
        closure.declarations,
        closure.identityOnly,
        mapper,
        coverage,
        requestedDependencies,
        closure.selected,
    )
    return CommonAnalysis(
        closure,
        coverage,
        model,
        extensions,
        values,
        requestedDependencies,
    )
}

/** Ports an immutable `X.Companion` extension value whose type is `X`. */
private fun scanTopLevelExtensionValue(
    declaration: KSPropertyDeclaration,
    types: CommonTypeMapper,
    coverage: CoverageLedger,
): CommonExtensionValue? {
    val subject = declaration.topLevelExtensionSubject()
    if (declaration.isMutable || declaration.typeParameters.isNotEmpty()) {
        coverage.skipped(
            CoverageKind.TOP_LEVEL_EXTENSION,
            subject,
            SkipReason.UNSUPPORTED_DECLARATION_KIND,
            "only non-generic immutable extension values are emitted",
        )
        return null
    }
    val receiver = declaration.extensionReceiver
        ?: error("Indexed top-level extension has no receiver: $subject")
    val companion = (receiver.resolve().declaration as? KSClassDeclaration)
        ?.takeIf(KSClassDeclaration::isCompanionObject)
    val ownerName = (companion?.parentDeclaration as? KSClassDeclaration)?.qualifiedName?.asString()
    if (ownerName == null) {
        coverage.skipped(
            CoverageKind.TOP_LEVEL_EXTENSION,
            subject,
            SkipReason.TOP_LEVEL_EXTENSION,
            "only extension values on a classifier's companion are emitted",
        )
        return null
    }
    val mapping = types.result(declaration.type.resolve())
    if (mapping is TypeMapping.Skipped) {
        coverage.skipped(CoverageKind.TOP_LEVEL_EXTENSION, subject, mapping.reason, mapping.detail)
        return null
    }
    val browserOwner = ClassName.bestGuess(ownerName)
    if ((mapping as TypeMapping.Ported).type != browserOwner.toCommonName()) {
        coverage.skipped(
            CoverageKind.TOP_LEVEL_EXTENSION,
            subject,
            SkipReason.TOP_LEVEL_EXTENSION,
            "an extension value is emitted only where its type is the classifier it hangs off",
        )
        return null
    }
    val value = CommonExtensionValue(
        browserMember = MemberName(declaration.packageName.asString(), declaration.simpleName.asString()),
        browserOwner = browserOwner,
        name = declaration.simpleName.asString(),
        sourceFile = declaration.containingFile,
    )
    coverage.ported(CoverageKind.TOP_LEVEL_EXTENSION, subject, "emitted as ${value.key}")
    return value
}

/** Ports one operator extension when its full signature maps. */
private fun scanTopLevelExtension(
    declaration: KSDeclaration,
    signatures: SignatureAnalyzer,
    coverage: CoverageLedger,
    requestedDependencies: MutableSet<String>,
): CommonExtensionFunction? {
    val subject = declaration.topLevelExtensionSubject()
    if (declaration !is KSFunctionDeclaration || Modifier.OPERATOR !in declaration.modifiers) {
        coverage.skipped(
            CoverageKind.TOP_LEVEL_EXTENSION,
            subject,
            SkipReason.TOP_LEVEL_EXTENSION,
            "only operator extension functions are emitted",
        )
        return null
    }
    val analysis = signatures.extensionFunction(declaration)
    requestedDependencies += analysis.dependencies
    val decision = analysis.decision
    if (decision is SignatureDecision.Skipped) {
        coverage.skipped(CoverageKind.TOP_LEVEL_EXTENSION, subject, decision.reason, decision.detail)
        return null
    }
    val signature = decision.value()
    val extension = CommonExtensionFunction(
        browserMember = MemberName(declaration.packageName.asString(), declaration.simpleName.asString()),
        receiverType = signature.receiverType,
        function = signature.function,
        sourceFile = declaration.containingFile,
    )
    coverage.ported(CoverageKind.TOP_LEVEL_EXTENSION, subject, "emitted as ${extension.key}")
    return extension
}
