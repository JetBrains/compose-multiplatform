/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.compose.internal.utils.property
import javax.inject.Inject

private const val DEFAULT_PROGUARD_VERSION = "7.8.0"

abstract class ProguardSettings @Inject constructor(
    objects: ObjectFactory,
) {
    val version: Property<String> = objects.property(DEFAULT_PROGUARD_VERSION)
    val maxHeapSize: Property<String> = objects.property()
    val configurationFiles: ConfigurableFileCollection = objects.fileCollection()
    val isEnabled: Property<Boolean> = objects.property(false)
    val obfuscate: Property<Boolean> = objects.property(false)
    val optimize: Property<Boolean> = objects.property(true)
    val joinOutputJars: Property<Boolean> = objects.property(false)
}
