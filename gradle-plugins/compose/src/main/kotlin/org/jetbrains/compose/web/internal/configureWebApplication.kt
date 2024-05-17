/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.UnresolvedDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.Provider
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.web.tasks.UnpackSkikoWasmRuntimeTask
import org.jetbrains.compose.internal.utils.*
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal fun Project.configureWeb(
    composeExt: ComposeExtension,
) {
    val webExt = composeExt.extensions.getByType(WebExtension::class.java)

    // here we check all dependencies (including transitive)
    // If there is compose.ui, then skiko is required!
    val shouldRunUnpackSkiko = project.provider {
        webExt.targetsToConfigure(project).any { target ->
            val compilation = target.compilations.getByName("main")
            val compileConfiguration = compilation.compileDependencyConfigurationName
            val runtimeConfiguration = compilation.runtimeDependencyConfigurationName

            listOf(compileConfiguration, runtimeConfiguration).mapNotNull {  name ->
                project.configurations.findByName(name)
            }.flatMap { configuration ->
                configuration.incoming.resolutionResult.allComponents.map { it.id }
            }.any { identifier ->
                if (identifier is ModuleComponentIdentifier) {
                    identifier.group == "org.jetbrains.compose.ui" && identifier.module == "ui"
                } else {
                    false
                }
            }
        }
    }

    // configure only if there is k/wasm or k/js target:
    webExt.targetsToConfigure(project)
        .configureWebApplication(project, shouldRunUnpackSkiko)
}

internal fun Collection<KotlinJsIrTarget>.configureWebApplication(
    project: Project,
    shouldRunUnpackSkiko: Provider<Boolean>
) {
    val skikoJsWasmRuntimeConfiguration = project.configurations.create("COMPOSE_SKIKO_JS_WASM_RUNTIME")
    val skikoJsWasmRuntimeDependency = skikoVersionProvider(project).map { skikoVersion ->
        project.dependencies.create("org.jetbrains.skiko:skiko-js-wasm-runtime:$skikoVersion")
    }
    skikoJsWasmRuntimeConfiguration.defaultDependencies {
        it.addLater(skikoJsWasmRuntimeDependency)
    }
    forEach {
        val mainCompilation = it.compilations.getByName("main")
        val testCompilation = it.compilations.getByName("test")
        val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-wasm/${it.targetName}")
        val taskName = "unpackSkikoWasmRuntime${it.targetName.uppercaseFirstChar()}"
        mainCompilation.defaultSourceSet.resources.srcDir(unpackedRuntimeDir)
        testCompilation.defaultSourceSet.resources.srcDir(unpackedRuntimeDir)

        val unpackRuntime = project.registerTask<UnpackSkikoWasmRuntimeTask>(taskName) {
            onlyIf {
                shouldRunUnpackSkiko.get()
            }

            skikoRuntimeFiles = skikoJsWasmRuntimeConfiguration
            outputDir.set(unpackedRuntimeDir)
        }
        project.tasks.named(mainCompilation.processResourcesTaskName).configure { processResourcesTask ->
            processResourcesTask.dependsOn(unpackRuntime)
        }
        project.tasks.named(testCompilation.processResourcesTaskName).configure { processResourcesTask ->
            processResourcesTask.dependsOn(unpackRuntime)
        }
    }
}

private const val SKIKO_GROUP = "org.jetbrains.skiko"

private fun skikoVersionProvider(project: Project): Provider<String> {
    val composeVersion = ComposeBuildConfig.composeVersion
    val configurationWithSkiko = project.detachedComposeDependency(
        artifactId = "ui-graphics",
        groupId = "org.jetbrains.compose.ui"
    )
    return project.provider {
        val skikoDependency = configurationWithSkiko.allDependenciesDescriptors.firstOrNull(::isSkikoDependency)
        skikoDependency?.version
            ?: error("Cannot determine the version of Skiko for Compose '$composeVersion'")
    }
}

private fun isSkikoDependency(dep: DependencyDescriptor): Boolean =
    dep.group == SKIKO_GROUP && dep.version != null

private val Configuration.allDependenciesDescriptors: Sequence<DependencyDescriptor>
    get() = with (resolvedConfiguration.lenientConfiguration) {
        allModuleDependencies.asSequence().map { ResolvedDependencyDescriptor(it) } +
                unresolvedModuleDependencies.asSequence().map { UnresolvedDependencyDescriptor(it) }
    }

private abstract class DependencyDescriptor {
    abstract val group: String?
    abstract val name: String?
    abstract val version: String?
}

private class ResolvedDependencyDescriptor(private val dependency: ResolvedDependency) : DependencyDescriptor() {
    override val group: String?
        get() = dependency.moduleGroup

    override val name: String?
        get() = dependency.moduleName

    override val version: String?
        get() = dependency.moduleVersion
}

private class UnresolvedDependencyDescriptor(private val dependency: UnresolvedDependency) : DependencyDescriptor() {
    override val group: String?
        get() = dependency.selector.group

    override val name: String?
        get() = dependency.selector.name

    override val version: String?
        get() = dependency.selector.version
}