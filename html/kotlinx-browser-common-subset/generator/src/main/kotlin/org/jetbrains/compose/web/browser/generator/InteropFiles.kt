/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Emits portable interop declarations and target-specific actuals.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ANY as KOTLIN_ANY
import com.squareup.kotlinpoet.ARRAY
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.MUTABLE_LIST
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.NUMBER
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeAliasSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName

/*
 * The executable interop table. Every row owns the common declaration and its JS, Wasm/JS, and JVM
 * actuals, so adding a portable interop type cannot accidentally omit one target.
 *
 *   type       | JS                     | Wasm/JS               | JVM
 *   -----------|------------------------|-----------------------|-----------------------------
 *   JsAny      | kotlin.Any             | kotlin.js.JsAny       | marker interface
 *   JsString   | kotlin.String          | kotlin.js.JsString    | value wrapper
 *   JsNumber   | kotlin.Number          | kotlin.js.JsNumber    | abstract stub plus wrapper
 *   JsDouble   | kotlin.Double          | kotlin.js.JsNumber    | value wrapper
 *   JsArray<T> | kotlin.Array<T>        | kotlin.js.JsArray<T>  | list-backed stub
 *   Promise<T> | kotlin.js.Promise<T>   | kotlin.js.Promise<T>  | opaque stub
 */
private enum class InteropTarget { COMMON, JS, WASM_JS, JVM }

private class InteropRow(
    private val common: FileSpec.Builder.() -> Unit,
    private val js: FileSpec.Builder.() -> Unit,
    private val wasmJs: FileSpec.Builder.() -> Unit,
    private val jvm: FileSpec.Builder.() -> Unit,
) {
    /** Emits this row's declaration set for [target]. */
    fun emit(target: InteropTarget, file: FileSpec.Builder) = file.run(
        when (target) {
            InteropTarget.COMMON -> common
            InteropTarget.JS -> js
            InteropTarget.WASM_JS -> wasmJs
            InteropTarget.JVM -> jvm
        },
    )
}

// Portable generic shapes.

/** `T : JsAny?`, the element bound `JsArray` and `Promise` are declared with. */
private val ELEMENT = TypeVariableName("T", PORTABLE_JS_ANY.copy(nullable = true))

/** Unbounded `T` repeated on actual typealiases whose targets already enforce the bound. */
private val BARE_ELEMENT = TypeVariableName("T")

/** Covariant `T : JsAny?` used by the opaque portable Promise identity. */
private val COVARIANT_ELEMENT =
    TypeVariableName("T", PORTABLE_JS_ANY.copy(nullable = true), variance = KModifier.OUT)
private val PORTABLE_JS_ARRAY_OF_ELEMENT = PORTABLE_JS_ARRAY.parameterizedBy(ELEMENT)
private val LIST_OF_ELEMENT = LIST.parameterizedBy(ELEMENT)

// Browser members are aliased to avoid collisions with generated bridges of the same names.

private val BROWSER_LENGTH = MemberName("kotlin.js", "length")
private val BROWSER_GET = MemberName("kotlin.js", "get")
private val BROWSER_SET = MemberName("kotlin.js", "set")
private val BROWSER_TO_LIST = MemberName("kotlin.js", "toList")
private val BROWSER_TO_JS_ARRAY = MemberName("kotlin.js", "toJsArray")
private val BROWSER_TO_JS_NUMBER = MemberName("kotlin.js", "toJsNumber")
private val BROWSER_TO_JS_STRING = MemberName("kotlin.js", "toJsString")
private val BROWSER_TO_DOUBLE = MemberName("kotlin.js", "toDouble")
private val JVM_JS_NUMBER = ClassName(PORTABLE_JS_PACKAGE, "JvmJsNumber")

// Per-type common and target declarations.

