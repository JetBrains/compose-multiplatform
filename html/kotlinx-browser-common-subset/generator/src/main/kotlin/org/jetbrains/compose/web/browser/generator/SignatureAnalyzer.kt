/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Converts browser declarations to portable signatures and discovers missing dependencies.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.TypeName

/** Canonical signature form used for matching declarations across targets. */
internal sealed interface PortableDeclarationSignature {
    fun render(): String
}

/** A function key consisting of its name and parameter signatures. */
internal data class FunctionSignature(
    val name: String,
    val parameters: List<ParameterSignature>,
) : PortableDeclarationSignature {
    override fun render(): String = parameters.joinToString(
        prefix = "fun $name(",
        postfix = ")",
        transform = ParameterSignature::render,
    )
}

/** A constructor key consisting of its parameter signatures. */
internal data class ConstructorSignature(
    val parameters: List<ParameterSignature>,
) : PortableDeclarationSignature {
    override fun render(): String = parameters.joinToString(
        prefix = "constructor(",
        postfix = ")",
        transform = ParameterSignature::render,
    )
}

/** A property key consisting of its name. */
internal data class PropertySignature(val name: String) : PortableDeclarationSignature {
    override fun render(): String = "val $name"
}

/** A parameter type and its vararg marker. */
internal data class ParameterSignature(
    val type: TypeName,
    val isVararg: Boolean,
) {
    fun render(): String = (if (isVararg) "vararg " else "") + type.signature()
}

internal fun PortableFunction.signatureKey(): FunctionSignature =
    FunctionSignature(name, parameters.map(PortableParameter::signatureKey))

internal fun PortableConstructor.signatureKey(): ConstructorSignature =
    ConstructorSignature(parameters.map(PortableParameter::signatureKey))

internal fun PortableProperty.signatureKey(): PropertySignature = PropertySignature(name)

private fun PortableParameter.signatureKey(): ParameterSignature = ParameterSignature(type, isVararg)

/** Key used to deduplicate inherited functions. */
internal fun PortableFunction.key(): String = signatureKey().render()

/** Key used to deduplicate inherited properties. */
internal fun PortableProperty.key(): String = signatureKey().render()

/** A signature decision and any classifiers it can add to the closure. */
internal class SignatureAnalysis<out T>(
    val decision: SignatureDecision<T>,
    val dependencies: Set<String>,
    /** Whether a non-discoverable signature problem makes [dependencies] incomplete. */
    internal val discoveryBlocked: Boolean,
)

/** The portable result of analyzing a signature, or its structured skip reason. */
internal sealed interface SignatureDecision<out T> {
    fun value(): T

    data class Ported<T>(private val result: T) : SignatureDecision<T> {
        override fun value(): T = result
    }

    data class Skipped(val reason: SkipReason, val detail: String) : SignatureDecision<Nothing> {
        override fun value(): Nothing = error("Skipped signature has no value")
    }
}

/** A portable top-level extension receiver and function signature. */
internal data class PortableExtensionSignature(
    val receiverType: TypeName,
    val function: PortableFunction,
)

/** Maps browser properties, functions, constructors, and extensions to portable signatures. */
internal class SignatureAnalyzer(private val types: PortableTypeMapper) {
    /** Analyzes a property as a member of [ownType]. */
    fun property(declaration: KSPropertyDeclaration, ownType: KSType?): SignatureAnalysis<PortableProperty> {
        val mapping = firstPorted(
            types.result(ownType?.let { runCatching { declaration.asMemberOf(it) }.getOrNull() }),
            types.result(declaration.type.resolve()),
        )
        val blocked = declaration.typeParameters.isNotEmpty() || declaration.extensionReceiver != null
        val decision = when {
            declaration.typeParameters.isNotEmpty() ->
                SignatureDecision.Skipped(SkipReason.GENERIC_MEMBER, declaration.simpleName.asString())
            declaration.extensionReceiver != null ->
                SignatureDecision.Skipped(SkipReason.TOP_LEVEL_EXTENSION, declaration.simpleName.asString())
            mapping is TypeMapping.Skipped -> SignatureDecision.Skipped(mapping.reason, mapping.detail)
            else -> SignatureDecision.Ported(
                PortableProperty(
                    name = declaration.simpleName.asString(),
                    type = (mapping as TypeMapping.Ported).type,
                    mutable = declaration.isMutable,
                    open = declaration.isOpenMember(),
                    abstractInBrowser = declaration.isAbstractInBrowser(),
                ),
            )
        }
        return analysis(decision, listOf(mapping), blocked)
    }

