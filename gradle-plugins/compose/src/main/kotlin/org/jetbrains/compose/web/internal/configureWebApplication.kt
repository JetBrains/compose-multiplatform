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
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.gradle.api.file.*
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.joinLowerCamelCase
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.web.tasks.registerWebCompatibilityTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.Executable
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import javax.inject.Inject

internal fun Project.configureWeb() {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        mppExt.targets.withType(KotlinJsIrTarget::class.java).all { target ->
            target.configureSkikoWebRuntime()
            target.configureComposeUiTestExecutableCheck()
        }
        registerWebCompatibilityTask(mppExt)
    }
}

private fun KotlinJsIrTarget.configureSkikoWebRuntime() {
    val target = this

    target.compilations.all { compilation ->
        val runtimeDepsConfig = project.configurations.findByName(compilation.runtimeDependencyConfigurationName)!!
        val skikoWebRuntimeJarFiles = project.skikoWebRuntimeJarFiles(runtimeDepsConfig)

        val qualifierName = if (compilation.name == KotlinCompilation.MAIN_COMPILATION_NAME) "" else compilation.name
        val buildDirPath = "compose/skiko-${target.name}-${if(qualifierName.isNotEmpty()) "$qualifierName-" else ""}runtime"
        val unpackedRuntimeDir = project.layout.buildDirectory.dir(buildDirPath)

        val unpackRuntime = project.registerTask<UnpackSkikoRuntimeTask>(
            joinLowerCamelCase("unpack", qualifierName, "skikoRuntimeFor", target.name)
        ) {
            runtimeFiles.from(skikoWebRuntimeJarFiles)
            outputDirectory.set(unpackedRuntimeDir)
        }

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

@CacheableTask
internal abstract class UnpackSkikoRuntimeTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun unpack() {
        fileSystemOperations.copy {
            it.from(runtimeFiles.files.map(archiveOperations::zipTree))
            it.into(outputDirectory)
            it.duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}

private fun Project.skikoWebRuntimeJarFiles(runtimeDepsConfig: Configuration) =
    runtimeDepsConfig.incoming.artifactView { view ->
        @Suppress("UnstableApiUsage")
        view.withVariantReselection()

        view.componentFilter { component ->
            component is ModuleComponentIdentifier && component.group == SKIKO_GROUP
        }

        view.attributes { attributes ->
            runtimeDepsConfig.attributes.keySet().forEach { key ->
                @Suppress("UNCHECKED_CAST")
                attributes.attribute(
                    key as Attribute<Any>,
                    runtimeDepsConfig.attributes.getAttribute(key) as Any
                )
            }
            attributes.attribute(
                Usage.USAGE_ATTRIBUTE,
                objects.named(Usage::class.java, "skiko-runtime")
            )
        }
    }.files

private fun KotlinJsIrTarget.configureComposeUiTestExecutableCheck() {
    val target = this
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