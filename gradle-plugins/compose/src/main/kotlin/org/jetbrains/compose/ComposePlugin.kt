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
import org.jetbrains.compose.desktop.DesktopExtension
import org.jetbrains.compose.desktop.application.internal.configureDesktop
import org.jetbrains.compose.desktop.preview.internal.initializePreview
import org.jetbrains.compose.experimental.internal.configureExperimentalTargetsFlagsCheck
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.compose.resources.configureComposeResources
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.compose.web.internal.configureWeb
import org.jetbrains.compose.web.tasks.configureWebCompatibility
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

internal val composeVersion get() = ComposeBuildConfig.composeVersion
internal val composeMaterial3Version get() = ComposeBuildConfig.composeMaterial3Version

abstract class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("compose", ComposeExtension::class.java, project)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)
        val resourcesExtension = composeExtension.extensions.create("resources", ResourcesExtension::class.java)

        project.dependencies.extensions.add("compose", Dependencies(project))

        if (!project.buildFile.endsWith(".gradle.kts")) {
            setUpGroovyDslExtensions(project)
        }

        project.initializePreview(desktopExtension)
        composeExtension.extensions.create("web", WebExtension::class.java)

        project.checkComposeCompilerPlugin()

        project.configureComposeResources(resourcesExtension)

        project.configureWebCompatibility()

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
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.animation:animation:${ComposeBuildConfig.composeVersion}\""))
        val animation get() = composeDependency("org.jetbrains.compose.animation:animation")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.animation:animation-graphics:${ComposeBuildConfig.composeVersion}\""))
        val animationGraphics get() = composeDependency("org.jetbrains.compose.animation:animation-graphics")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.foundation:foundation:${ComposeBuildConfig.composeVersion}\""))
        val foundation get() = composeDependency("org.jetbrains.compose.foundation:foundation")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.material:material:${ComposeBuildConfig.composeVersion}\""))
        val material get() = composeDependency("org.jetbrains.compose.material:material")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.material3:material3:${ComposeBuildConfig.composeMaterial3Version}\""))
        val material3 get() = composeMaterial3Dependency("org.jetbrains.compose.material3:material3")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.material3:material3-adaptive-navigation-suite:${ComposeBuildConfig.composeMaterial3Version}\""))
        val material3AdaptiveNavigationSuite get() = composeMaterial3Dependency("org.jetbrains.compose.material3:material3-adaptive-navigation-suite")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.runtime:runtime:${ComposeBuildConfig.composeVersion}\""))
        val runtime get() = composeDependency("org.jetbrains.compose.runtime:runtime")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.runtime:runtime-saveable:${ComposeBuildConfig.composeVersion}\""))
        val runtimeSaveable get() = composeDependency("org.jetbrains.compose.runtime:runtime-saveable")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui:${ComposeBuildConfig.composeVersion}\""))
        val ui get() = composeDependency("org.jetbrains.compose.ui:ui")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui-test:${ComposeBuildConfig.composeVersion}\""))
        @ExperimentalComposeLibrary
        val uiTest get() = composeDependency("org.jetbrains.compose.ui:ui-test")
        @Deprecated("Use org.jetbrains.compose.ui:ui-tooling-preview module instead", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui-tooling:${ComposeBuildConfig.composeVersion}\""))
        val uiTooling get() = composeDependency("org.jetbrains.compose.ui:ui-tooling")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui-util:${ComposeBuildConfig.composeVersion}\""))
        val uiUtil get() = composeDependency("org.jetbrains.compose.ui:ui-util")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui-tooling-preview:${ComposeBuildConfig.composeVersion}\""))
        val preview get() = composeDependency("org.jetbrains.compose.ui:ui-tooling-preview")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.material:material-icons-extended:1.7.3\""))
        val materialIconsExtended get() = "org.jetbrains.compose.material:material-icons-extended:1.7.3"
        @Deprecated("Specify dependency via version catalog")
        val components get() = CommonComponentsDependencies
        @Deprecated("Use compose.html", replaceWith = ReplaceWith("html"), level = DeprecationLevel.ERROR)
        val web: WebDependencies get() = WebDependencies
        @Deprecated("Specify dependency via version catalog")
        val html: HtmlDependencies get() = HtmlDependencies
    }

    @Deprecated("Specify dependency via version catalog")
    object DesktopDependencies {
        @Deprecated("Specify dependency via version catalog")
        val components = DesktopComponentsDependencies

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop:${ComposeBuildConfig.composeVersion}\""))
        val common = composeDependency("org.jetbrains.compose.desktop:desktop")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop-jvm-linux-x64:${ComposeBuildConfig.composeVersion}\""))
        val linux_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-x64")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop-jvm-linux-arm64:${ComposeBuildConfig.composeVersion}\""))
        val linux_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-linux-arm64")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop-jvm-windows-x64:${ComposeBuildConfig.composeVersion}\""))
        val windows_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows-x64")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop-jvm-windows-arm64:${ComposeBuildConfig.composeVersion}\""))
        val windows_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-windows-arm64")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop-jvm-macos-x64:${ComposeBuildConfig.composeVersion}\""))
        val macos_x64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-x64")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.desktop:desktop-jvm-macos-arm64:${ComposeBuildConfig.composeVersion}\""))
        val macos_arm64 = composeDependency("org.jetbrains.compose.desktop:desktop-jvm-macos-arm64")

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui-test-junit4:${ComposeBuildConfig.composeVersion}\""))
        val uiTestJUnit4 get() = composeDependency("org.jetbrains.compose.ui:ui-test-junit4")

        val currentOs by lazy {
            composeDependency("org.jetbrains.compose.desktop:desktop-jvm-${currentTarget.id}")
        }
    }

    @Deprecated("Specify dependency via version catalog")
    object CommonComponentsDependencies {
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.components:components-resources:${ComposeBuildConfig.composeVersion}\""))
        val resources = composeDependency("org.jetbrains.compose.components:components-resources")
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.ui:ui-tooling-preview:${ComposeBuildConfig.composeVersion}\""))
        val uiToolingPreview = composeDependency("org.jetbrains.compose.components:components-ui-tooling-preview")
    }

    @Deprecated("Specify dependency via version catalog")
    object DesktopComponentsDependencies {
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.components:components-splitpane:${ComposeBuildConfig.composeVersion}\""))
        @ExperimentalComposeLibrary
        val splitPane = composeDependency("org.jetbrains.compose.components:components-splitpane")

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.components:components-animatedimage:${ComposeBuildConfig.composeVersion}\""))
        @ExperimentalComposeLibrary
        val animatedImage = composeDependency("org.jetbrains.compose.components:components-animatedimage")
    }

    @Deprecated("Use compose.html")
    object WebDependencies {
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.html:html-core:${ComposeBuildConfig.composeVersion}\""))
        val core by lazy {
            composeDependency("org.jetbrains.compose.html:html-core")
        }

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.html:html-svg:${ComposeBuildConfig.composeVersion}\""))
        val svg by lazy {
            composeDependency("org.jetbrains.compose.html:html-svg")
        }

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.html:html-test-utils:${ComposeBuildConfig.composeVersion}\""))
        val testUtils by lazy {
            composeDependency("org.jetbrains.compose.html:html-test-utils")
        }
    }

    @Deprecated("Specify dependency via version catalog")
    object HtmlDependencies {
        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.html:html-core:${ComposeBuildConfig.composeVersion}\""))
        val core by lazy {
            composeDependency("org.jetbrains.compose.html:html-core")
        }

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.html:html-svg:${ComposeBuildConfig.composeVersion}\""))
        val svg by lazy {
            composeDependency("org.jetbrains.compose.html:html-svg")
        }

        @Deprecated("Specify dependency via version catalog", replaceWith = ReplaceWith("\"org.jetbrains.compose.html:html-test-utils:${ComposeBuildConfig.composeVersion}\""))
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
private fun composeMaterial3Dependency(groupWithArtifact: String) = "$groupWithArtifact:$composeMaterial3Version"

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
