/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Produces inert values for generated JVM stubs and defaults.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.BYTE
import com.squareup.kotlinpoet.CHAR
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.SHORT
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.WildcardTypeName

/** `kotlin.collections.emptyList`, the seed of an inert `JsArray`. */
private val EMPTY_LIST = MemberName("kotlin.collections", "emptyList")

/**
 * Supplies JVM property initializers, factory defaults, and stub return values.
 * Requests a per-classifier singleton when no standalone value exists.
 */
internal class JvmStubValues(val classes: Map<ClassName, PortableClass>) {
    private val singletons = linkedMapOf<ClassName, ClassName>()
    private val mixinClosures = mutableMapOf<ClassName, Set<ClassName>>()
    private val storedInterfaces = mutableMapOf<ClassName, List<PortableClass>>()

    fun value(type: TypeName): CodeBlock =
        inertLiteral(type, classes) ?: jvmInteropLiteral(type) ?: callback(type) ?: singleton(type)

    /** Renders requested fallbacks to a fixed point because rendering one can request more. */
    fun singletonFiles(): List<FileSpec> {
        val rendered = linkedMapOf<ClassName, TypeSpec>()
        while (true) {
            val pending = singletons.filterKeys { it !in rendered }
            if (pending.isEmpty()) break
            pending.forEach { (facadeType, singletonName) ->
                rendered[facadeType] = classes.getValue(facadeType).jvmSingleton(singletonName, this)
            }
        }
        return rendered.entries
            .groupBy({ it.key.packageName }, Map.Entry<ClassName, TypeSpec>::value)
            .toSortedMap()
            .map { (packageName, types) ->
                facadeFile(packageName, "JvmStubValues") { types.forEach(::addType) }
            }
    }

    /** Returns every mixin reachable from [owner], preserving declaration order. */
    fun mixinClosure(owner: PortableClass): Set<ClassName> = mixinClosures.getOrPut(owner.portableName) {
        val reached = linkedSetOf<ClassName>()

        fun walk(name: ClassName) {
            if (reached.add(name)) classes[name]?.superinterfaces?.forEach(::walk)
        }

        owner.superinterfaces.forEach(::walk)
        reached
    }

    /** Keeps only mixins whose members are not already supplied by an ancestor class. */
    fun jvmStoredInterfaces(owner: PortableClass): List<PortableClass> =
        storedInterfaces.getOrPut(owner.portableName) {
            val above = owner.ancestors
                .mapNotNull(classes::get)
                .flatMapTo(mutableSetOf(), ::mixinClosure)
            (mixinClosure(owner) - above).mapNotNull(classes::get)
        }

    /** Creates callbacks whose return values require singleton support. */
    private fun callback(type: TypeName): CodeBlock? =
        (type as? LambdaTypeName)?.inertLambda(value(type.returnType))

    private fun singleton(type: TypeName): CodeBlock {
        val facadeType = type as? ClassName ?: error("No JVM stub value for $type")
        val facade = classes[facadeType] ?: error("No JVM stub value for $facadeType")
        // Final classes without no-argument construction cannot use a subclass singleton.
        check(facade.shape != ClassShape.FINAL || facade.jvmConstructibleWithoutArguments) {
            "No JVM stub value for the final $facadeType: every constructor it declares takes arguments"
        }
        val name = singletons.getOrPut(facadeType) {
            ClassName(facadeType.packageName, "Empty${facadeType.simpleName}")
        }
        return CodeBlock.of("%T", name)
    }
}

/** Builds a requested singleton as an interface implementation or class subclass. */
private fun PortableClass.jvmSingleton(name: ClassName, values: JvmStubValues): TypeSpec =
    TypeSpec.objectBuilder(name)
        .addModifiers(KModifier.INTERNAL)
        .apply {
            if (shape == ClassShape.INTERFACE) {
                addSuperinterface(portableName)
                val inherited = values.mixinClosure(this@jvmSingleton).mapNotNull(values.classes::get)
                addJvmStoredMembers(this@jvmSingleton, listOf(this@jvmSingleton) + inherited, values)
            } else {
                superclass(portableName)
            }
        }
        .build()

/** Returns a standalone inert value for [type], or `null` when a singleton is required. */
internal fun inertLiteral(type: TypeName, classes: Map<ClassName, PortableClass> = emptyMap()): CodeBlock? {
    if (type.isNullable) return CodeBlock.of("null")
    if (type is LambdaTypeName) {
        // Facade return types need singleton support unavailable to common factory defaults.
        if (type.returnType == UNIT) return type.inertLambda(null)
        return inertLiteral(type.returnType, classes)?.let(type::inertLambda)
    }
    if (type is ParameterizedTypeName && type.rawType == PORTABLE_JS_ARRAY) {
        return CodeBlock.of("%M<%T>().%M()", EMPTY_LIST, type.typeArguments.single().unprojected(), TO_JS_ARRAY)
    }
    return when (type) {
        BOOLEAN -> CodeBlock.of("false")
        STRING -> CodeBlock.of("%S", "")
        BYTE, SHORT, INT -> CodeBlock.of("0")
        LONG -> CodeBlock.of("0L")
        FLOAT -> CodeBlock.of("0.0F")
        DOUBLE -> CodeBlock.of("0.0")
        CHAR -> CodeBlock.of("' '")
        // The interop bridges are declared on every target, so these read the same in common code.
        PORTABLE_JS_STRING -> CodeBlock.of("%S.%M()", "", TO_JS_STRING)
        PORTABLE_JS_NUMBER -> CodeBlock.of("0.0.%M()", TO_JS_NUMBER)
        PORTABLE_JS_DOUBLE -> CodeBlock.of("0.0.%M()", TO_JS_DOUBLE)
        else -> {
            val facade = classes[type] ?: return null
            when (facade.shape) {
                // Option dictionaries use their generated factory.
                ClassShape.INTERFACE -> facade.factory?.let { CodeBlock.of("%T()", type) }
                // Concrete types use a no-argument constructor when available.
                ClassShape.OPEN, ClassShape.FINAL ->
                    if (facade.jvmConstructibleWithoutArguments) CodeBlock.of("%T()", type) else null
                ClassShape.ABSTRACT -> null
            }
        }
    }
}

/** Builds a callback that ignores its parameters and returns [result] when present. */
private fun LambdaTypeName.inertLambda(result: CodeBlock?): CodeBlock {
    val ignored = if (parameters.isEmpty()) "" else parameters.joinToString(postfix = " -> ") { "_" }
    return if (result == null) CodeBlock.of("{ %L}", ignored) else CodeBlock.of("{ %L%L }", ignored, result)
}

/** Removes a projection so an inert collection can use the underlying element type. */
private fun TypeName.unprojected(): TypeName = when (this) {
    is WildcardTypeName -> inTypes.singleOrNull() ?: outTypes.single()
    else -> this
}

/** JVM-only values for interop types that cannot be constructed in common code. */
private fun jvmInteropLiteral(type: TypeName): CodeBlock? = when {
    type == PORTABLE_JS_ANY -> CodeBlock.of("%T", EMPTY_JS_ANY)
    type is ParameterizedTypeName && type.rawType == PORTABLE_PROMISE ->
        CodeBlock.of("%T<%T>()", PORTABLE_PROMISE, NOTHING.copy(nullable = true))
    else -> null
}
