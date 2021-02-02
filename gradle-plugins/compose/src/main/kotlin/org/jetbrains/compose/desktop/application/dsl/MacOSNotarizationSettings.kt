package org.jetbrains.compose.desktop.application.dsl

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.nullableProperty
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
}