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
import org.jetbrains.compose.experimental.internal.configureExperimentalTargetsFlagsCheck
import org.jetbrains.compose.experimental.internal.configureExperimental
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

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
            project.configureExperimentalTargetsFlagsCheck()

            project.tasks.withType(KotlinCompile::class.java).configureEach {
                it.kotlinOptions.apply {
                    freeCompilerArgs = freeCompilerArgs +
                            composeExtension.kotlinCompilerPluginArgs.get().flatMap { arg ->
                                listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:$arg")
                            }
                }
            }

            project.tasks.withType(KotlinJsCompile::class.java).configureEach {
                it.kotlinOptions.apply {
                    freeCompilerArgs += "-Xklib-enable-signature-clash-checks=false"
                }
            }
        }

        project.configurations.all {
            // TODO: remove these HACKS for version substitution when possible
            val conf = it
            conf.resolutionStrategy.eachDependency {
                if (project.getKotlinPluginVersion() == "1.8.20-RC2") {
                    if (it.requested.module.name.contains("kotlin-stdlib")) {
                        it.useVersion("1.8.20-RC2")
                    }
                }
                val isWasm = conf.name.contains("wasm", true)

                if (it.requested.module.group == "org.jetbrains.kotlinx" &&
                    it.requested.module.name.contains("kotlinx-coroutines", true)
                ) {
                    if (isWasm) it.useVersion("1.7.0-Beta-wasm0")
                }

                if (it.requested.module.group == "org.jetbrains.kotlinx" &&
                    it.requested.module.name.contains("atomicfu", true)
                ) {
                    if (isWasm) it.useVersion("0.18.5-wasm0")
                }

                if (it.requested.module.group.startsWith("org.jetbrains.skiko")) {
                    // skiko 0.0.7.58-wasm01 is broken for k/wasm, but don't want to republish every lib:
                    if (it.requested.version == "0.0.7.58-wasm01") {
                        it.useVersion("0.0.7.58-wasm02")
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
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
        val components get() = CommonComponentsDependencies
        @Deprecated("Use compose.html", replaceWith = ReplaceWith("html"))
        val web: WebDependencies get() = WebDependencies
        val html: HtmlDependencies get() = HtmlDependencies
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

    object CommonComponentsDependencies {
        @ExperimentalComposeLibrary
        val resources = composeDependency("org.jetbrains.compose.components:components-resources")
    }

    object DesktopComponentsDependencies {
        @ExperimentalComposeLibrary
        val splitPane = composeDependency("org.jetbrains.compose.components:components-splitpane")

        @ExperimentalComposeLibrary
        val animatedImage = composeDependency("org.jetbrains.compose.components:components-animatedimage")
    }

    @Deprecated("Use compose.html")
    object WebDependencies {
        val core by lazy {
            composeDependency("org.jetbrains.compose.html:html-core")
        }

        val svg by lazy {
            composeDependency("org.jetbrains.compose.html:html-svg")
        }

        val testUtils by lazy {
            composeDependency("org.jetbrains.compose.html:html-test-utils")
        }
    }

    object HtmlDependencies {
        val core by lazy {
            composeDependency("org.jetbrains.compose.html:html-core")
        }

        val svg by lazy {
            composeDependency("org.jetbrains.compose.html:html-svg")
        }

        val testUtils by lazy {
            composeDependency("org.jetbrains.compose.html:html-test-utils")
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
