// Maps browser types to portable KotlinPoet types and reports unsupported shapes.
package prototype.dom.generator

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSTypeAlias
import com.google.devtools.ksp.symbol.KSTypeParameter
import com.google.devtools.ksp.symbol.Variance
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName

/**
 * Maps browser types recursively while preserving nullability and projections.
 * Accepts built-ins, closure classifiers, portable interop types, and plain callbacks.
 */
internal class PortableTypeMapper(
    val portableNames: Map<String, ClassName>,
    private val signatureOnlyPackages: Set<String> = emptySet(),
) {
    fun result(type: KSType?): TypeMapping {
        if (type == null) return TypeMapping.Skipped(SkipReason.MISSING_TYPE, "type is absent")
        if (type.isError) return TypeMapping.Skipped(SkipReason.UNRESOLVED_TYPE, type.toString())

        // Suspend callbacks cannot be actualized by a typealias to a plain browser callback.
        if (type.isSuspendFunctionType) {
            return TypeMapping.Skipped(SkipReason.CALLBACK_TYPE, "suspend function type: $type")
        }
        if (type.isFunctionType) return functionType(type)

        val declaration = type.declaration
        val qualifiedName = declaration.qualifiedName?.asString()
        // Interop types are themselves typealiases on JS and must be handled before general rejection.
        interop(qualifiedName, type)?.let { return it }
        if (declaration is KSTypeAlias) {
            return TypeMapping.Skipped(SkipReason.TYPE_ALIAS, type.toString())
        }
        if (declaration is KSTypeParameter) {
            return TypeMapping.Ported(
                TypeVariableName(declaration.name.asString()).copy(nullable = type.isMarkedNullable),
            )
        }
        val classDeclaration = declaration as? KSClassDeclaration
            ?: return TypeMapping.Skipped(
                if (qualifiedName == null) SkipReason.TYPE_PARAMETER else SkipReason.UNRESOLVED_TYPE,
                type.toString(),
            )
        val className = classDeclaration.qualifiedName?.asString()
            ?: return TypeMapping.Skipped(SkipReason.UNRESOLVED_TYPE, type.toString())
        val portable = BUILTIN_TYPES[className] ?: portableNames[className]
        if (portable != null) return parameterized(type, portable)

        val packageName = className.substringBeforeLast('.', missingDelimiterValue = "")
        val reason = if (
            packageName in PORTABLE_PACKAGE_BY_BROWSER_PACKAGE ||
            packageName in signatureOnlyPackages
        ) {
            SkipReason.UNSELECTED_CLASSIFIER
        } else {
            SkipReason.UNSUPPORTED_PACKAGE
        }
        return TypeMapping.Skipped(reason, className)
    }

    /** The mapping for one of the interop types, or `null` when [qualifiedName] names none of them. */
    private fun interop(qualifiedName: String?, type: KSType): TypeMapping? {
        val portable = PORTABLE_INTEROP_TYPES[qualifiedName] ?: return null
        return when (portable) {
            // Promise results differ between JS and Wasm/JS, so the portable result remains opaque.
            PORTABLE_PROMISE -> TypeMapping.Ported(
                portable.parameterizedBy(STAR).copy(nullable = type.isMarkedNullable),
            )
            else -> parameterized(type, portable)
        }
    }

    /** Maps [raw]'s type arguments recursively, preserving missing-classifier discovery. */
    private fun parameterized(type: KSType, raw: ClassName): TypeMapping {
        if (type.arguments.isEmpty()) {
            return TypeMapping.Ported(raw.copy(nullable = type.isMarkedNullable))
        }
        val arguments = typeArguments(type, raw)
        arguments.firstSkip()?.let { return it }
        return TypeMapping.Ported(
            raw.parameterizedBy(arguments.map { (it as TypeMapping.Ported).type })
                .copy(nullable = type.isMarkedNullable),
        )
    }

    /** Reconstructs a KSP `FunctionN` as an unnamed Kotlin function type. */
    private fun functionType(type: KSType): TypeMapping {
        // Receiver function types cannot be flattened into plain callbacks without changing type identity.
        if (type.annotations.any { it.shortName.asString() == EXTENSION_FUNCTION_TYPE }) {
            return TypeMapping.Skipped(SkipReason.CALLBACK_TYPE, "function type with a receiver: $type")
        }
        val arguments = typeArguments(type)
        if (arguments.isEmpty()) {
            return TypeMapping.Skipped(SkipReason.CALLBACK_TYPE, "function type without a result: $type")
        }
        arguments.firstSkip()?.let { return it }
        val mapped = arguments.map { (it as TypeMapping.Ported).type }
        return TypeMapping.Ported(
            LambdaTypeName
                .get(
                    parameters = mapped.dropLast(1).map(ParameterSpec::unnamed),
                    returnType = mapped.last(),
                )
                .copy(nullable = type.isMarkedNullable),
        )
    }

    /** Maps type arguments while preserving variance; star projections are unsupported. */
    private fun typeArguments(type: KSType, raw: ClassName? = null): List<TypeMapping> = type.arguments.map { argument ->
        if (argument.variance == Variance.STAR) {
            return@map TypeMapping.Skipped(SkipReason.GENERIC_TYPE, "star-projected argument: $type")
        }
        val resolved = argument.type?.resolve()
            ?: return@map TypeMapping.Skipped(SkipReason.GENERIC_TYPE, "unresolved argument: $type")
        // Numeric Web IDL sequences differ only in their element classifier: Kotlin/JS exposes
        // Array<Double>, while Kotlin/Wasm exposes JsArray<JsNumber>. A portable JsDouble actualizes
        // to precisely those two scalar types, so the surrounding invariant array stays identical.
        val mapping = if (
            raw == PORTABLE_JS_ARRAY &&
            resolved.declaration.qualifiedName?.asString() == BROWSER_JS_NUMBER.canonicalName
        ) {
            TypeMapping.Ported(PORTABLE_JS_DOUBLE.copy(nullable = resolved.isMarkedNullable))
        } else {
            result(resolved)
        }
        when (mapping) {
            is TypeMapping.Skipped -> mapping
            is TypeMapping.Ported -> TypeMapping.Ported(mapping.type.projected(argument.variance))
        }
    }
}

/** Marks the `FunctionN` that stands for a callback with a receiver; see [PortableTypeMapper]. */
private const val EXTENSION_FUNCTION_TYPE = "ExtensionFunctionType"

/** Returns the first failed nested type mapping. */
private fun List<TypeMapping>.firstSkip(): TypeMapping.Skipped? =
    firstNotNullOfOrNull { it as? TypeMapping.Skipped }

private fun TypeName.projected(variance: Variance): TypeName = when (variance) {
    Variance.COVARIANT -> WildcardTypeName.producerOf(this)
    Variance.CONTRAVARIANT -> WildcardTypeName.consumerOf(this)
    // STAR is rejected before it gets here, and INVARIANT is the type itself.
    else -> this
}

internal sealed interface TypeMapping {
    data class Ported(val type: TypeName) : TypeMapping

    data class Skipped(val reason: SkipReason, val detail: String) : TypeMapping
}
