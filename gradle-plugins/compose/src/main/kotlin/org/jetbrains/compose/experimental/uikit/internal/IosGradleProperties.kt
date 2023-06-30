/*
 * Copyright 2020-2023 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.experimental.uikit.internal

import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.jetbrains.compose.internal.utils.findProperty
import org.jetbrains.compose.internal.utils.toBooleanProvider

internal object IosGradleProperties {
    const val SYNC_RESOURCES_PROPERTY = "org.jetbrains.compose.ios.resources.sync"

    fun syncResources(providers: ProviderFactory): Provider<Boolean> =
        providers.findProperty(SYNC_RESOURCES_PROPERTY).toBooleanProvider(true)
}