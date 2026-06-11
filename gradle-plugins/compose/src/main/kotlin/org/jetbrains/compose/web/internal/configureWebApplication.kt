/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web.internal

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.UnresolvedDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.language.jvm.tasks.ProcessResources
import org.jetbrains.compose.ComposeBuildConfig
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.internal.utils.detachedComposeDependency
import org.jetbrains.compose.internal.utils.file
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.compose.web.WebExtension
import org.jetbrains.compose.web.tasks.UnpackSkikoWasmRuntimeTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.testing.karma.KotlinKarma
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

            listOf(compileConfiguration, runtimeConfiguration).mapNotNull { name ->
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

    val targets = webExt.targetsToConfigure(project)

    // configure only if there is k/wasm or k/js target:
    if (targets.isNotEmpty()) {
        configureWebApplication(targets, project, shouldRunUnpackSkiko)
    }
}

internal fun configureWebApplication(
    targets: Collection<KotlinJsIrTarget>,
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

    val unpackedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-for-web-runtime")
    val processedRuntimeDir = project.layout.buildDirectory.dir("compose/skiko-runtime-processed-wasmjs")
    val taskName = "unpackSkikoWasmRuntime"

    val unpackRuntime = project.registerTask<UnpackSkikoWasmRuntimeTask>(taskName) {
        onlyIf {
            shouldRunUnpackSkiko.get()
        }

        skikoRuntimeFiles = skikoJsWasmRuntimeConfiguration
        outputDir.set(unpackedRuntimeDir)
    }

    val processSkikoRuntimeForKWasm = project.registerTask<Copy>("processSkikoRuntimeForKWasm") {
        dependsOn(unpackRuntime)
        from(unpackedRuntimeDir)
        into(processedRuntimeDir)
    }

    targets.forEach { target ->
        target.compilations.all { compilation ->
            // `wasmTargetType` is available starting with kotlin 1.9.2x
            if (target.wasmTargetType != null) {
                // Kotlin/Wasm uses ES module system to depend on skiko through skiko.mjs.
                // Further bundler could process all files by its own (both skiko.mjs and skiko.wasm) and then emits its own version.
                // So that’s why we need to provide skiko.mjs and skiko.wasm only for webpack, but not in the final dist.
                compilation.binaries.all {
                    it.linkSyncTask.configure {
                        it.dependsOn(processSkikoRuntimeForKWasm)
                        it.from.from(processedRuntimeDir)
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

                if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
                    configureJsBrowserTestsSkikoLoading(
                        project = project,
                        target = target,
                        compilationProcessResourcesTaskName = compilation.processResourcesTaskName
                    )
                }
            }
        }
    }
}


/**
 * Configures Karma test runner for Kotlin/JS browser tests to properly load Skiko runtime dependencies.
 * 
 * This function generates a custom Karma configuration file that:
 * - Locates the test entry point JavaScript file in the build output
 * - Ensures Skiko runtime files (skiko.mjs, skiko.wasm, js-reexport-symbols.mjs) are served by Karma
 * - Creates a loader script that intercepts Karma's test execution to wait for Skiko initialization
 * - Hooks into the window.__karma__.loaded() function to ensure Skiko is ready before tests run
 * 
 * The generated configuration ensures that Compose UI tests that depend on Skiko can properly
 * initialize the graphics runtime before test execution begins, preventing race conditions
 * where tests might run before Skiko's WebAssembly module is fully loaded.
 *
 * @param project The Gradle project being configured
 * @param target The Kotlin/JS IR target being configured for testing
 * @param compilationProcessResourcesTaskName The name of the task that processes resources for the test compilation,
 *        used to ensure Skiko resources are available before tests run
 */
private fun configureJsBrowserTestsSkikoLoading(
    project: Project,
    target: KotlinJsIrTarget,
    compilationProcessResourcesTaskName: String
) {
    val targetName = target.name.replaceFirstChar { it.titlecase() }
    val configDir = project.layout.buildDirectory.dir("compose/karma-config/$targetName")
    val configFile = configDir.map { it.file("compose-skiko-runtime.js") }

    val generateConfigTask = project.registerTask<DefaultTask>("generateTestComposeSkikoKarmaConfigFor$targetName") {
        outputs.file(configFile)
        doLast {
            val file = configFile.get().asFile
            file.parentFile.mkdirs()
            file.writeText(
                //language=JavaScript
                """
                const fs = require("fs");
                const path = require("path");
                
                (function(config) {
                  const files = config.files || [];
                  const testEntry = files.find((entry) =>
                    typeof entry === "string" &&
                    entry.endsWith(".js") &&
                    entry.includes(path.sep + "kotlin" + path.sep)
                  );
                  if (!testEntry) return;
                
                  const reexportModule = path.resolve(path.dirname(testEntry), "js-reexport-symbols.mjs");
                  const skikoModule = path.resolve(path.dirname(testEntry), "skiko.mjs");
                  const skikoWasm = path.resolve(path.dirname(testEntry), "skiko.wasm");
                  const loaderFile = path.resolve(path.dirname(testEntry), "compose-skiko-loader.js");
                  if (!fs.existsSync(reexportModule)) return;
                
                  const ensureServed = (filePath) => {
                    if (!fs.existsSync(filePath)) return;
                    const exists = files.some((entry) =>
                      entry === filePath ||
                      (entry && typeof entry === "object" && entry.pattern === filePath)
                    );
                    if (!exists) {
                      files.push({
                        pattern: filePath,
                        watched: false,
                        included: false,
                        served: true,
                      });
                    }
                  };
                  ensureServed(reexportModule);
                  ensureServed(skikoModule);
                  ensureServed(skikoWasm);

                  fs.writeFileSync(loaderFile, `
                  (function() {
                    if (!window.__karma__) return;
                    const originalLoaded = window.__karma__.loaded.bind(window.__karma__);
                    let skikoReady = null;
                    window.__karma__.loaded = function() {
                      if (!skikoReady) {
                        skikoReady = import("/base/kotlin/js-reexport-symbols.mjs")
                          .then((mod) => mod?.api?.awaitSkiko || Promise.resolve());
                      }
                      skikoReady.then(() => originalLoaded()).catch((error) => {
                        const message = error && error.stack ? error.stack : String(error);
                        window.__karma__.error(message);
                      });
                    };
                  })();
                  `.trim());

                  const hasLoader = files.some((entry) =>
                    entry === loaderFile ||
                    (entry && typeof entry === "object" && entry.pattern === loaderFile)
                  );
                  if (!hasLoader) {
                    files.unshift(loaderFile);
                  }
                })(config);
                """.trimIndent()
            )
        }
    }

    project.tasks.withType(KotlinJsTest::class.java).configureEach { testTask ->
        if (testTask.compilation.target != target ||
            testTask.compilation.compilationName != KotlinCompilation.TEST_COMPILATION_NAME
        ) {
            return@configureEach
        }

        testTask.dependsOn(generateConfigTask)
        testTask.dependsOn(compilationProcessResourcesTaskName)

        val configDirectoryPath = configDir.get().asFile
        (testTask.testFramework as? KotlinKarma)?.useConfigDirectory(configDirectoryPath)
        testTask.onTestFrameworkSet { framework ->
            (framework as? KotlinKarma)?.useConfigDirectory(configDirectoryPath)
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
