/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Builds the portable class model from the resolved closure and renders its ledger.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeVariableName

internal data class InheritedMemberKeys(
    val visible: Set<String>,
    val provided: Set<String>,
    val deduplication: Set<String>,
    val overrides: Set<String>,
)

/** Keeps inherited interface contracts distinct from members implemented by a parent class. */
internal fun inheritedMemberKeys(
    shape: ClassShape,
    parentVisible: Set<String>,
    parentProvided: Set<String>,
    interfaceVisible: Set<String>,
): InheritedMemberKeys {
    val visible = parentVisible + interfaceVisible
    val provided = parentProvided
    return if (shape == ClassShape.INTERFACE) {
        InheritedMemberKeys(visible, provided, deduplication = visible, overrides = emptySet())
    } else {
        InheritedMemberKeys(visible, provided, deduplication = provided, overrides = visible - provided)
    }
}

/** Converts the resolved browser closure into portable source declarations. */
internal fun buildPortableModel(
    index: DeclarationIndex,
    closure: Map<String, KSClassDeclaration>,
    identityOnly: Set<String>,
    mapper: PortableTypeMapper,
    coverage: CoverageLedger,
    requestedDependencies: MutableSet<String>,
    identitySupertypeSelection: Set<String>,
): List<PortableClass> {
    val portableNames = mapper.portableNames
    val memberScanner = MemberScanner(mapper, coverage, requestedDependencies)
    val companionScanner = CompanionScanner(mapper, coverage, requestedDependencies)
    // Interface members are visible to subtypes but do not provide an implementation. Keep those
    // facts separate so the first portable class in the hierarchy repeats the browser's fake
    // overrides instead of incorrectly deduplicating them as concrete inherited members.
    val visibleKeys = mutableMapOf<String, Set<String>>()
    val providedKeys = mutableMapOf<String, Set<String>>()
    val ancestors = mutableMapOf<String, List<ClassName>>()

    return closure.map { (qualifiedName, declaration) ->
        val isIdentityOnly = qualifiedName in identityOnly
        coverage.ported(
            CoverageKind.CLASSIFIER,
            qualifiedName,
            if (isIdentityOnly) "emitted as an identity-only dependency" else "",
        )
        val hierarchy = index.hierarchy(declaration)
        if (!isIdentityOnly && hierarchy.declaresJsAny) {
            coverage.ported(
                CoverageKind.CLASSIFIER,
                "$qualifiedName <= ${BROWSER_JS_ANY.canonicalName}",
                "provided by the portable interop marker",
            )
        }
        val parentName = hierarchy.parent
            ?.qualifiedName
            ?.takeIf { !isIdentityOnly || it in closure }
        val retainedInterfaces = hierarchy.interfaces.filter {
            it.qualifiedName in closure && (!isIdentityOnly || it.qualifiedName in identitySupertypeSelection)
        }
        val interfaceNames = retainedInterfaces.map { checkNotNull(it.qualifiedName) }
        val interfaceTypes = retainedInterfaces.map { supertype ->
            when (val mapping = mapper.result(supertype.type)) {
                is TypeMapping.Ported -> mapping.type
                is TypeMapping.Skipped -> error(
                    "Selected superinterface ${supertype.qualifiedName} of $qualifiedName does not map: " +
                        "${mapping.reason.slug}: ${mapping.detail}",
                )
            }
        }
        val shape = declaration.shape()
        val inherited = inheritedMemberKeys(
            shape,
            parentName?.let(visibleKeys::getValue).orEmpty(),
            parentName?.let(providedKeys::getValue).orEmpty(),
            interfaceNames.flatMapTo(linkedSetOf(), visibleKeys::getValue),
        )
        val isDictionary = !isIdentityOnly &&
            shape == ClassShape.INTERFACE &&
            index.topLevelFunctionFor(qualifiedName) != null
        val members = if (isIdentityOnly) {
            coverage.auditIdentityOnly(declaration)
            ScannedMembers.EMPTY
        } else {
            memberScanner.scan(declaration, inherited.deduplication, inherited.overrides)
        }
        visibleKeys[qualifiedName] = inherited.visible + members.keys
        providedKeys[qualifiedName] = if (shape == ClassShape.INTERFACE) {
            emptySet()
        } else {
            inherited.provided + members.keys
        }
        ancestors[qualifiedName] = parentName
            ?.let { listOf(portableNames.getValue(it)) + ancestors.getValue(it) }
            .orEmpty()

        PortableClass(
            browserName = ClassName.bestGuess(qualifiedName),
            parentBrowserName = parentName?.let(ClassName::bestGuess),
            superinterfaces = interfaceNames.map(portableNames::getValue),
            ancestors = ancestors.getValue(qualifiedName),
            shape = shape,
            isDictionary = isDictionary,
            isJsAny = hierarchy.declaresJsAny,
            properties = members.properties,
            functions = members.functions,
            constructors = members.constructors,
            companion = if (isIdentityOnly) null else companionScanner.scan(declaration),
            factory = if (isDictionary) {
                index.factoryFor(
                    qualifiedName,
                    mapper,
                    coverage,
                    requestedDependencies,
                )
            } else {
                null
            },
            sourceFile = declaration.containingFile,
            typeVariables = declaration.portableTypeVariables(mapper),
            superinterfaceTypes = interfaceTypes,
        )
    }
}

