package org.jetbrains.compose.internal

import org.gradle.api.Project
import org.gradle.configurationcache.extensions.serviceOf
import org.gradle.internal.logging.text.StyledTextOutput
import org.gradle.internal.logging.text.StyledTextOutputFactory

internal fun Project.checkAndWarnAboutComposeWithSerialization() {
    afterEvaluate {
        val usesKotlinxSerialization = configurations.names.asSequence().filter {
            it.startsWith("kotlinCompilerPluginClasspath")
        }.any { configurationName ->
            configurations.getByName(configurationName).dependencies.any { dependency ->
                dependency.name.contains("kotlin-serialization")
            }
        }

        if (usesKotlinxSerialization) {
            val out = serviceOf<StyledTextOutputFactory>().create("COMPOSE_PLUGIN")
            out.style(StyledTextOutput.Style.FailureHeader)
                .text("WARNING! Both Compose and kotlinx.serialization plugins are used in the module '${project.name}'")
                .println()
                .style(StyledTextOutput.Style.Failure)
                .text("Consider using these plugins in separate modules to avoid compilation errors")
                .println()
        }
    }
}