    /** Analyzes a function and records dependencies from its complete signature. */
    fun function(
        declaration: KSFunctionDeclaration,
        ownType: KSType?,
        keepDefaults: Boolean,
        allowExtension: Boolean = false,
    ): SignatureAnalysis<PortableFunction> {
        val resolved = ownType?.let { runCatching { declaration.asMemberOf(it) }.getOrNull() }
        val returnMapping = firstPorted(
            types.result(resolved?.returnType),
            types.result(declaration.returnType?.resolve()),
        )
        val parameterMappings = declaration.parameters.mapIndexed { index, parameter ->
            firstPorted(
                types.result(resolved?.parameterTypes?.getOrNull(index)),
                types.result(parameter.type.resolve()),
            )
        }
        val blocked = declaration.typeParameters.isNotEmpty() ||
            (!allowExtension && declaration.extensionReceiver != null) ||
            declaration.parameters.any { it.name == null }
        val decision = functionDecision(declaration, returnMapping, parameterMappings, keepDefaults, allowExtension)
        return analysis(decision, listOf(returnMapping) + parameterMappings, blocked)
    }

    /** Analyzes a top-level extension including its receiver type. */
    fun extensionFunction(declaration: KSFunctionDeclaration): SignatureAnalysis<PortableExtensionSignature> {
        val receiverMapping = types.result(declaration.extensionReceiver?.resolve())
        // Facade extensions are real wrapper actuals, so they cannot inherit a web-only
        // `definedExternally` default through a typealias. Keep their parameters explicit.
        val function = function(declaration, ownType = null, keepDefaults = false, allowExtension = true)
        val blocked = function.discoveryBlocked ||
            (receiverMapping is TypeMapping.Skipped && receiverMapping.reason != SkipReason.UNSELECTED_CLASSIFIER)
        val dependencies = if (blocked) {
            emptySet()
        } else {
            buildSet {
                if (receiverMapping is TypeMapping.Skipped) add(receiverMapping.detail)
                addAll(function.dependencies)
            }
        }
        val decision = when {
            receiverMapping is TypeMapping.Skipped -> SignatureDecision.Skipped(
                receiverMapping.reason,
                "receiver type: ${receiverMapping.detail}",
            )
            function.decision is SignatureDecision.Skipped -> function.decision
            else -> SignatureDecision.Ported(
                PortableExtensionSignature(
                    (receiverMapping as TypeMapping.Ported).type,
                    function.decision.value(),
                ),
            )
        }
        return SignatureAnalysis(decision, dependencies, blocked)
    }

    /** Analyzes a constructor and preserves whether it is primary. */
    fun constructor(
        declaration: KSFunctionDeclaration,
        primary: KSFunctionDeclaration?,
    ): SignatureAnalysis<PortableConstructor> {
        val mappings = declaration.parameters.map { types.result(it.type.resolve()) }
        val blocked = declaration.parameters.any { it.name == null }
        val decision = when (val parameters = mapParameters(declaration, mappings, keepDefaults = true)) {
            is SignatureDecision.Skipped -> parameters
            else -> SignatureDecision.Ported(
                PortableConstructor(parameters.value(), primary != null && declaration.isSameAs(primary)),
            )
        }
        return analysis(decision, mappings, blocked)
    }