private val INTEROP_ROWS = listOf(
    InteropRow(
        common = {
            addType(
                expectedType(PORTABLE_JS_ANY, interfaceType = true)
                    .addKdoc(
                        """
                        |Portable marker actualized to each target's JavaScript root type.
                        |
                        |```kotlin
                        |val raw: JsAny = media.getStartDate()
                        |```
                        """.trimMargin(),
                    )
                    .build(),
            )
        },
        js = { addTypeAlias(actualAlias(PORTABLE_JS_ANY, KOTLIN_ANY)) },
        wasmJs = { addTypeAlias(actualAlias(PORTABLE_JS_ANY, BROWSER_JS_ANY)) },
        jvm = {
            addType(actualType(PORTABLE_JS_ANY, interfaceType = true).build())
            addType(
                TypeSpec.objectBuilder(EMPTY_JS_ANY)
                    .addModifiers(KModifier.INTERNAL)
                    .addSuperinterface(PORTABLE_JS_ANY)
                    .addKdoc("Inert `JsAny` value used by JVM stubs.")
                    .build(),
            )
        },
    ),
    InteropRow(
        common = {
            addType(
                expectedType(PORTABLE_JS_STRING)
                    .addKdoc(
                        """
                        |A JavaScript string, which stays distinct from [String] on Wasm/JS.
                        |
                        |```kotlin
                        |val token: JsString? = tokens.item(0)
                        |```
                        """.trimMargin(),
                    )
                    .build(),
            )
            addFunction(expectBridge("toJsString", STRING, PORTABLE_JS_STRING))
            addFunction(expectBridge("toKotlinString", PORTABLE_JS_STRING, STRING))
        },
        js = {
            addTypeAlias(actualAlias(PORTABLE_JS_STRING, STRING))
            addFunction(actualBridge("toJsString", STRING, PORTABLE_JS_STRING, "return this"))
            addFunction(actualBridge("toKotlinString", PORTABLE_JS_STRING, STRING, "return this"))
        },
        wasmJs = {
            addTypeAlias(actualAlias(PORTABLE_JS_STRING, BROWSER_JS_STRING))
            addFunction(actualBridge("toJsString", STRING, PORTABLE_JS_STRING, "return this.%M()", BROWSER_TO_JS_STRING))
            addFunction(actualBridge("toKotlinString", PORTABLE_JS_STRING, STRING, "return toString()"))
        },
        jvm = {
            addType(
                actualType(PORTABLE_JS_STRING)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addModifiers(KModifier.INTERNAL)
                            .addParameter("value", STRING)
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder("value", STRING)
                            .addModifiers(KModifier.INTERNAL)
                            .initializer("value")
                            .build(),
                    )
                    .addFunction(
                        FunSpec.builder("toString")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(STRING)
                            .addStatement("return value")
                            .build(),
                    )
                    .build(),
            )
            addFunction(actualBridge("toJsString", STRING, PORTABLE_JS_STRING, "return %T(this)", PORTABLE_JS_STRING))
            addFunction(actualBridge("toKotlinString", PORTABLE_JS_STRING, STRING, "return value"))
        },
    ),
    InteropRow(
        common = {
            addType(
                expectedType(PORTABLE_JS_NUMBER)
                    .addModifiers(KModifier.ABSTRACT)
                    .addAnnotation(suppression(IR_SUPPRESSION))
                    .addKdoc(
                        """
                        |A JavaScript number, which stays distinct from the Kotlin numeric types on Wasm/JS.
                        |
                        |```kotlin
                        |val timestamp: JsNumber = event.timeStamp
                        |```
                        """.trimMargin(),
                    )
                    .build(),
            )
            addFunction(expectBridge("toJsNumber", DOUBLE, PORTABLE_JS_NUMBER))
            addFunction(expectBridge("toDouble", PORTABLE_JS_NUMBER, DOUBLE))
        },
        js = {
            addTypeAlias(actualAlias(PORTABLE_JS_NUMBER, NUMBER))
            addFunction(actualBridge("toJsNumber", DOUBLE, PORTABLE_JS_NUMBER, "return this"))
            addFunction(actualBridge("toDouble", PORTABLE_JS_NUMBER, DOUBLE, "return this.toDouble()"))
        },
        wasmJs = {
            addTypeAlias(actualAlias(PORTABLE_JS_NUMBER, BROWSER_JS_NUMBER))
            addFunction(actualBridge("toJsNumber", DOUBLE, PORTABLE_JS_NUMBER, "return this.%M()", BROWSER_TO_JS_NUMBER))
            addFunction(actualBridge("toDouble", PORTABLE_JS_NUMBER, DOUBLE, "return this.%M()", BROWSER_TO_DOUBLE))
        },
        jvm = {
            addType(
                actualType(PORTABLE_JS_NUMBER)
                    .addModifiers(KModifier.ABSTRACT)
                    .addProperty(
                        PropertySpec.builder("value", DOUBLE)
                            .addModifiers(KModifier.INTERNAL, KModifier.ABSTRACT)
                            .build(),
                    )
                    .build(),
            )
            addType(
                TypeSpec.classBuilder(JVM_JS_NUMBER)
                    .addModifiers(KModifier.PRIVATE)
                    .primaryConstructor(FunSpec.constructorBuilder().addParameter("value", DOUBLE).build())
                    .addProperty(
                        PropertySpec.builder("value", DOUBLE)
                            .addModifiers(KModifier.OVERRIDE)
                            .initializer("value")
                            .build(),
                    )
                    .superclass(PORTABLE_JS_NUMBER)
                    .build(),
            )
            addFunction(actualBridge("toJsNumber", DOUBLE, PORTABLE_JS_NUMBER, "return %T(this)", JVM_JS_NUMBER))
            addFunction(actualBridge("toDouble", PORTABLE_JS_NUMBER, DOUBLE, "return value"))
        },
    ),
    InteropRow(
        common = {
            addType(
                expectedType(PORTABLE_JS_DOUBLE)
                    .addAnnotation(suppression(IR_SUPPRESSION))
                    .addKdoc(
                        """
                        |A numeric Web IDL sequence element: [Double] on JS and [JsNumber] on Wasm/JS.
                        |
                        |```kotlin
                        |val dash = listOf(1.25.toJsDouble(), 2.5.toJsDouble()).toJsArray()
                        |context.setLineDash(dash)
                        |```
                        """.trimMargin(),
                    )
                    .build(),
            )
            addFunction(expectBridge("toJsDouble", DOUBLE, PORTABLE_JS_DOUBLE))
            addFunction(expectBridge("toKotlinDouble", PORTABLE_JS_DOUBLE, DOUBLE))
        },
        js = {
            addTypeAlias(actualAlias(PORTABLE_JS_DOUBLE, DOUBLE))
            addFunction(actualBridge("toJsDouble", DOUBLE, PORTABLE_JS_DOUBLE, "return this"))
            addFunction(actualBridge("toKotlinDouble", PORTABLE_JS_DOUBLE, DOUBLE, "return this"))
        },
        wasmJs = {
            addTypeAlias(actualAlias(PORTABLE_JS_DOUBLE, BROWSER_JS_NUMBER))
            addFunction(
                actualBridge("toJsDouble", DOUBLE, PORTABLE_JS_DOUBLE, "return this.%M()", BROWSER_TO_JS_NUMBER),
            )
            addFunction(
                actualBridge(
                    "toKotlinDouble",
                    PORTABLE_JS_DOUBLE,
                    DOUBLE,
                    "return this.%M()",
                    BROWSER_TO_DOUBLE,
                ),
            )
        },
        jvm = {
            addType(
                actualType(PORTABLE_JS_DOUBLE)
                    .primaryConstructor(
                        FunSpec.constructorBuilder()
                            .addModifiers(KModifier.INTERNAL)
                            .addParameter("value", DOUBLE)
                            .build(),
                    )
                    .addProperty(
                        PropertySpec.builder("value", DOUBLE)
                            .addModifiers(KModifier.INTERNAL)
                            .initializer("value")
                            .build(),
                    )
                    .build(),
            )
            addFunction(actualBridge("toJsDouble", DOUBLE, PORTABLE_JS_DOUBLE, "return %T(this)", PORTABLE_JS_DOUBLE))
            addFunction(actualBridge("toKotlinDouble", PORTABLE_JS_DOUBLE, DOUBLE, "return value"))
        },
    ),
    InteropRow(
        common = {
            addType(
                expectedType(PORTABLE_JS_ARRAY)
                    .addTypeVariable(ELEMENT)
                    .addKdoc(
                        """
                        |Portable JavaScript array identity. Use [toJsArray] to create one.
                        |
                        |```kotlin
                        |val filter: JsArray<JsString> = listOf("class".toJsString()).toJsArray()
                        |val options = MutationObserverInit(attributeFilter = filter)
                        |```
                        """.trimMargin(),
                    )
                    .build(),
            )
            addProperty(arrayLength(KModifier.EXPECT))
            addFunction(indexedFunction("get", KModifier.EXPECT))
            addFunction(indexedFunction("set", KModifier.EXPECT))
            addFunction(expectBridge("toList", PORTABLE_JS_ARRAY_OF_ELEMENT, LIST_OF_ELEMENT, generic = true))
            addFunction(expectBridge("toJsArray", LIST_OF_ELEMENT, PORTABLE_JS_ARRAY_OF_ELEMENT, generic = true))
        },
        js = {
            addTypeAlias(actualAlias(PORTABLE_JS_ARRAY, ARRAY.parameterizedBy(BARE_ELEMENT), BARE_ELEMENT))
            addProperty(arrayLength(KModifier.ACTUAL, "return size"))
            addFunction(actualIndexed("get", "return getOrNull(index)"))
            addFunction(actualIndexed("set", "this[index] = value"))
            addFunction(actualBridge("toList", PORTABLE_JS_ARRAY_OF_ELEMENT, LIST_OF_ELEMENT, "return asList()", generic = true))
            addFunction(actualBridge("toJsArray", LIST_OF_ELEMENT, PORTABLE_JS_ARRAY_OF_ELEMENT, "return toTypedArray()", generic = true))
        },
        wasmJs = {
            addTypeAlias(actualAlias(PORTABLE_JS_ARRAY, BROWSER_JS_ARRAY.parameterizedBy(BARE_ELEMENT), BARE_ELEMENT))
            addProperty(arrayLength(KModifier.ACTUAL, "return this.%M", BROWSER_LENGTH))
            addFunction(actualIndexed("get", "return this.%M(index)", BROWSER_GET))
            addFunction(actualIndexed("set", "this.%M(index, value)", BROWSER_SET))
            addFunction(actualBridge("toList", PORTABLE_JS_ARRAY_OF_ELEMENT, LIST_OF_ELEMENT, "return this.%M()", BROWSER_TO_LIST, generic = true))
            addFunction(actualBridge("toJsArray", LIST_OF_ELEMENT, PORTABLE_JS_ARRAY_OF_ELEMENT, "return this.%M()", BROWSER_TO_JS_ARRAY, generic = true))
        },
        jvm = {
            addType(jvmJsArray())
            addProperty(arrayLength(KModifier.ACTUAL, "return size"))
            addFunction(actualIndexed("get", "return elementAtOrNull(index)"))
            addFunction(actualIndexed("set", "put(index, value)"))
            addFunction(actualBridge("toList", PORTABLE_JS_ARRAY_OF_ELEMENT, LIST_OF_ELEMENT, "return snapshot()", generic = true))
            addFunction(
                actualBridge(
                    "toJsArray",
                    LIST_OF_ELEMENT,
                    PORTABLE_JS_ARRAY_OF_ELEMENT,
                    "return %T(toMutableList<%T>())",
                    PORTABLE_JS_ARRAY,
                    ELEMENT.copy(nullable = true),
                    generic = true,
                ),
            )
        },
    ),
    InteropRow(
        common = {
            addType(
                expectedType(PORTABLE_PROMISE)
                    .addTypeVariable(COVARIANT_ELEMENT)
                    .addKdoc(
                        """
                        |Opaque promise identity with star-projected results across web targets.
                        |
                        |```kotlin
                        |val pending: Promise<*> = element.requestFullscreen()
                        |```
                        """.trimMargin(),
                    )
                    .build(),
            )
        },
        js = { addTypeAlias(actualAlias(PORTABLE_PROMISE, BROWSER_PROMISE.parameterizedBy(BARE_ELEMENT), BARE_ELEMENT)) },
        wasmJs = { addTypeAlias(actualAlias(PORTABLE_PROMISE, BROWSER_PROMISE.parameterizedBy(BARE_ELEMENT), BARE_ELEMENT)) },
        jvm = {
            addType(
                actualType(PORTABLE_PROMISE)
                    .addTypeVariable(COVARIANT_ELEMENT)
                    .primaryConstructor(FunSpec.constructorBuilder().addModifiers(KModifier.INTERNAL).build())
                    .build(),
            )
        },
    ),
)

