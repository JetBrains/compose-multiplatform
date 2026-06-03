/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.internal.utils

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.extraProperties

internal inline fun <reified T : Any> ObjectFactory.new(vararg params: Any): T =
    newInstance(T::class.java, *params)

internal inline fun <reified T : Any> ObjectFactory.property(): Property<T> =
    property(T::class.java)

internal inline fun <reified T : Any> ObjectFactory.property(defaultValue: T): Property<T> =
    property(T::class.java).value(defaultValue)

internal inline fun <reified T : Any> Provider<T>.toProperty(objects: ObjectFactory): Property<T> =
    objects.property(T::class.java).value(this)

internal inline fun <reified T : Any> Task.provider(noinline fn: () -> T): Provider<T> =
    project.provider(fn)

internal fun Provider<String>.toBooleanProvider(defaultValue: Boolean): Provider<Boolean> =
    orElse(defaultValue.toString()).map { "true" == it }

internal fun Project.findLocalOrGlobalProperty(name: String, default: String = ""): Provider<String> = provider {
    if (extraProperties.has(name)) extraProperties.get(name)?.toString() ?: default
    else providers.gradleProperty(name).getOrElse(default)
}
