/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

// Emits stateful JVM stubs for option dictionaries.
package org.jetbrains.compose.web.browser.generator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec

// Stateful option dictionaries.

private data class JvmDictionaryProperty(
    val property: CommonProperty,
    val parameter: CommonParameter,
)

/** Adds the backing implementation and factory that retain dictionary arguments on JVM. */
internal fun FileSpec.Builder.addJvmDictionaryState(dictionary: CommonClass, values: JvmStubValues) {
    val factory = dictionary.factory ?: return
    val implementation = dictionary.jvmDictionaryImplementationName()
    val properties = dictionary.jvmDictionaryProperties(factory, values)
    addType(dictionary.jvmDictionaryImplementation(implementation, properties))
    addFunction(dictionary.jvmFactory(factory, implementation, properties))
}

/** Pairs visible dictionary properties with factory parameters, including KSP keyword spellings. */
private fun CommonClass.jvmDictionaryProperties(
    factory: CommonFactory,
    values: JvmStubValues,
): List<JvmDictionaryProperty> {
    val visibleDictionaries = listOf(this) + values.mixinClosure(this).mapNotNull(values.classes::get)
    val visibleProperties = visibleDictionaries
        .flatMap(CommonClass::properties)
        .distinctBy { "${it.name}:${it.type}" }

    return visibleProperties.map { property ->
        val parameterNames = setOf(property.name, "${property.name}_", "param_${property.name}")
        val parameter = factory.parameters.singleOrNull {
            it.name in parameterNames && it.type == property.type
        }
        checkNotNull(parameter) {
            "No factory parameter initializes ${commonName.canonicalName}.${property.name}: " +
                "expected one of $parameterNames with type ${property.type}"
        }
        JvmDictionaryProperty(property, parameter)
    }.sortedBy { factory.parameters.indexOf(it.parameter) }
}

private fun CommonClass.jvmDictionaryImplementation(
    implementation: ClassName,
    properties: List<JvmDictionaryProperty>,
): TypeSpec = TypeSpec.classBuilder(implementation)
    .addModifiers(KModifier.PRIVATE)
    .primaryConstructor(
        FunSpec.constructorBuilder()
            .apply { properties.forEach { addParameter(it.parameter.spec(null)) } }
            .build(),
    )
    .addSuperinterface(commonName)
    .apply {
        properties.forEach { stored ->
            addProperty(
                PropertySpec.builder(stored.property.name, stored.property.type)
                    .mutable(stored.property.mutable)
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("%N", stored.parameter.name)
                    .build(),
            )
        }
    }
    .build()

private fun CommonClass.jvmFactory(
    factory: CommonFactory,
    implementation: ClassName,
    properties: List<JvmDictionaryProperty>,
): FunSpec = FunSpec.builder(commonName.simpleName)
    .addModifiers(KModifier.PUBLIC, KModifier.ACTUAL)
    .returns(commonName)
    .apply { factory.parameters.forEach { addParameter(it.spec(null)) } }
    .addStatement(
        "return %T(%L)",
        implementation,
        CodeBlock.builder()
            .apply {
                properties.forEachIndexed { index, stored ->
                    if (index > 0) add(", ")
                    add("%N", stored.parameter.name)
                }
            }
            .build(),
    )
    .build()

private fun CommonClass.jvmDictionaryImplementationName(): ClassName =
    ClassName(commonName.packageName, "Jvm${commonName.simpleName}")
