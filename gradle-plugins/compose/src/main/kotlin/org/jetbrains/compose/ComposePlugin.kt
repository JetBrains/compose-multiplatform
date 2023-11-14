/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("unused")

package org.jetbrains.compose

import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
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
import org.jetbrains.compose.experimental.internal.configureNativeCompilerCaching
import org.jetbrains.compose.experimental.uikit.internal.resources.configureSyncTask
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.mppExtOrNull
import org.jetbrains.compose.internal.service.ConfigurationProblemReporterService
import org.jetbrains.compose.internal.service.GradlePropertySnapshotService
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.resources.configureResourceGenerator
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

internal val composeVersion get() = ComposeBuildConfig.composeVersion

private fun initBuildServices(project: Project) {
    ConfigurationProblemReporterService.init(project)
    GradlePropertySnapshotService.init(project)
}

abstract class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        initBuildServices(project)

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
        project.configureNativeCompilerCaching()

        project.configureResourceGenerator()

        project.afterEvaluate {
            configureDesktop(project, desktopExtension)
            project.configureExperimental(composeExtension, experimentalExtension)
            project.plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
                val mppExt = project.mppExt
                project.configureExperimentalTargetsFlagsCheck(mppExt)
                project.configureSyncTask(mppExt)
            }

            project.tasks.withType(KotlinCompile::class.java).configureEach {
                it.kotlinOptions.apply {
                    freeCompilerArgs = freeCompilerArgs +
                            composeExtension.kotlinCompilerPluginArgs.get().flatMap { arg ->
                                listOf("-P", "plugin:androidx.compose.compiler.plugins.kotlin:$arg")
                            }
                }
            }

            disableSignatureClashCheck(project)
        }
    }

    private fun disableSignatureClashCheck(project: Project) {
        val hasAnyWebTarget = project.mppExtOrNull?.targets?.firstOrNull {
            it.platformType == KotlinPlatformType.js ||
                    it.platformType == KotlinPlatformType.wasm
        } != null
        if (hasAnyWebTarget) {
            // currently k/wasm compile task is covered by KotlinJsCompile type
            project.tasks.withType(KotlinJsCompile::class.java).configureEach {
                it.kotlinOptions.freeCompilerArgs += listOf(
                    "-Xklib-enable-signature-clash-checks=false",
                )
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
        val runtimeSaveable get() = composeDependency("org.jetbrains.compose.runtime:runtime-saveable")
        val ui get() = composeDependency("org.jetbrains.compose.ui:ui")
        @Deprecated("Use desktop.uiTestJUnit4", replaceWith = ReplaceWith("desktop.uiTestJUnit4"))
        @ExperimentalComposeLibrary
        val uiTestJUnit4 get() = composeDependency("org.jetbrains.compose.ui:ui-test-junit4")
        val uiTooling get() = composeDependency("org.jetbrains.compose.ui:ui-tooling")
        val uiUtil get() = composeDependency("org.jetbrains.compose.ui:ui-util")
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

        val uiTestJUnit4 get() = composeDependency("org.jetbrains.compose.ui:ui-test-junit4")

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
