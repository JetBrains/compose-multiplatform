/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose

import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Property
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import javax.inject.Inject

abstract class ComposeExtension @Inject constructor(
    objects: ObjectFactory
) : ExtensionAware {
    val kotlinCompilerPlugin: Property<String?> = objects.nullableProperty()
}

