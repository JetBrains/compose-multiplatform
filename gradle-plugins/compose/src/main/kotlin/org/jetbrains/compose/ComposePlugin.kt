@file:Suppress("unused")

package org.jetbrains.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.configureApplicationImpl
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val composeVersion get() = ComposeBuildConfig.composeVersion

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("compose", ComposeExtension::class.java)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)

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
        val linux = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux")
        val windows = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows")
        val macos = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos")
        val all = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-all")

        val currentOs by lazy {
            val os = System.getProperty("os.name")
            val artifactOs = when {
                os == "Mac OS X" -> "macos"
                os.startsWith("Win") -> "windows"
                os.startsWith("Linux") -> "linux"
                else -> throw Error("Unsupported OS $os")
            }
            composeDependency("org.jetbrains.compose.desktop:desktop-jvm-$artifactOs")
        }
    }
}

fun KotlinDependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val KotlinDependencyHandler.compose get() = ComposePlugin.Dependencies

fun DependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val DependencyHandler.compose get() = ComposePlugin.Dependencies

private fun composeDependency(groupWithArtifact: String) = "$groupWithArtifact:$composeVersion"