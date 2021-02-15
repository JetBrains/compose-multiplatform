package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import org.jetbrains.compose.desktop.application.internal.validation.validate

abstract class AbstractNotarizationTask : AbstractComposeDesktopTask() {
    @get:Input
    @get:Optional
    internal val nonValidatedBundleID: Property<String?> = objects.nullableProperty()

    @get:Nested
    @get:Optional
    internal var nonValidatedNotarizationSettings: MacOSNotarizationSettings? = null

    internal fun validateNotarization() =
        nonValidatedNotarizationSettings.validate(nonValidatedBundleID)
}