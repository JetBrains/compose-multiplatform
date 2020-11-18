@file:Suppress("unused")

package org.jetbrains.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.configureApplicationImpl
import org.jetbrains.compose.desktop.application.internal.currentTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val composeVersion get() = ComposeBuildConfig.composeVersion

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("compose", ComposeExtension::class.java)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)

        if (!project.buildFile.endsWith(".gradle.kts")) {
            // add compose extension for Groovy DSL to work
            project.dependencies.extensions.add("compose", Dependencies)
            project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
                (project.extensions.getByName("kotlin") as? ExtensionAware)?.apply {
                    extensions.add("compose", Dependencies)
                }
            }
        }

        project.afterEvaluate {
            if (desktopExtension._isApplicationInitialized) {
                // If application object was not accessed in a script,
                // we want to avoid creating tasks like package, run, etc. to avoid conflicts with other plugins
                configureApplicationImpl(project, desktopExtension.application)
            }

            project.dependencies.add(
                    "kotlinCompilerPluginClasspath",
                    "org.jetbrains.compose.compiler:compiler:$composeVersion"
            )
        }

        project.tasks.withType(KotlinCompile::class.java) {
            it.kotlinOptions.apply {
                jvmTarget = "1.8".takeIf { jvmTarget.toDouble() < 1.8 } ?: jvmTarget
                useIR = true
            }
        }
    }

    object Dependencies {
        val desktop = DesktopDependencies
        val animation get() = composeDependency("org.jetbrains.compose.animation:animation")
        val foundation get() = composeDependency("org.jetbrains.compose.foundation:foundation")
        val material get() = composeDependency("org.jetbrains.compose.material:material")
        val runtime get() = composeDependency("org.jetbrains.compose.runtime:runtime")
        val ui get() = composeDependency("org.jetbrains.compose.ui:ui")
        val materialIconsExtended get() = composeDependency("org.jetbrains.compose.material:material-icons-extended")
    }

    object DesktopDependencies {
        val common = composeDependency("org.jetbrains.compose.desktop:desktop")
        val linux_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-x64")
        val windows_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows-x64")
        val macos_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-x64")
        val macos_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")

        @Deprecated(
            "compose.desktop.linux is deprecated, use compose.desktop.linux_x64 instead",
            replaceWith = ReplaceWith("linux_x64")
        )
        val linux = linux_x64
        @Deprecated(
            "compose.desktop.windows is deprecated, use compose.desktop.windows_x64 instead",
            replaceWith = ReplaceWith("windows_x64")
        )
        val windows = windows_x64
        @Deprecated(
            "compose.desktop.macos is deprecated, use compose.desktop.macosX64 instead",
            replaceWith = ReplaceWith("macos_x64")
        )
        val macos = macos_x64

        val currentOs by lazy {
            composeDependency("org.jetbrains.compose.desktop:desktop-jvm-${currentTarget.id}")
        }
    }
}

fun KotlinDependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val KotlinDependencyHandler.compose get() = ComposePlugin.Dependencies

fun DependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val DependencyHandler.compose get() = ComposePlugin.Dependencies

private fun composeDependency(groupWithArtifact: String) = "$groupWithArtifact:$composeVersion"