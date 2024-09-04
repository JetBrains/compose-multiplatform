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
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.compose.resources.configureComposeResources
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.compose.web.internal.configureWeb
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

internal val composeVersion get() = ComposeBuildConfig.composeVersion

abstract class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("compose", ComposeExtension::class.java, project)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)
        val androidExtension = composeExtension.extensions.create("android", AndroidExtension::class.java)
        val experimentalExtension = composeExtension.extensions.create("experimental", ExperimentalExtension::class.java)
        val resourcesExtension = composeExtension.extensions.create("resources", ResourcesExtension::class.java)

        project.dependencies.extensions.add("compose", Dependencies(project))

        if (!project.buildFile.endsWith(".gradle.kts")) {
            setUpGroovyDslExtensions(project)
        }

        project.initializePreview(desktopExtension)
        composeExtension.extensions.create("web", WebExtension::class.java)

        project.configureComposeCompilerPlugin()

        project.configureComposeResources(resourcesExtension)

        project.afterEvaluate {
            configureDesktop(project, desktopExtension)
            project.configureWeb(composeExtension)
            project.plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
                val mppExt = project.mppExt
                project.configureExperimentalTargetsFlagsCheck(mppExt)
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
        @ExperimentalComposeLibrary
        val uiTest get() = composeDependency("org.jetbrains.compose.ui:ui-test")
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
        val resources = composeDependency("org.jetbrains.compose.components:components-resources")
        val uiToolingPreview = composeDependency("org.jetbrains.compose.components:components-ui-tooling-preview")
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
