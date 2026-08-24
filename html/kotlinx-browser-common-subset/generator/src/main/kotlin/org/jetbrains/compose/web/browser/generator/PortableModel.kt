/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Defines the intermediate model shared by KSP analysis and source emission.
package org.jetbrains.compose.web.browser.generator

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName

/** Intermediate model shared by the KSP reader and KotlinPoet emitters. */
internal data class PortableClass(
    val browserName: ClassName,
    val parentBrowserName: ClassName?,
    val superinterfaces: List<ClassName>,
    val ancestors: List<ClassName>,
    val shape: ClassShape,
    val isDictionary: Boolean,
    /** Whether the browser declaration lists the interop marker. */
    val isJsAny: Boolean,
    val properties: List<PortableProperty>,
    val functions: List<PortableFunction>,
    val constructors: List<PortableConstructor>,
    val companion: PortableCompanion?,
    val factory: PortableFactory?,
    val sourceFile: KSFile?,
    /** Classifier parameters preserved by common, web typealias, and JVM declarations. */
    val typeVariables: List<TypeVariableName> = emptyList(),
    /** Parameterized forms of [superinterfaces], in the same order. */
    val superinterfaceTypes: List<TypeName> = emptyList(),
) {
    val portableName = browserName.toPortableName()
    val portableParentName = parentBrowserName?.toPortableName()
    val portableSuperinterfaces: List<TypeName>
        get() = superinterfaceTypes.ifEmpty { superinterfaces }
    val instanceMemberCount = properties.size + functions.size
    val memberCount = instanceMemberCount + (companion?.memberCount ?: 0)

    /** Whether any emitted function or constructor parameter has a default. */
    val hasDefaultArguments = functions.any(PortableFunction::hasDefaultArgument) ||
        companion?.functions.orEmpty().any(PortableFunction::hasDefaultArgument) ||
        constructors.any(PortableConstructor::hasDefaultArgument)

    /** Uses the browser primary constructor or the first constructor for JVM delegation. */
    val jvmPrimaryConstructor = constructors.firstOrNull(PortableConstructor::primary) ?: constructors.firstOrNull()

    /** Whether the JVM stub can be instantiated as `Type()`. */
    val jvmConstructibleWithoutArguments =
        constructors.isEmpty() || constructors.any(PortableConstructor::callableWithoutArguments)

    /** Restores a protected no-argument path required by JVM subclasses. */
    val needsJvmFallbackConstructor = !jvmConstructibleWithoutArguments && shape != ClassShape.FINAL

    private val hasDictionaryProperties = isDictionary && properties.isNotEmpty()
    private val hasOpaquePromise = properties.any { it.type.mentionsPromise() } ||
        functions.any(PortableFunction::mentionsPromise) ||
        companion?.properties.orEmpty().any { it.type.mentionsPromise() } ||
        companion?.functions.orEmpty().any(PortableFunction::mentionsPromise) ||
        constructors.any(PortableConstructor::mentionsPromise)
    private val hasAbstractClassProperties =
        shape != ClassShape.INTERFACE && properties.any(PortableProperty::abstractInBrowser)
    private val hasIrIncompatibleClassFunctions = shape != ClassShape.INTERFACE && functions.any {
        it.abstractInBrowser || (it.open && it.parameters.any(PortableParameter::hasDefault))
    }

    /** Flags unavoidable expect/actual modality, default, dictionary, or Promise mismatches. */
    val needsIrSuppression = hasDictionaryProperties || hasOpaquePromise ||
        hasAbstractClassProperties || hasIrIncompatibleClassFunctions
}

private fun TypeName.mentionsPromise(): Boolean = mentions { it == PORTABLE_PROMISE }

private fun PortableFunction.hasDefaultArgument(): Boolean = parameters.any(PortableParameter::hasDefault)

private fun PortableConstructor.hasDefaultArgument(): Boolean = parameters.any(PortableParameter::hasDefault)

private fun PortableFunction.mentionsPromise(): Boolean =
    returnType.mentionsPromise() || parameters.any { it.type.mentionsPromise() }

private fun PortableConstructor.mentionsPromise(): Boolean = parameters.any { it.type.mentionsPromise() }

/** A portable instance property and the modality needed by generated targets. */
internal data class PortableProperty(
    val name: String,
    val type: TypeName,
    val mutable: Boolean,
    val open: Boolean,
    val abstractInBrowser: Boolean,
    val overrides: Boolean = false,
)

/** A portable instance function and its complete signature. */
internal data class PortableFunction(
    val name: String,
    val parameters: List<PortableParameter>,
    val returnType: TypeName,
    val open: Boolean,
    val abstractInBrowser: Boolean,
    val overrides: Boolean = false,
)

/** A browser constructor reproduced in common and JVM declarations. */
internal data class PortableConstructor(
    val parameters: List<PortableParameter>,
    val primary: Boolean,
) {
    /** Whether `Type()` compiles: every parameter either defaults or is a vararg. */
    val callableWithoutArguments = parameters.all { it.hasDefault || it.isVararg }

    fun key(): String = signatureKey().render()
}

/** A source companion object and the members the facade can reproduce on every target. */
internal data class PortableCompanion(
    val properties: List<PortableConstant>,
    val functions: List<PortableFunction>,
) {
    val memberCount: Int = properties.size + functions.size
}

/** A numeric companion constant whose JVM value is assigned during emission. */
internal data class PortableConstant(
    val name: String,
    val type: TypeName,
)

