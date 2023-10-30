/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.utils.nullableProperty
import javax.inject.Inject

abstract class MacOSNotarizationSettings {
    @get:Inject
    protected abstract val objects: ObjectFactory

    @get:Inject
    protected abstract val providers: ProviderFactory

    @get:Input
    @get:Optional
    val appleID: Property<String?> = objects.nullableProperty<String>().apply {
        set(ComposeProperties.macNotarizationAppleID(providers))
    }

    @get:Input
    @get:Optional
    val password: Property<String?> = objects.nullableProperty<String>().apply {
        set(ComposeProperties.macNotarizationPassword(providers))
    }

    @get:Input
    @get:Optional
    val teamID: Property<String?> = objects.nullableProperty<String>().apply {
        set(ComposeProperties.macNotarizationTeamID(providers))
    }

    @Deprecated("This option is no longer supported and got replaced by teamID", level = DeprecationLevel.ERROR)
    @get:Internal
    val ascProvider: Property<String?> = objects.nullableProperty<String>().apply {
        set(providers.provider {
            throw UnsupportedOperationException("This option is not supported by notary tool and was replaced by teamID")
        })
    }
}
