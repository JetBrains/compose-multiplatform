/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.configureApplicationImpl
import org.jetbrains.compose.desktop.application.internal.currentTarget
import org.jetbrains.compose.desktop.preview.internal.initializePreview
import org.jetbrains.compose.web.internal.initializeWeb
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val composeVersion get() = ComposeBuildConfig.composeVersion

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

        project.initializePreview()
        if (ComposeBuildConfig.isComposeWithWeb) {
            project.initializeWeb(composeExtension)
        }

        project.plugins.apply(ComposeCompilerKotlinSupportPlugin::class.java)

        project.afterEvaluate {
            if (desktopExtension._isApplicationInitialized) {
                // If application object was not accessed in a script,
                // we want to avoid creating tasks like package, run, etc. to avoid conflicts with other plugins
                configureApplicationImpl(project, desktopExtension.application)
            }
        }

        fun ComponentModuleMetadataHandler.replaceAndroidx(original: String, replacement: String) {
            module(original) {
                it.replacedBy(replacement, "org.jetbrains.compose isn't compatible with androidx.compose, because it is the same library published with different maven coordinates")
            }
        }

        project.dependencies.modules {
            // Replace 'androidx.compose' artifacts by 'org.jetbrains.compose' artifacts.
            // It is needed, because 'org.jetbrains.compose' artifacts are the same artifacts as 'androidx.compose'
            // (but with different version).
            // And Gradle will throw an error when it cannot determine which class from which artifact should it use.
            //
            // Note that we don't provide a configuration parameter to disable dependency replacement,
            // because without replacement, gradle will fail anyway because classpath contains two incompatible artifacts.
            //
            // We should define all replacements, even for transient dependencies.
            // For example, a library can depend on androidx.compose.foundation:foundation-layout
            //
            // List of all org.jetbrains.compose libraries is here:
            // https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/
            //
            // (use ./gradle printAllAndroidxReplacements to know what dependencies should be here)
            //
            // It is temporarily solution until we will be publishing all MPP artifacts in Google Maven repository.
            // Or align versions with androidx artifacts and point MPP-android artifacts to androidx artifacts (is it possible?)

            it.replaceAndroidx("androidx.compose.animation:animation", "org.jetbrains.compose.animation:animation")
            it.replaceAndroidx("androidx.compose.animation:animation-core", "org.jetbrains.compose.animation:animation-core")
            it.replaceAndroidx("androidx.compose.compiler:compiler", "org.jetbrains.compose.compiler:compiler")
            it.replaceAndroidx("androidx.compose.compiler:compiler-hosted", "org.jetbrains.compose.compiler:compiler-hosted")
            it.replaceAndroidx("androidx.compose.foundation:foundation", "org.jetbrains.compose.foundation:foundation")
            it.replaceAndroidx("androidx.compose.foundation:foundation-layout", "org.jetbrains.compose.foundation:foundation-layout")
            it.replaceAndroidx("androidx.compose.material:material", "org.jetbrains.compose.material:material")
            it.replaceAndroidx("androidx.compose.material:material-icons-core", "org.jetbrains.compose.material:material-icons-core")
            it.replaceAndroidx("androidx.compose.material:material-icons-extended", "org.jetbrains.compose.material:material-icons-extended")
            it.replaceAndroidx("androidx.compose.material:material-ripple", "org.jetbrains.compose.material:material-ripple")
            it.replaceAndroidx("androidx.compose.runtime:runtime", "org.jetbrains.compose.runtime:runtime")
            it.replaceAndroidx("androidx.compose.runtime:runtime-saveable", "org.jetbrains.compose.runtime:runtime-saveable")
            it.replaceAndroidx("androidx.compose.ui:ui", "org.jetbrains.compose.ui:ui")
            it.replaceAndroidx("androidx.compose.ui:ui-geometry", "org.jetbrains.compose.ui:ui-geometry")
            it.replaceAndroidx("androidx.compose.ui:ui-graphics", "org.jetbrains.compose.ui:ui-graphics")
            it.replaceAndroidx("androidx.compose.ui:ui-test", "org.jetbrains.compose.ui:ui-test")
            it.replaceAndroidx("androidx.compose.ui:ui-test-junit4", "org.jetbrains.compose.ui:ui-test-junit4")
            it.replaceAndroidx("androidx.compose.ui:ui-text", "org.jetbrains.compose.ui:ui-text")
            it.replaceAndroidx("androidx.compose.ui:ui-unit", "org.jetbrains.compose.ui:ui-unit")
            it.replaceAndroidx("androidx.compose.ui:ui-util", "org.jetbrains.compose.ui:ui-util")
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
        val uiTooling get() = composeDependency("org.jetbrains.compose.ui:ui-tooling")
        val materialIconsExtended get() = composeDependency("org.jetbrains.compose.material:material-icons-extended")
        val web: WebDependencies get() =
            if (ComposeBuildConfig.isComposeWithWeb) WebDependencies
            else error("This version of Compose plugin does not support 'compose.web.*' dependencies")
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
            "compose.desktop.macos is deprecated, use compose.desktop.macos_x64 instead",
            replaceWith = ReplaceWith("macos_x64")
        )
        val macos = macos_x64

        val currentOs by lazy {
            composeDependency("org.jetbrains.compose.desktop:desktop-jvm-${currentTarget.id}")
        }
    }

    object WebDependencies {
        val core by lazy {
            composeDependency("org.jetbrains.compose.web:web-core")
        }

        val widgets by lazy {
            composeDependency("org.jetbrains.compose.web:web-widgets")
        }
    }
}

fun KotlinDependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val KotlinDependencyHandler.compose get() = ComposePlugin.Dependencies

fun DependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val DependencyHandler.compose get() = ComposePlugin.Dependencies

private fun composeDependency(groupWithArtifact: String) = "$groupWithArtifact:$composeVersion"
