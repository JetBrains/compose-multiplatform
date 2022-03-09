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
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.desktop.application.internal.configureDesktop
import org.jetbrains.compose.desktop.application.internal.currentTarget
import org.jetbrains.compose.desktop.preview.internal.initializePreview
import org.jetbrains.compose.experimental.dsl.ExperimentalExtension
import org.jetbrains.compose.experimental.internal.configureExperimental
import org.jetbrains.compose.internal.COMPOSE_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_JS_PLUGIN_ID
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal val composeVersion get() = ComposeBuildConfig.composeVersion

class ComposePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val composeExtension = project.extensions.create("compose", ComposeExtension::class.java)
        val desktopExtension = composeExtension.extensions.create("desktop", DesktopExtension::class.java)
        val androidExtension = composeExtension.extensions.create("android", AndroidExtension::class.java)
        val experimentalExtension = composeExtension.extensions.create("experimental", ExperimentalExtension::class.java)

        if (!project.buildFile.endsWith(".gradle.kts")) {
            setUpGroovyDslExtensions(project)
        }

        project.initializePreview()
        composeExtension.extensions.create("web", WebExtension::class.java)

        project.plugins.apply(ComposeCompilerKotlinSupportPlugin::class.java)

        project.afterEvaluate {
            configureDesktop(project, desktopExtension)
            project.configureExperimental(composeExtension, experimentalExtension)

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

            //redirecting all android artifacts to androidx.compose
            project.dependencies.modules {
                if (!androidExtension.useAndroidX && !ComposeBuildConfig.experimentalOELPublication) {
                    // Replace 'androidx.compose' artifacts by 'org.jetbrains.compose' artifacts.
                    // It is needed, because 'org.jetbrains.compose' artifacts are the same artifacts as 'androidx.compose'
                    // (but with different version).
                    // And Gradle will throw an error when it cannot determine which class from which artifact should it use.
                    //
                    // Note that we don't provide a configuration parameter to disable dependency replacement,
                    // because without replacement, gradle will fail anyway because classpath contains two incompatible artifacts.
                    //
                    // We should define all replacements, even for transitive dependencies.
                    // For example, a library can depend on androidx.compose.foundation:foundation-layout
                    //
                    // List of all org.jetbrains.compose libraries is here:
                    // https://maven.pkg.jetbrains.space/public/p/compose/dev/org/jetbrains/compose/
                    //
                    // (use ./gradle printAllAndroidxReplacements to know what dependencies should be here)
                    //
                    // It is temporarily solution until we will be publishing all MPP artifacts in Google Maven repository.
                    // Or align versions with androidx artifacts and point MPP-android artifacts to androidx artifacts (is it possible?)

                    listOf(
                        "androidx.compose.animation:animation",
                        "androidx.compose.animation:animation-core",
                        "androidx.compose.animation:animation-graphics",
                        "androidx.compose.compiler:compiler",
                        "androidx.compose.compiler:compiler-hosted",
                        "androidx.compose.foundation:foundation",
                        "androidx.compose.foundation:foundation-layout",
                        "androidx.compose.material:material",
                        "androidx.compose.material:material-icons-core",
                        "androidx.compose.material:material-icons-extended",
                        "androidx.compose.material:material-ripple",
                        "androidx.compose.material:material3",
                        "androidx.compose.runtime:runtime",
                        "androidx.compose.runtime:runtime-saveable",
                        "androidx.compose.ui:ui",
                        "androidx.compose.ui:ui-geometry",
                        "androidx.compose.ui:ui-graphics",
                        "androidx.compose.ui:ui-test",
                        "androidx.compose.ui:ui-test-junit4",
                        "androidx.compose.ui:ui-text",
                        "androidx.compose.ui:ui-unit",
                        "androidx.compose.ui:ui-util"
                    ).forEach() { module ->
                        it.replaceAndroidx(
                            module,
                            module.replace("androidx.compose", "org.jetbrains.compose")
                        )
                    }
                }
            }

            val overrideDefaultJvmTarget = ComposeProperties.overrideKotlinJvmTarget(project.providers).get()
            project.tasks.withType(KotlinCompile::class.java) {
                it.kotlinOptions.apply {
                    if (overrideDefaultJvmTarget) {
                        jvmTarget = "11".takeIf { jvmTarget.toDouble() < 11 } ?: jvmTarget
                    }
                    useIR = true
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

    object Dependencies {
        val desktop = DesktopDependencies
        val animation get() = composeDependency("org.jetbrains.compose.animation:animation")
        val animationGraphics get() = composeDependency("org.jetbrains.compose.animation:animation-graphics")
        val foundation get() = composeDependency("org.jetbrains.compose.foundation:foundation")
        val material get() = composeDependency("org.jetbrains.compose.material:material")
        @ExperimentalComposeLibrary
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

    object DesktopComponentsDependencies {
        @ExperimentalComposeLibrary
        val splitPane = composeDependency("org.jetbrains.compose.components:components-splitpane")
    }

    object WebDependencies {
        val core by lazy {
            composeDependency("org.jetbrains.compose.web:web-core")
        }

        val svg by lazy {
            composeDependency("org.jetbrains.compose.web:web-svg")
        }

        @Deprecated("compose.web.web-widgets API is deprecated")
        val widgets by lazy {
            composeDependency("org.jetbrains.compose.web:web-widgets")
        }

        val testUtils by lazy {
            composeDependency("org.jetbrains.compose.web:test-utils")
        }
    }
}

fun RepositoryHandler.jetbrainsCompose(): MavenArtifactRepository =
    maven { repo -> repo.setUrl("https://maven.pkg.jetbrains.space/public/p/compose/dev") }

fun KotlinDependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val KotlinDependencyHandler.compose get() = ComposePlugin.Dependencies

fun DependencyHandler.compose(groupWithArtifact: String) = composeDependency(groupWithArtifact)
val DependencyHandler.compose get() = ComposePlugin.Dependencies

private fun composeDependency(groupWithArtifact: String) = "$groupWithArtifact:$composeVersion"

private fun setUpGroovyDslExtensions(project: Project) {
    // add compose extension for Groovy DSL to work
    project.dependencies.extensions.add("compose", ComposePlugin.Dependencies)
    project.plugins.withId("org.jetbrains.kotlin.multiplatform") {
        (project.extensions.getByName("kotlin") as? ExtensionAware)?.apply {
            extensions.add("compose", ComposePlugin.Dependencies)
        }
    }
    (project.repositories as? ExtensionAware)?.extensions?.apply {
        add("jetbrainsCompose", object : Closure<MavenArtifactRepository>(project.repositories) {
            fun doCall(): MavenArtifactRepository =
                project.repositories.jetbrainsCompose()
        })
    }
}
