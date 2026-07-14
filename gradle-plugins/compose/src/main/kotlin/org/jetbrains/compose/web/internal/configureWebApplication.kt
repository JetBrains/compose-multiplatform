/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.UnresolvedDependency
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.Executable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

internal fun Project.configureWeb(
    composeExt: ComposeExtension,
) {
    val webExt = composeExt.extensions.getByType(WebExtension::class.java)

    val targets = webExt.targetsToConfigure(project)
    targets.forEach { target ->
        configureSkikoWebRuntime(project, target)
        configureComposeUiTestExecutableCheck(project, target)
    }
}

private fun configureSkikoWebRuntime(
    project: Project,
    target: KotlinJsIrTarget,
) {
    val titledTargetName = target.name.replaceFirstChar { it.titlecase() }
    val mainCompilation = target.compilations.findByName(KotlinCompilation.MAIN_COMPILATION_NAME)!!
    val runtimeDepsConfig = project.configurations.findByName(mainCompilation.runtimeDependencyConfigurationName)!!
    val skikoWebRuntimeJarFiles = runtimeDepsConfig.incoming.artifactView { act ->
        @Suppress("UnstableApiUsage")
        act.withVariantReselection()
        act.attributes { cont ->
            runtimeDepsConfig.attributes.keySet().forEach {
                @Suppress("UNCHECKED_CAST")
                cont.attribute(it as Attribute<Any>, runtimeDepsConfig.attributes.getAttribute(it) as Any)
            }
            cont.attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, "skiko-runtime"))
        }
    }.files
    val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-${target.name}-runtime")

    val unpackRuntime = project.registerTask<Copy>("unpackSkikoRuntimeFor$titledTargetName") {
        destinationDir = project.file(unpackedRuntimeDir)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(
            skikoWebRuntimeJarFiles.map { artifact -> project.zipTree(artifact) }
        )
    }

    target.compilations.all { compilation ->
        if (target.wasmTargetType != null) {
            // Kotlin/Wasm uses ES module system to depend on skiko through skiko.mjs.
            // Further bundler could process all files by its own (both skiko.mjs and skiko.wasm) and then emits its own version.
            // So that’s why we need to provide skiko.mjs and skiko.wasm only for webpack, but not in the final dist.
            compilation.binaries.all {
                it.linkSyncTask.configure {
                    it.dependsOn(unpackRuntime)
                    it.from.from(unpackedRuntimeDir)
                }
            }
        } else {
            // Kotlin/JS depends on Skiko through global space.
            // Bundler cannot know anything about global externals, so that’s why we need to copy it to final dist
            project.tasks.named(compilation.processResourcesTaskName, ProcessResources::class.java) {
                it.from(unpackedRuntimeDir)
                it.dependsOn(unpackRuntime)
                it.exclude("META-INF")
            }
        }
    }
}

private fun configureComposeUiTestExecutableCheck(
    project: Project,
    target: KotlinJsIrTarget,
) {
    val titledTargetName = target.name.replaceFirstChar { it.titlecase() }
    val checkTask = project.registerTask<CheckComposeUiTestExecutableTask>(
        "checkComposeUiTestConfigurationFor$titledTargetName"
    ) {
        targetName.set(target.name)
        // Computed lazily, after all `afterEvaluate`s: `binaries.executable()` may be declared
        // after this plugin runs, so the binaries set can still be empty here. Reading these
        // through providers (instead of in the task action) also keeps the task free of
        // Project/target references, so it stays compatible with the configuration cache.
        testDependsOnSkiko.set(project.provider { project.testCompilationDependsOnSkiko(target) })
        hasExecutableBinary.set(
            project.provider { target.binaries.withType(Executable::class.java).isNotEmpty() }
        )
    }

    project.tasks.withType(KotlinJsTest::class.java).configureEach { testTask ->
        val compilation = testTask.compilation
        // Browser test tasks (Karma) are named "<target>BrowserTest"; node tests don't run Compose UI.
        if (compilation.target == target &&
            compilation.compilationName == KotlinCompilation.TEST_COMPILATION_NAME &&
            testTask.name.endsWith("BrowserTest")
        ) {
            testTask.dependsOn(checkTask)
        }
    }
}

/**
 * Compose UI browser tests must be bundled by webpack to load the Skiko runtime, which only
 * happens when the target declares an executable `binaries.executable()`. When a target that
 * depends on Skiko has no executable, this task fails with an actionable message instead of
 * letting the tests fail in a confusing way.
 */
@DisableCachingByDefault(because = "Not worth caching: only validates the configuration")
internal abstract class CheckComposeUiTestExecutableTask : DefaultTask() {
    @get:Input
    abstract val targetName: Property<String>

    @get:Input
    abstract val testDependsOnSkiko: Property<Boolean>

    @get:Input
    abstract val hasExecutableBinary: Property<Boolean>

    @TaskAction
    fun check() {
        if (!hasExecutableBinary.get() && testDependsOnSkiko.get()) {
            val target = targetName.get()
            throw GradleException(
                "Compose UI tests for the '$target' target are not bundled with webpack: " +
                        "no executable binary is declared, so the Skiko runtime required by Compose UI " +
                        "cannot be loaded and the tests may fail. Add `binaries.executable()` to the " +
                        "'$target' target. See https://youtrack.jetbrains.com/issue/CMP-4906"
            )
        }
    }
}

private fun Project.testCompilationDependsOnSkiko(target: KotlinJsIrTarget): Boolean {
    val testCompilation = target.compilations.findByName(KotlinCompilation.TEST_COMPILATION_NAME)
        ?: return false
    return listOf(
        testCompilation.compileDependencyConfigurationName, testCompilation.runtimeDependencyConfigurationName
    ).mapNotNull { name ->
        configurations.findByName(name)
    }.any { configuration ->
        configuration.allDependenciesDescriptors.any(::isSkikoDependency)
    }
}

private const val SKIKO_GROUP = "org.jetbrains.skiko"

private fun isSkikoDependency(dep: DependencyDescriptor): Boolean =
    dep.group == SKIKO_GROUP && dep.version != null

private val Configuration.allDependenciesDescriptors: Sequence<DependencyDescriptor>
    get() = with(resolvedConfiguration.lenientConfiguration) {
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