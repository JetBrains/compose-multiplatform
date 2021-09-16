/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.compose

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.configureApplicationImpl
import org.jetbrains.compose.desktop.application.internal.currentTarget
import org.jetbrains.compose.desktop.preview.internal.initializePreview
import org.jetbrains.compose.internal.checkAndWarnAboutComposeWithSerialization
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

        listOf(
            AddAndroidxDependencyToOrgJetbrainsStubVariants::class.java,
            AddDesktopComposeDependencyToAndroidxStubVariants::class.java
        ).forEach(project.dependencies.components::all)

        project.tasks.withType(KotlinCompile::class.java) {
            it.kotlinOptions.apply {
                jvmTarget = "1.8".takeIf { jvmTarget.toDouble() < 1.8 } ?: jvmTarget
                useIR = true
            }
        }

        project.checkAndWarnAboutComposeWithSerialization()
    }

    internal abstract class AddAndroidxDependencyToOrgJetbrainsStubVariants :
        AddSyntheticDependencyToChosenVariants(
            listOf(
                "debugApiElements-published",
                "debugRuntimeElements-published",
                "releaseApiElements-published",
                "releaseRuntimeElements-published",
                "metadataApiElements"
            ), { moduleId ->
                if (moduleId.group.startsWith("org.jetbrains.compose")) {
                    val group = moduleId.group.replaceFirst("org.jetbrains.compose", "androidx.compose")
                    val version = ComposeBuildConfig.androidxComposeVersion
                    "$group:${moduleId.module.name}:$version"
                } else null
            })

    internal abstract class AddDesktopComposeDependencyToAndroidxStubVariants :
        AddSyntheticDependencyToChosenVariants(
            listOf("desktopApiElements-published", "desktopRuntimeElements-published"), { moduleId ->
                if (moduleId.group.startsWith("androidx.compose")) {
                    val group = moduleId.group.replaceFirst("androidx.compose", "org.jetbrains.compose")
                    val version = ComposeBuildConfig.composeVersion
                    "$group:${moduleId.module.name}:$version"
                } else null
            }
        )

    internal abstract class AddSyntheticDependencyToChosenVariants(
        private val variantNamesToAlter: Iterable<String>,
        private val syntheticDependencyProvider: (ModuleVersionIdentifier) -> String?
    ) : ComponentMetadataRule {
        override fun execute(context: ComponentMetadataContext) = with(context.details) {
            val syntheticDependencyToAdd = syntheticDependencyProvider(id)
            if (syntheticDependencyToAdd != null)
                variantNamesToAlter.forEach { variantNameToAlter ->
                    withVariant(variantNameToAlter) { variantMetadata ->
                        variantMetadata.withDependencies { dependencies ->
                            dependencies.add(syntheticDependencyToAdd)
                        }
                    }
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
        val preview get() = composeDependency("org.jetbrains.compose.ui:ui-tooling-preview")
        val materialIconsExtended get() = composeDependency("org.jetbrains.compose.material:material-icons-extended")
        val web: WebDependencies get() =
            if (ComposeBuildConfig.isComposeWithWeb) WebDependencies
            else error("This version of Compose plugin does not support 'compose.web.*' dependencies")
    }

    object DesktopDependencies {
        val components = DesktopComponentsDependencies

        val common = composeDependency("org.jetbrains.compose.desktop:desktop")
        val linux_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-x64")
        val linux_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-arm64")
        val windows_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows-x64")
        val macos_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-x64")
        val macos_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")

        val currentOs by lazy {
            composeDependency("org.jetbrains.compose.desktop:desktop-jvm-${currentTarget.id}")
        }
    }

    object DesktopComponentsDependencies {
        val splitPane = composeDependency("org.jetbrains.compose.components:components-splitpane")
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
