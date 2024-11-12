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
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.gradle.util.GradleVersion

internal inline fun <reified T> ObjectFactory.new(vararg params: Any): T =
    newInstance(T::class.java, *params)

@SuppressWarnings("UNCHECKED_CAST")
internal inline fun <reified T : Any> ObjectFactory.nullableProperty(): Property<T?> =
    property(T::class.java) as Property<T?>

internal inline fun <reified T : Any> ObjectFactory.notNullProperty(): Property<T> =
    property(T::class.java)

internal inline fun <reified T : Any> ObjectFactory.notNullProperty(defaultValue: T): Property<T> =
    property(T::class.java).value(defaultValue)

internal inline fun <reified T> Provider<T>.toProperty(objects: ObjectFactory): Property<T> =
    objects.property(T::class.java).value(this)

internal inline fun <reified T> Task.provider(noinline fn: () -> T): Provider<T> =
    project.provider(fn)

internal fun ProviderFactory.valueOrNull(prop: String): Provider<String?> =
    provider {
        gradleProperty(prop).forUseAtConfigurationTimeSafe().orNull
    }

private fun Provider<String?>.forUseAtConfigurationTimeSafe(): Provider<String?> =
    // forUseAtConfigurationTime is a no-op starting at Gradle 7.4 and just produces deprecation warnings. 
    // See https://docs.gradle.org/current/kotlin-dsl/gradle/org.gradle.api.provider/-provider/for-use-at-configuration-time.html
    if (GradleVersion.current() >= GradleVersion.version("7.4")) {
        this
    } else {
        // todo: remove once we drop support for Gradle 6.4
        forUseAtConfigurationTime()
    }

internal fun Provider<String?>.toBooleanProvider(defaultValue: Boolean): Provider<Boolean> =
    orElse(defaultValue.toString()).map { "true" == it }

internal fun Project.findLocalOrGlobalProperty(name: String, default: String = ""): Provider<String> = provider {
    if (extraProperties.has(name)) extraProperties.get(name).toString()
    else providers.gradleProperty(name).forUseAtConfigurationTimeSafe().getOrElse(default)
}
