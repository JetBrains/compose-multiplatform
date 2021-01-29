package org.jetbrains.compose.desktop.application.internal

import org.gradle.api.provider.Property
import org.jetbrains.compose.desktop.application.dsl.MacOSNotarizationSettings

internal fun validateNotarizationSettings(
    settingsProperty: Property<MacOSNotarizationSettings?>
): MacOSNotarizationSettings {
    val value = settingsProperty.orNull
    checkNotNull(value) {
        """Specify notarization settings using DSL: 
            |  nativeExecutables.macOS {
            |     notarization {
            |       appleId = "<apple_id>"
            |       password = "<password>"
            |     }
            |  }
        """.trimMargin()
    }
    return value
}
