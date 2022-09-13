/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

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

internal fun Provider<Directory>.file(relativePath: String): Provider<RegularFile> =
    map { it.file(relativePath) }

internal fun Provider<Directory>.dir(relativePath: String): Provider<Directory> =
    map { it.dir(relativePath) }

internal inline fun <reified T> ObjectFactory.new(vararg params: Any): T =
    newInstance(T::class.java, *params)

internal fun <T : Task> TaskProvider<T>.dependsOn(vararg dependencies: Any) {
    configure { it.dependsOn(*dependencies) }
}

internal val <T : FileSystemLocation> Provider<T>.ioFile: File
    get() = get().asFile

internal val <T : FileSystemLocation> Provider<T>.ioFileOrNull: File?
    get() = orNull?.asFile