/** A browser `expect operator` extension whose receiver and complete signature are portable. */
internal data class PortableExtensionFunction(
    val browserMember: MemberName,
    val receiverType: TypeName,
    val function: PortableFunction,
    val sourceFile: KSFile?,
) {
    val usesInterop = receiverType.mentionsInterop() || function.mentionsInterop()
    val portablePackage =
        requireNotNull(PORTABLE_PACKAGE_BY_BROWSER_PACKAGE[browserMember.packageName]).portablePackage
    val browserAlias = "browser" + function.name.replaceFirstChar(Char::uppercaseChar)
    val key = "$receiverType.${function.key()}"
}

/** A browser string-enum value such as `ScrollBehavior.SMOOTH`. */
internal data class PortableExtensionValue(
    val browserMember: MemberName,
    val browserOwner: ClassName,
    val name: String,
    val sourceFile: KSFile?,
) {
    val portableOwner = browserOwner.toPortableName()
    val portablePackage = portableOwner.packageName
    val browserAlias = "browser$name"

    /** JVM singleton used where no browser value exists. */
    val jvmSingleton = ClassName(
        portableOwner.packageName,
        portableOwner.simpleName + name.lowercase().replaceFirstChar(Char::uppercaseChar),
    )

    val key = "${portableOwner.simpleName}.Companion.$name"
}

/** A function or constructor parameter after portable type mapping. */
internal data class PortableParameter(
    val name: String,
    val type: TypeName,
    val isVararg: Boolean,
    val hasDefault: Boolean,
)

/** A portable option-dictionary factory. */
internal data class PortableFactory(
    val parameters: List<PortableParameter>,
) {
    /** Whether a target-specific source file must implement this factory. */
    val usesInterop = parameters.any { it.type.mentionsInterop() }
}

/** Whether a function signature uses target-specific interop types. */
internal fun PortableFunction.mentionsInterop(): Boolean =
    returnType.mentionsInterop() || parameters.any { it.type.mentionsInterop() }

/** Whether a type requires a target-specific interop actual. */
internal fun TypeName.mentionsInterop(): Boolean = mentions { it.canonicalName in PORTABLE_INTEROP_NAMES }

/** Whether this type mentions a classifier matching [predicate], searching nested types recursively. */
private fun TypeName.mentions(predicate: (ClassName) -> Boolean): Boolean = when (this) {
    is ClassName -> predicate(this)
    is ParameterizedTypeName -> predicate(rawType) || typeArguments.any { it.mentions(predicate) }
    is WildcardTypeName -> (inTypes + outTypes).any { it.mentions(predicate) }
    is LambdaTypeName -> nestedTypes.any { it.mentions(predicate) }
    else -> false
}

private val LambdaTypeName.nestedTypes: List<TypeName>
    get() = listOfNotNull(receiver) + parameters.map(ParameterSpec::type) + returnType

private val PORTABLE_INTEROP_NAMES: Set<String> =
    PORTABLE_INTEROP_TYPES.values.mapTo(mutableSetOf(PORTABLE_JS_DOUBLE.canonicalName), ClassName::canonicalName)

/** Maps a generic mixin's variables to the type arguments used by this class. */
internal fun PortableClass.typeSubstitutionsFor(mixin: PortableClass): Map<String, TypeName> {
    if (mixin.typeVariables.isEmpty()) return emptyMap()
    val type = portableSuperinterfaces
        .filterIsInstance<ParameterizedTypeName>()
        .firstOrNull { it.rawType == mixin.portableName }
        ?: return emptyMap()
    return mixin.typeVariables.map(TypeVariableName::name).zip(type.typeArguments).toMap()
}

internal fun PortableProperty.substituteTypes(types: Map<String, TypeName>): PortableProperty =
    copy(type = type.substitute(types))

internal fun PortableFunction.substituteTypes(types: Map<String, TypeName>): PortableFunction = copy(
    parameters = parameters.map { it.copy(type = it.type.substitute(types)) },
    returnType = returnType.substitute(types),
)

internal fun TypeName.substitute(types: Map<String, TypeName>): TypeName = when (this) {
    is TypeVariableName -> types[name]?.copy(nullable = isNullable || types[name]?.isNullable == true) ?: this
    is ParameterizedTypeName -> rawType
        .parameterizedBy(typeArguments.map { it.substitute(types) })
        .copy(nullable = isNullable)
    else -> this
}

/** Maps a browser classifier to its configured facade package. */
internal fun ClassName.toPortableName(): ClassName =
    ClassName(
        (PORTABLE_PACKAGE_BY_BROWSER_PACKAGE[packageName]
            ?: EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE[packageName]
            ?: signatureOnlyPackageMapping(packageName)).portablePackage,
        simpleName,
    )

/** Class modality preserved by the facade and its KotlinPoet modifier. */
internal enum class ClassShape(val modifier: KModifier?) {
    INTERFACE(null),
    ABSTRACT(KModifier.ABSTRACT),
    OPEN(KModifier.OPEN),
    FINAL(KModifier.FINAL),
}

/** Reads the source declaration's modality into the portable model. */
internal fun KSClassDeclaration.shape(): ClassShape = when {
    classKind == ClassKind.INTERFACE -> ClassShape.INTERFACE
    Modifier.ABSTRACT in modifiers -> ClassShape.ABSTRACT
    Modifier.OPEN in modifiers -> ClassShape.OPEN
    else -> ClassShape.FINAL
}

/** Returns the package of a browser classifier name. */
internal fun String?.browserPackage(): String? = this?.substringBeforeLast('.')