// Target file assembly.

internal fun commonInteropFile(): FileSpec = interopFile(InteropTarget.COMMON)
internal fun jsInteropFile(): FileSpec = interopFile(InteropTarget.JS)
internal fun wasmJsInteropFile(): FileSpec = interopFile(InteropTarget.WASM_JS)
internal fun jvmInteropFile(): FileSpec = interopFile(InteropTarget.JVM)

/** Adds target imports and suppressions before emitting every interop row. */
private fun interopFile(target: InteropTarget): FileSpec =
    facadeFile(PORTABLE_JS_PACKAGE, PORTABLE_INTEROP_FILE) {
        when (target) {
            InteropTarget.JS -> {
                suppress(VARIANCE_SUPPRESSION)
                addAliasedImport(BROWSER_PROMISE, BROWSER_PROMISE.browserImportAlias())
            }
            InteropTarget.WASM_JS -> {
                suppress(VARIANCE_SUPPRESSION, MODALITY_SUPPRESSION)
                listOf(BROWSER_JS_ANY, BROWSER_JS_STRING, BROWSER_JS_NUMBER, BROWSER_JS_ARRAY, BROWSER_PROMISE)
                    .forEach { addAliasedImport(it, it.browserImportAlias()) }
                listOf(
                    BROWSER_LENGTH,
                    BROWSER_GET,
                    BROWSER_SET,
                    BROWSER_TO_LIST,
                    BROWSER_TO_JS_ARRAY,
                    BROWSER_TO_JS_NUMBER,
                    BROWSER_TO_JS_STRING,
                    BROWSER_TO_DOUBLE,
                ).forEach { addAliasedImport(it, "browser" + it.simpleName.replaceFirstChar(Char::uppercaseChar)) }
            }
            else -> Unit
        }
        INTEROP_ROWS.forEach { it.emit(target, this) }
    }

