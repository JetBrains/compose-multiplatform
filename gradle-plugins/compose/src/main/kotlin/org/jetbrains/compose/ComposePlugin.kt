/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.compose

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.dsl.ComponentModuleMetadataHandler
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.jetbrains.compose.android.AndroidExtension
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.configureDesktop
import org.jetbrains.compose.desktop.preview.internal.initializePreview
import org.jetbrains.compose.experimental.dsl.ExperimentalExtension
import org.jetbrains.compose.experimental.internal.checkExperimentalTargetsWithSkikoIsEnabled
import org.jetbrains.compose.experimental.internal.configureExperimental
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

internal val composeVersion get() = ComposeBuildConfig.composeVersion

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("compose", ComposeExtension::class.java, project)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)
        val androidExtension = composeExtension.extensions.create("android", AndroidExtension::class.java)
        val experimentalExtension = composeExtension.extensions.create("experimental", ExperimentalExtension::class.java)

        project.dependencies.extensions.add("compose", Dependencies(project))

        if (!project.buildFile.endsWith(".gradle.kts")) {
            setUpGroovyDslExtensions(project)
        }

        project.initializePreview(desktopExtension)
        composeExtension.extensions.create("web", WebExtension::class.java)

        project.plugins.apply(ComposeCompilerKotlinSupportPlugin::class.java)

        project.afterEvaluate {
            configureDesktop(project, desktopExtension)
            project.configureExperimental(composeExtension, experimentalExtension)
            project.checkExperimentalTargetsWithSkikoIsEnabled()

            if (androidExtension.useAndroidX) {
                project.logger.warn("useAndroidX is an experimental feature at the moment!")
                RedirectAndroidVariants.androidxVersion = androidExtension.androidxVersion
                listOf(
                    RedirectAndroidVariants::class.java,
                ).forEach(project.dependencies.components::all)
            }

            fun ComponentModuleMetadataHandler.replaceAndroidx(original: String, replacement: String) {
                module(original) {
                    it.replacedBy(replacement, "org.jetbrains.compose isn't compatible with androidx.compose, because it is the same library published with different maven coordinates")
                }
            }

            project.tasks.withType(KotlinCompile::class.java).configureEach {
                it.kotlinOptions.apply {
                    freeCompilerArgs = freeCompilerArgs +
                            composeExtension.kotlinCompilerPluginArgs.get().flatMap { arg ->
                                listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:$arg")
                            }
                }
            }
        }
    }

    class RedirectAndroidVariants : ComponentMetadataRule {
        override fun execute(context: ComponentMetadataContext) = with(context.details) {
            if (id.group.startsWith("org.jetbrains.compose")) {
                val group = id.group.replaceFirst("org.jetbrains.compose", "androidx.compose")
                val newReference = "$group:${id.module.name}:$androidxVersion"
                listOf(
                    "debugApiElements-published",
                    "debugRuntimeElements-published",
                    "releaseApiElements-published",
                    "releaseRuntimeElements-published"
                ).forEach { variantNameToAlter ->
                    withVariant(variantNameToAlter) { variantMetadata ->
                        variantMetadata.withDependencies { dependencies ->
                            dependencies.removeAll { true } //there are references to org.jetbrains artifacts now
                            dependencies.add(newReference)
                        }
                    }
                }
            }
        }

        companion object {
            var androidxVersion: String? = null
        }
    }

    class Dependencies(project: Project) {
        val desktop = DesktopDependencies
        val compiler = CompilerDependencies(project)
        val animation get() = composeDependency("org.jetbrains.compose.animation:animation")
        val animationGraphics get() = composeDependency("org.jetbrains.compose.animation:animation-graphics")
        val foundation get() = composeDependency("org.jetbrains.compose.foundation:foundation")
        val material get() = composeDependency("org.jetbrains.compose.material:material")
        val material3 get() = composeDependency("org.jetbrains.compose.material3:material3")
        val runtime get() = composeDependency("org.jetbrains.compose.runtime:runtime")
        val ui get() = composeDependency("org.jetbrains.compose.ui:ui")
        @ExperimentalComposeLibrary
        val uiTestJUnit4 get() = composeDependency("org.jetbrains.compose.ui:ui-test-junit4")
        val uiTooling get() = composeDependency("org.jetbrains.compose.ui:ui-tooling")
        val preview get() = composeDependency("org.jetbrains.compose.ui:ui-tooling-preview")
        val materialIconsExtended get() = composeDependency("org.jetbrains.compose.material:material-icons-extended")
        val web: WebDependencies get() = WebDependencies
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

    class CompilerDependencies(private val project: Project) {
        fun forKotlin(version: String) = "org.jetbrains.compose.compiler:compiler:" +
                ComposeCompilerCompatibility.compilerVersionFor(version)

        /**
         * Compose Compiler that is chosen by the version of Kotlin applied to the Gradle project
         */
        val auto get() = forKotlin(project.getKotlinPluginVersion())
    }

    object DesktopComponentsDependencies {
        @ExperimentalComposeLibrary
        val splitPane = composeDependency("org.jetbrains.compose.components:components-splitpane")

        @ExperimentalComposeLibrary
        val animatedImage = composeDependency("org.jetbrains.compose.components:components-animatedimage")
    }

    object WebDependencies {
        val core by lazy {
            composeDependency("org.jetbrains.compose.web:web-core")
        }

        val svg by lazy {
            composeDependency("org.jetbrains.compose.web:web-svg")
        }

        val testUtils by lazy {
            composeDependency("org.jetbrains.compose.web:test-utils")
        }
    }
}

fun RepositoryHandler.jetbrainsCompose(): MavenArtifactRepository =
    maven { repo -> repo.setUrl("https://maven.pkg.jetbrains.space/public/p/compose/dev") }

fun KotlinDependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)

fun DependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)

private fun composeDependency(groupWithArtifact: String) = "$groupWithArtifact:$composeVersion"

private fun setUpGroovyDslExtensions(project: Project) {
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
        (project.extensions.getByName("kotlin") as? ExtensionAware)?.apply {
            extensions.add("compose", ComposePlugin.Dependencies(project))
        }
    }
    (project.repositories as? ExtensionAware)?.extensions?.apply {
        add("jetbrainsCompose", object : Closure<MavenArtifactRepository>(project.repositories) {
            fun doCall(): MavenArtifactRepository =
                project.repositories.jetbrainsCompose()
        })
    }
}
