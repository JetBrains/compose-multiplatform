package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.process.ExecOperations
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.internal.nullableProperty
import org.jetbrains.compose.desktop.application.internal.validation.validate
import javax.inject.Inject

abstract class AbstractNotarizationTask(
) : DefaultTask() {
    @get:Inject
    protected abstract val objects: ObjectFactory
    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Input
    @get:Optional
    internal val nonValidatedBundleID: Property<String?> = objects.nullableProperty()

    @get:Nested
    @get:Optional
    internal var nonValidatedNotarizationSettings: MacOSNotarizationSettings? = null

    internal fun validateNotarization() =
        nonValidatedNotarizationSettings.validate(nonValidatedBundleID)
}