    private fun functionDecision(
        declaration: KSFunctionDeclaration,
        returnMapping: TypeMapping,
        parameterMappings: List<TypeMapping>,
        keepDefaults: Boolean,
        allowExtension: Boolean,
    ): SignatureDecision<PortableFunction> {
        if (declaration.typeParameters.isNotEmpty()) {
            return SignatureDecision.Skipped(SkipReason.GENERIC_MEMBER, declaration.simpleName.asString())
        }
        if (!allowExtension && declaration.extensionReceiver != null) {
            return SignatureDecision.Skipped(SkipReason.TOP_LEVEL_EXTENSION, declaration.simpleName.asString())
        }
        if (returnMapping is TypeMapping.Skipped) {
            return SignatureDecision.Skipped(returnMapping.reason, "return type: ${returnMapping.detail}")
        }

        return when (val parameters = mapParameters(declaration, parameterMappings, keepDefaults)) {
            is SignatureDecision.Skipped -> parameters
            else -> SignatureDecision.Ported(
                PortableFunction(
                    declaration.simpleName.asString(),
                    parameters.value(),
                    (returnMapping as TypeMapping.Ported).type,
                    declaration.isOpenMember(),
                    declaration.isAbstractInBrowser(),
                ),
            )
        }
    }

    private fun mapParameters(
        declaration: KSFunctionDeclaration,
        mappings: List<TypeMapping>,
        keepDefaults: Boolean,
    ): SignatureDecision<List<PortableParameter>> {
        val parameters = mutableListOf<PortableParameter>()
        val keepDeclaredDefaults = keepDefaults && Modifier.OVERRIDE !in declaration.modifiers
        declaration.parameters.forEachIndexed { index, parameter ->
            val mapping = mappings[index]
            if (mapping is TypeMapping.Skipped) {
                return SignatureDecision.Skipped(
                    mapping.reason,
                    "parameter ${parameter.name?.asString() ?: "#$index"}: ${mapping.detail}",
                )
            }
            val name = parameter.name?.asString()
                ?: return SignatureDecision.Skipped(SkipReason.MISSING_NAME, "parameter #$index")
            parameters += PortableParameter(
                name,
                (mapping as TypeMapping.Ported).type,
                parameter.isVararg,
                keepDeclaredDefaults && parameter.hasDefault,
            )
        }
        return SignatureDecision.Ported(parameters)
    }

    private fun <T> analysis(
        decision: SignatureDecision<T>,
        mappings: List<TypeMapping>,
        blocked: Boolean,
    ): SignatureAnalysis<T> {
        val discoveryBlocked = blocked || mappings.any {
            it is TypeMapping.Skipped && it.reason != SkipReason.UNSELECTED_CLASSIFIER
        }
        val dependencies = if (discoveryBlocked) {
            emptySet()
        } else {
            mappings.mapNotNullTo(linkedSetOf()) { mapping ->
                (mapping as? TypeMapping.Skipped)?.detail
            }
        }
        return SignatureAnalysis(decision, dependencies, discoveryBlocked)
    }
}

private fun firstPorted(primary: TypeMapping, fallback: TypeMapping): TypeMapping =
    if (primary is TypeMapping.Ported) primary else fallback

private fun KSFunctionDeclaration.isSameAs(other: KSFunctionDeclaration): Boolean =
    this == other || coverageSignature() == other.coverageSignature()

private fun KSDeclaration.isOpenMember(): Boolean =
    Modifier.OPEN in modifiers ||
        Modifier.OVERRIDE in modifiers ||
        Modifier.ABSTRACT in modifiers ||
        (parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE

private fun KSDeclaration.isAbstractInBrowser(): Boolean =
    (parentDeclaration as? KSClassDeclaration)?.classKind == ClassKind.INTERFACE && Modifier.OPEN !in modifiers