// Shared KotlinPoet specs.

private fun expectedType(name: ClassName, interfaceType: Boolean = false): TypeSpec.Builder =
    interopType(name, KModifier.EXPECT, interfaceType)

private fun actualType(name: ClassName, interfaceType: Boolean = false): TypeSpec.Builder =
    interopType(name, KModifier.ACTUAL, interfaceType)

/** Builds a public expect or actual type; concrete wrappers also implement portable JsAny. */
private fun interopType(name: ClassName, modifier: KModifier, interfaceType: Boolean): TypeSpec.Builder =
    (if (interfaceType) TypeSpec.interfaceBuilder(name) else TypeSpec.classBuilder(name))
        .addModifiers(KModifier.PUBLIC, modifier)
        .apply { if (!interfaceType) addSuperinterface(PORTABLE_JS_ANY) }

/** Emits the star-projected array length shared by every element type. */
private fun arrayLength(modifier: KModifier, body: String? = null, vararg arguments: Any): PropertySpec =
    PropertySpec.builder("length", INT)
        .addModifiers(KModifier.PUBLIC, modifier)
        .receiver(PORTABLE_JS_ARRAY.parameterizedBy(STAR))
        .apply { body?.let { getter(FunSpec.getterBuilder().addStatement(it, *arguments).build()) } }
        .build()