/** Preserves classifier variance and bounds. Generic members remain a separate policy decision. */
private fun KSClassDeclaration.portableTypeVariables(mapper: PortableTypeMapper): List<TypeVariableName> =
    typeParameters.map { parameter ->
        // On the JS KSP round, JsAny? in ItemArrayLike's source bound resolves to kotlin.Any?.
        // Restore the cross-target interop bound that Wasm/JS and the browser typealias require.
        val bounds = if (qualifiedName?.asString() == "$DOM_PACKAGE.ItemArrayLike") {
            listOf(PORTABLE_JS_ANY.copy(nullable = true))
        } else {
            parameter.bounds.map { reference ->
                when (val mapping = mapper.result(reference.resolve())) {
                    is TypeMapping.Ported -> mapping.type
                    is TypeMapping.Skipped -> error(
                        "Type parameter ${parameter.name.asString()} on ${qualifiedName?.asString()} has an " +
                            "unsupported bound: ${mapping.reason.slug}: ${mapping.detail}",
                    )
                }
            }.toList()
        }
        TypeVariableName(
            parameter.name.asString(),
            *bounds.toTypedArray(),
            variance = when (parameter.variance) {
                Variance.COVARIANT -> KModifier.OUT
                Variance.CONTRAVARIANT -> KModifier.IN
                else -> null
            },
        )
    }

/** Builds the portable factory paired with an option-dictionary interface. */
private fun DeclarationIndex.factoryFor(
    qualifiedName: String,
    mapper: PortableTypeMapper,
    coverage: CoverageLedger,
    requestedDependencies: MutableSet<String>,
): PortableFactory? {
    val factory = topLevelFunctionFor(qualifiedName) ?: return null
    val parameters = mutableListOf<PortableParameter>()
    factory.parameters.forEachIndexed { index, parameter ->
        val name = parameter.name?.asString()
        val subject = "$qualifiedName#factory parameter ${name ?: "#$index"}"
        if (name == null) {
            coverage.skipped(CoverageKind.PARAMETER, subject, SkipReason.MISSING_NAME)
            return@forEachIndexed
        }
        val mapping = mapper.result(parameter.type.resolve())
        if (mapping is TypeMapping.Skipped && mapping.reason == SkipReason.UNSELECTED_CLASSIFIER) {
            requestedDependencies += mapping.detail
        }
        when (mapping) {
            is TypeMapping.Ported -> {
                coverage.ported(CoverageKind.PARAMETER, subject)
                parameters += PortableParameter(name, mapping.type, parameter.isVararg, hasDefault = true)
            }
            is TypeMapping.Skipped -> coverage.skipped(
                CoverageKind.PARAMETER,
                subject,
                mapping.reason,
                mapping.detail,
            )
        }
    }
    if (parameters.isEmpty()) {
        coverage.skipped(
            CoverageKind.FACTORY,
            "$qualifiedName#factory",
            SkipReason.NO_PORTABLE_PARAMETERS,
            "none of ${factory.parameters.size} parameters can be emitted",
        )
        return null
    }
    coverage.ported(
        CoverageKind.FACTORY,
        "$qualifiedName#factory",
        "${parameters.size}/${factory.parameters.size} parameters emitted",
    )
    return PortableFactory(parameters)
}

