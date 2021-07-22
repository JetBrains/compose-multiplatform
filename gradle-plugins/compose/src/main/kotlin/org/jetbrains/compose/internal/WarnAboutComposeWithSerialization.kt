package org.jetbrains.compose.internal

import org.gradle.api.Project

internal fun Project.checkAndWarnAboutComposeWithSerialization() {
    project.plugins.withId("org.jetbrains.kotlin.plugin.serialization") {
        val warningMessage = """

            >>> COMPOSE WARNING
            >>> Project `${project.name}` has `compose` and `kotlinx.serialization` plugins applied!
            >>> Consider using these plugins in separate modules to avoid compilation errors
            >>> Check more details here: https://github.com/JetBrains/compose-jb/issues/738

        """.trimIndent()

        logger.warn(warningMessage)
    }
}