/** Emits nullable indexed reads and typed indexed writes for portable arrays. */
private fun indexedFunction(name: String, modifier: KModifier): FunSpec = FunSpec.builder(name)
    .addModifiers(KModifier.PUBLIC, modifier, KModifier.OPERATOR)
    .addTypeVariable(ELEMENT)
    .receiver(PORTABLE_JS_ARRAY_OF_ELEMENT)
    .addParameter("index", INT)
    .apply {
        if (name == "get") returns(ELEMENT.copy(nullable = true))
        else addParameter("value", ELEMENT)
    }
    .build()

private fun actualIndexed(name: String, body: String, vararg arguments: Any): FunSpec =
    indexedFunction(name, KModifier.ACTUAL).toBuilder().addStatement(body, *arguments).build()

private fun expectBridge(
    name: String,
    receiver: TypeName,
    returns: TypeName,
    generic: Boolean = false,
): FunSpec = bridge(name, receiver, returns, KModifier.EXPECT, generic).build()

private fun actualBridge(
    name: String,
    receiver: TypeName,
    returns: TypeName,
    body: String,
    vararg arguments: Any,
    generic: Boolean = false,
): FunSpec = bridge(name, receiver, returns, KModifier.ACTUAL, generic)
    .addStatement(body, *arguments)
    .build()

