/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.desktop.application.tasks

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings
import org.jetbrains.compose.desktop.application.internal.validation.validate
import org.jetbrains.compose.desktop.tasks.AbstractComposeDesktopTask
import org.jetbrains.compose.internal.utils.nullableProperty

abstract class AbstractNotarizationTask : AbstractComposeDesktopTask() {
    @get:Nested
    @get:Optional
    internal var nonValidatedNotarizationSettings: MacOSNotarizationSettings? = null

    internal fun validateNotarization() =
        nonValidatedNotarizationSettings.validate()
}
