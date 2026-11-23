/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.ios

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.compose.ExperimentalComposeApi
import org.jetbrains.compose.ios.dsl.IosRenderingBackend
import javax.inject.Inject

abstract class IosExtension @Inject constructor(
    objects: ObjectFactory,
) {
    @ExperimentalComposeApi
    val renderingBackend: Property<IosRenderingBackend> =
        objects.property(IosRenderingBackend::class.java).convention(IosRenderingBackend.Ganesh)
}
