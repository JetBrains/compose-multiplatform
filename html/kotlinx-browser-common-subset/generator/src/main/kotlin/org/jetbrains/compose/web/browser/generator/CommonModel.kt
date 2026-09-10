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
internal data class CommonClass(
    val browserName: ClassName,
    val parentBrowserName: ClassName?,
    val superinterfaces: List<ClassName>,
    val ancestors: List<ClassName>,
    val shape: ClassShape,
    val isDictionary: Boolean,
    /** Whether the browser declaration lists the interop marker. */
    val isJsAny: Boolean,
    val properties: List<CommonProperty>,
    val functions: List<CommonFunction>,
    val constructors: List<CommonConstructor>,
    val companion: CommonCompanion?,
    val factory: CommonFactory?,
    val sourceFile: KSFile?,
    /** Classifier parameters preserved by common, web typealias, and JVM declarations. */
    val typeVariables: List<TypeVariableName> = emptyList(),
    /** Parameterized forms of [superinterfaces], in the same order. */
    val superinterfaceTypes: List<TypeName> = emptyList(),
) {
    val commonName = browserName.toCommonName()
    val commonParentName = parentBrowserName?.toCommonName()
    val commonSuperinterfaces: List<TypeName>
        get() = superinterfaceTypes.ifEmpty { superinterfaces }
    val instanceMemberCount = properties.size + functions.size
    val memberCount = instanceMemberCount + (companion?.memberCount ?: 0)

    /** Whether any emitted function or constructor parameter has a default. */
    val hasDefaultArguments = functions.any(CommonFunction::hasDefaultArgument) ||
        companion?.functions.orEmpty().any(CommonFunction::hasDefaultArgument) ||
        constructors.any(CommonConstructor::hasDefaultArgument)

    /** Uses the browser primary constructor or the first constructor for JVM delegation. */
    val jvmPrimaryConstructor = constructors.firstOrNull(CommonConstructor::primary) ?: constructors.firstOrNull()

    /** Whether the JVM stub can be instantiated as `Type()`. */
    val jvmConstructibleWithoutArguments =
        constructors.isEmpty() || constructors.any(CommonConstructor::callableWithoutArguments)

    /** Restores a protected no-argument path required by JVM subclasses. */
    val needsJvmFallbackConstructor = !jvmConstructibleWithoutArguments && shape != ClassShape.FINAL

    private val hasDictionaryProperties = isDictionary && properties.isNotEmpty()
    private val hasOpaquePromise = properties.any { it.type.mentionsPromise() } ||
        functions.any(CommonFunction::mentionsPromise) ||
        companion?.properties.orEmpty().any { it.type.mentionsPromise() } ||
        companion?.functions.orEmpty().any(CommonFunction::mentionsPromise) ||
        constructors.any(CommonConstructor::mentionsPromise)
    private val hasAbstractClassProperties =
        shape != ClassShape.INTERFACE && properties.any(CommonProperty::abstractInBrowser)
    private val hasIrIncompatibleClassFunctions = shape != ClassShape.INTERFACE && functions.any {
        it.abstractInBrowser || (it.open && it.parameters.any(CommonParameter::hasDefault))
    }

    /** Flags unavoidable expect/actual modality, default, dictionary, or Promise mismatches. */
    val needsIrSuppression = hasDictionaryProperties || hasOpaquePromise ||
        hasAbstractClassProperties || hasIrIncompatibleClassFunctions
}

private fun TypeName.mentionsPromise(): Boolean = mentions { it == COMMON_PROMISE }

private fun CommonFunction.hasDefaultArgument(): Boolean = parameters.any(CommonParameter::hasDefault)

private fun CommonConstructor.hasDefaultArgument(): Boolean = parameters.any(CommonParameter::hasDefault)

private fun CommonFunction.mentionsPromise(): Boolean =
    returnType.mentionsPromise() || parameters.any { it.type.mentionsPromise() }

private fun CommonConstructor.mentionsPromise(): Boolean = parameters.any { it.type.mentionsPromise() }

/** A common instance property and the modality needed by generated targets. */
internal data class CommonProperty(
    val name: String,
    val type: TypeName,
    val mutable: Boolean,
    val open: Boolean,
    val abstractInBrowser: Boolean,
    val overrides: Boolean = false,
)

/** A common instance function and its complete signature. */
internal data class CommonFunction(
    val name: String,
    val parameters: List<CommonParameter>,
    val returnType: TypeName,
    val open: Boolean,
    val abstractInBrowser: Boolean,
    val overrides: Boolean = false,
)

/** A browser constructor reproduced in common and JVM declarations. */
internal data class CommonConstructor(
    val parameters: List<CommonParameter>,
    val primary: Boolean,
) {
    /** Whether `Type()` compiles: every parameter either defaults or is a vararg. */
    val callableWithoutArguments = parameters.all { it.hasDefault || it.isVararg }

    fun key(): String = signatureKey().render()
}

