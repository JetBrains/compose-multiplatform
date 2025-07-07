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

        project.checkComposeCompilerPlugin()

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
        val animation = composeDependency("org.jetbrains.compose.animation:animation")
        val animationGraphics = composeDependency("org.jetbrains.compose.animation:animation-graphics")
        val foundation = composeDependency("org.jetbrains.compose.foundation:foundation")
        val material = composeDependency("org.jetbrains.compose.material:material")
        val material3 = composeDependency("org.jetbrains.compose.material3:material3")
        val material3AdaptiveNavigationSuite = composeDependency("org.jetbrains.compose.material3:material3-adaptive-navigation-suite")
        val runtime = composeDependency("org.jetbrains.compose.runtime:runtime")
        val runtimeSaveable = composeDependency("org.jetbrains.compose.runtime:runtime-saveable")
        val ui = composeDependency("org.jetbrains.compose.ui:ui")

        @Deprecated("Use desktop.uiTestJUnit4", replaceWith = ReplaceWith("desktop.uiTestJUnit4"))
        @ExperimentalComposeLibrary
        val uiTestJUnit4 = composeDependency("org.jetbrains.compose.ui:ui-test-junit4")

        @ExperimentalComposeLibrary
        val uiTest = composeDependency("org.jetbrains.compose.ui:ui-test")
        val uiTooling = composeDependency("org.jetbrains.compose.ui:ui-tooling")
        val uiUtil = composeDependency("org.jetbrains.compose.ui:ui-util")
        val preview = composeDependency("org.jetbrains.compose.ui:ui-tooling-preview")
        val materialIconsExtended = "org.jetbrains.compose.material:material-icons-extended:1.7.3"
        val components = CommonComponentsDependencies

        @Deprecated("Use compose.html", replaceWith = ReplaceWith("html"))
        val web: WebDependencies = WebDependencies
        val html: HtmlDependencies = HtmlDependencies
    }

    object DesktopDependencies {
        val components = DesktopComponentsDependencies

        val common = composeDependency("org.jetbrains.compose.desktop:desktop")
        val linux_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-x64")
        val linux_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-arm64")
        val windows_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows-x64")
        val windows_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows-arm64")
        val macos_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-x64")
        val macos_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")

        val uiTestJUnit4 = composeDependency("org.jetbrains.compose.ui:ui-test-junit4")

        val currentOs by lazy {
            composeDependency("org.jetbrains.compose.desktop:desktop-jvm-${currentTarget.id}")
        }
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
        val core = composeDependency("org.jetbrains.compose.html:html-core")

        val svg = composeDependency("org.jetbrains.compose.html:html-svg")

        val testUtils = composeDependency("org.jetbrains.compose.html:html-test-utils")
    }

    object HtmlDependencies {
        val core = composeDependency("org.jetbrains.compose.html:html-core")

        val svg = composeDependency("org.jetbrains.compose.html:html-svg")

        val testUtils = composeDependency("org.jetbrains.compose.html:html-test-utils")
    }
}

fun RepositoryHandler.jetbrainsCompose(): MavenArtifactRepository =
    maven { repo -> repo.setUrl("https://maven.pkg.jetbrains.space/public/p/compose/dev") }

fun KotlinDependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)

fun DependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)

private fun composeDependency(groupWithArtifact: String) = "$groupWithArtifact:${ComposeBuildConfig.composeVersion}"

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
