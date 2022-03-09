/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.notNullProperty
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import javax.inject.Inject

abstract class MacOSSigningSettings {
    @get:Inject
    protected abstract val objects: ObjectFactory
    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Input
    val sign: Property<Boolean> = objects.notNullProperty<Boolean>().apply {
        set(
            ComposeProperties.macSign(providers)
                .orElse(false)
        )
    }
    @get:Input
    @get:Optional
    val identity: Property<String?> = objects.nullableProperty<String>().apply {
        set(ComposeProperties.macSignIdentity(providers))
    }
    @get:Input
    @get:Optional
    val keychain: Property<String?> = objects.nullableProperty<String>().apply {
        set(ComposeProperties.macSignKeychain(providers))
    }
    @get:Input
    @get:Optional
    val prefix: Property<String?> = objects.nullableProperty<String>().apply {
        set(ComposeProperties.macSignPrefix(providers))
    }
}