/** Builds an expect or actual receiver conversion, optionally with the portable element bound. */
private fun bridge(
    name: String,
    receiver: TypeName,
    returns: TypeName,
    modifier: KModifier,
    generic: Boolean,
): FunSpec.Builder = FunSpec.builder(name)
    .addModifiers(KModifier.PUBLIC, modifier)
    .apply { if (generic) addTypeVariable(ELEMENT) }
    .receiver(receiver)
    .returns(returns)

private fun actualAlias(
    portable: ClassName,
    target: TypeName,
    vararg variables: TypeVariableName,
): TypeAliasSpec = TypeAliasSpec.builder(portable.simpleName, target)
    .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL)
    .apply { variables.forEach(::addTypeVariable) }
    .build()

/** Emulates JavaScript indexed writes by padding gaps with nulls in list-backed storage. */
private fun jvmJsArray(): TypeSpec {
    val storage = MUTABLE_LIST.parameterizedBy(ELEMENT.copy(nullable = true))
    return actualType(PORTABLE_JS_ARRAY)
        .addTypeVariable(ELEMENT)
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addModifiers(KModifier.INTERNAL)
                .addParameter(ParameterSpec.builder("values", storage).defaultValue("mutableListOf()").build())
                .build(),
        )
        .addProperty(
            PropertySpec.builder("values", storage)
                .addModifiers(KModifier.PRIVATE)
                .initializer("values")
                .build(),
        )
        .addProperty(
            PropertySpec.builder("size", INT)
                .addModifiers(KModifier.INTERNAL)
                .getter(FunSpec.getterBuilder().addStatement("return values.size").build())
                .build(),
        )
        .addFunction(
            FunSpec.builder("elementAtOrNull")
                .addModifiers(KModifier.INTERNAL)
                .addParameter("index", INT)
                .returns(ELEMENT.copy(nullable = true))
                .addStatement("return values.getOrNull(index)")
                .build(),
        )
        .addFunction(
            FunSpec.builder("put")
                .addModifiers(KModifier.INTERNAL)
                .addParameter("index", INT)
                .addParameter("value", ELEMENT)
                .beginControlFlow("while (values.size <= index)")
                .addStatement("values += null")
                .endControlFlow()
                .addStatement("values[index] = value")
                .build(),
        )
        .addFunction(
            FunSpec.builder("snapshot")
                .addModifiers(KModifier.INTERNAL)
                .addAnnotation(suppression("UNCHECKED_CAST"))
                .returns(LIST_OF_ELEMENT)
                .addStatement("return values.toList() as %T", LIST_OF_ELEMENT)
                .build(),
        )
        .build()
}