/** A source companion object and the members the facade can reproduce on every target. */
internal data class CommonCompanion(
    val properties: List<CommonConstant>,
    val functions: List<CommonFunction>,
) {
    val memberCount: Int = properties.size + functions.size
}

/** A numeric companion constant whose JVM value is assigned during emission. */
internal data class CommonConstant(
    val name: String,
    val type: TypeName,
)

/** A browser `expect operator` extension whose receiver and complete signature are common. */
internal data class CommonExtensionFunction(
    val browserMember: MemberName,
    val receiverType: TypeName,
    val function: CommonFunction,
    val sourceFile: KSFile?,
) {
    val usesInterop = receiverType.mentionsInterop() || function.mentionsInterop()
    val commonPackage =
        requireNotNull(COMMON_PACKAGE_BY_BROWSER_PACKAGE[browserMember.packageName]).commonPackage
    val browserAlias = "browser" + function.name.replaceFirstChar(Char::uppercaseChar)
    val key = "$receiverType.${function.key()}"
}

/** A browser string-enum value such as `ScrollBehavior.SMOOTH`. */
internal data class CommonExtensionValue(
    val browserMember: MemberName,
    val browserOwner: ClassName,
    val name: String,
    val sourceFile: KSFile?,
) {
    val commonOwner = browserOwner.toCommonName()
    val commonPackage = commonOwner.packageName
    val browserAlias = "browser$name"

    /** JVM singleton used where no browser value exists. */
    val jvmSingleton = ClassName(
        commonOwner.packageName,
        commonOwner.simpleName + name.lowercase().replaceFirstChar(Char::uppercaseChar),
    )

    val key = "${commonOwner.simpleName}.Companion.$name"
}

/** A function or constructor parameter after common type mapping. */
internal data class CommonParameter(
    val name: String,
    val type: TypeName,
    val isVararg: Boolean,
    val hasDefault: Boolean,
)

/** A common option-dictionary factory. */
internal data class CommonFactory(
    val parameters: List<CommonParameter>,
) {
    /** Whether a target-specific source file must implement this factory. */
    val usesInterop = parameters.any { it.type.mentionsInterop() }
}

/** Whether a function signature uses target-specific interop types. */
internal fun CommonFunction.mentionsInterop(): Boolean =
    returnType.mentionsInterop() || parameters.any { it.type.mentionsInterop() }

/** Whether a type requires a target-specific interop actual. */
internal fun TypeName.mentionsInterop(): Boolean = mentions { it.canonicalName in COMMON_INTEROP_NAMES }

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

private val COMMON_INTEROP_NAMES: Set<String> =
    COMMON_INTEROP_TYPES.values.mapTo(mutableSetOf(COMMON_JS_DOUBLE.canonicalName), ClassName::canonicalName)

/** Maps a generic mixin's variables to the type arguments used by this class. */
internal fun CommonClass.typeSubstitutionsFor(mixin: CommonClass): Map<String, TypeName> {
    if (mixin.typeVariables.isEmpty()) return emptyMap()
    val type = commonSuperinterfaces
        .filterIsInstance<ParameterizedTypeName>()
        .firstOrNull { it.rawType == mixin.commonName }
        ?: return emptyMap()
    return mixin.typeVariables.map(TypeVariableName::name).zip(type.typeArguments).toMap()
}

internal fun CommonProperty.substituteTypes(types: Map<String, TypeName>): CommonProperty =
    copy(type = type.substitute(types))

internal fun CommonFunction.substituteTypes(types: Map<String, TypeName>): CommonFunction = copy(
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
internal fun ClassName.toCommonName(): ClassName =
    ClassName(
        (COMMON_PACKAGE_BY_BROWSER_PACKAGE[packageName]
            ?: EXTERNAL_PACKAGE_BY_BROWSER_PACKAGE[packageName]
            ?: signatureOnlyPackageMapping(packageName)).commonPackage,
        simpleName,
    )

/** Class modality preserved by the facade and its KotlinPoet modifier. */
internal enum class ClassShape(val modifier: KModifier?) {
    INTERFACE(null),
    ABSTRACT(KModifier.ABSTRACT),
    OPEN(KModifier.OPEN),
    FINAL(KModifier.FINAL),
}

/** Reads the source declaration's modality into the common model. */
internal fun KSClassDeclaration.shape(): ClassShape = when {
    classKind == ClassKind.INTERFACE -> ClassShape.INTERFACE
    Modifier.ABSTRACT in modifiers -> ClassShape.ABSTRACT
    Modifier.OPEN in modifiers -> ClassShape.OPEN
    else -> ClassShape.FINAL
}

/** Returns the package of a browser classifier name. */
internal fun String?.browserPackage(): String? = this?.substringBeforeLast('.')