/** Renders the reviewable model report backing `model.txt`. */
internal fun modelLedger(
    closure: PortableClosure,
    model: List<PortableClass>,
    extensions: List<PortableExtensionFunction>,
    values: List<PortableExtensionValue>,
): LedgerFile = LedgerFile(
    linkedMapOf(
        "inputs" to closure.inputs.size.toString(),
        "dependencies" to closure.dependencies.size.toString(),
        "identityOnly" to closure.identityOnly.size.toString(),
        "closure" to model.size.toString(),
        "members" to model.sumOf(PortableClass::memberCount).toString(),
        "constructors" to model.sumOf { it.constructors.size }.toString(),
        "extensions" to extensions.size.toString(),
        "values" to values.size.toString(),
    ),
    model.map { declaration ->
        val browserName = declaration.browserName.canonicalName
        LedgerFile.Section(
            fields = listOf(
                browserName,
                declaration.parentBrowserName?.canonicalName.orEmpty(),
                declaration.superinterfaces.joinToString(",", transform = ClassName::simpleName),
                if (declaration.isDictionary) "dictionary" else declaration.shape.slug,
                declaration.memberCount.toString(),
                when {
                    browserName in closure.identityOnly -> "identity-only"
                    browserName in closure.inputs -> "input"
                    browserName in closure.dependencies -> "dependency"
                    else -> "supertype"
                },
            ),
            lines = buildList {
                declaration.constructors.forEach {
                    add((if (it.primary) "primary " else "secondary ") + it.key())
                }
                declaration.properties.forEach {
                    add((if (it.mutable) "var " else "val ") + "${it.name}: ${it.type.signature()}")
                }
                declaration.functions.forEach { add("${it.key()}: ${it.returnType.signature()}") }
                declaration.companion?.properties?.forEach {
                    add("companion val ${it.name}: ${it.type.signature()}")
                }
                declaration.companion?.functions?.forEach {
                    add("companion ${it.key()}: ${it.returnType.signature()}")
                }
                values.filter { it.portableOwner == declaration.portableName }.forEach { add("value ${it.key}") }
            },
        )
    },
)

/** Ports immutable companion constants and non-generic functions with portable signatures. */
private class CompanionScanner(
    private val types: PortableTypeMapper,
    private val coverage: CoverageLedger,
    private val requestedDependencies: MutableSet<String>,
) {
    private val signatures = SignatureAnalyzer(types)

    fun scan(declaration: KSClassDeclaration): PortableCompanion? {
        val owner = declaration.qualifiedName?.asString().orEmpty()
        val companion = declaration.declarations
            .filterIsInstance<KSClassDeclaration>()
            .firstOrNull(KSClassDeclaration::isCompanionObject)
            ?: return null
        val companionName = companion.qualifiedName?.asString() ?: "$owner.Companion"
        val properties = mutableListOf<PortableConstant>()
        val functions = mutableListOf<PortableFunction>()
        val functionKeys = mutableSetOf<String>()

        companion.declarations.forEach { member ->
            val subject = "$owner <= $companionName#${member.coverageSignature()}"
            when (member) {
                is KSPropertyDeclaration -> {
                    if (member.isMutable || member.typeParameters.isNotEmpty() || member.extensionReceiver != null) {
                        coverage.skipped(
                            CoverageKind.COMPANION_MEMBER,
                            subject,
                            if (member.typeParameters.isNotEmpty()) SkipReason.GENERIC_MEMBER
                            else SkipReason.UNSUPPORTED_DECLARATION_KIND,
                            "only non-generic immutable companion properties are emitted",
                        )
                        return@forEach
                    }
                    when (val mapping = types.result(member.type.resolve())) {
                        is TypeMapping.Skipped -> coverage.skipped(
                            CoverageKind.COMPANION_MEMBER,
                            subject,
                            mapping.reason,
                            mapping.detail,
                        )
                        is TypeMapping.Ported -> {
                            val name = member.simpleName.asString()
                            properties += PortableConstant(name = name, type = mapping.type)
                            coverage.ported(
                                CoverageKind.COMPANION_MEMBER,
                                subject,
                                "emitted on $owner.Companion",
                            )
                        }
                    }
                }
                is KSFunctionDeclaration -> {
                    val analysis = signatures.function(member, ownType = null, keepDefaults = true)
                    requestedDependencies += analysis.dependencies
                    val decision = analysis.decision
                    if (decision is SignatureDecision.Skipped) {
                        coverage.skipped(
                            CoverageKind.COMPANION_MEMBER,
                            subject,
                            decision.reason,
                            decision.detail,
                        )
                    } else {
                        val function = decision.value()
                        if (functionKeys.add(function.key())) {
                            functions += function
                            coverage.ported(
                                CoverageKind.COMPANION_MEMBER,
                                subject,
                                "emitted on $owner.Companion",
                            )
                        } else {
                            coverage.ported(
                                CoverageKind.COMPANION_MEMBER,
                                subject,
                                "deduplicated on $owner.Companion",
                            )
                        }
                    }
                }
                else -> coverage.skipped(
                    CoverageKind.COMPANION_MEMBER,
                    subject,
                    SkipReason.UNSUPPORTED_DECLARATION_KIND,
                    "only companion properties and functions are emitted",
                )
            }
        }

        coverage.ported(
            CoverageKind.COMPANION,
            "$owner <= $companionName",
            "emitted with ${properties.size} constants and ${functions.size} functions",
        )
        return PortableCompanion(properties, functions)
    }
}
