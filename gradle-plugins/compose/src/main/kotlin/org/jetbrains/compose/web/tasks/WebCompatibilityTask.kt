package org.jetbrains.compose.web.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.compose.internal.KOTLIN_MPP_PLUGIN_ID
import org.jetbrains.compose.internal.mppExt
import org.jetbrains.compose.internal.utils.clearDirs
import org.jetbrains.compose.internal.utils.joinLowerCamelCase
import org.jetbrains.compose.internal.utils.registerTask
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.io.File
import javax.inject.Inject

abstract class WebCompatibilityTask : DefaultTask() {
    @get:Inject
    internal abstract val fileOperations: FileSystemOperations

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @get:InputFiles
    abstract val jsDistFiles: ConfigurableFileCollection

    @get:InputFiles
    abstract val wasmDistFiles: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val jsOutputName: Property<String>

    @get:Input
    @get:Optional
    abstract val wasmOutputName: Property<String>

    @TaskAction
    fun run() {
        val prefix = "origin"
        val jsAppFileName = jsOutputName.orNull ?: return
        val jsAppRenamed = joinLowerCamelCase(prefix, "js", jsAppFileName)
        val wasmAppFileName = wasmOutputName.orNull ?: return
        val wasmAppRenamed = joinLowerCamelCase(prefix, "wasm", wasmAppFileName)

        fileOperations.clearDirs(outputDir)

        fileOperations.copy { copySpec ->
            copySpec.duplicatesStrategy = DuplicatesStrategy.WARN

            copySpec.from(jsDistFiles) {
                it.rename { name ->
                    when (name) {
                        jsAppFileName -> jsAppRenamed
                        "${jsAppFileName}.map" -> "${jsAppRenamed}.map"
                        else -> name
                    }
                }
            }
            copySpec.from(wasmDistFiles) {
                it.rename { name ->
                    when (name) {
                        wasmAppFileName -> wasmAppRenamed
                        "${wasmAppFileName}.map" -> "${wasmAppRenamed}.map"
                        else -> name
                    }
                }
            }

            copySpec.into(outputDir)
        }

        val fallbackResolverCode = """
            const loadApp = () => {
                 const simpleWasmModule = new Uint8Array([
                    0,  97, 115, 109,   1,   0,   0,  0,   1,   8,   2,  95,
                    1, 120,   0,  96,   0,   0,   3,  3,   2,   1,   1,  10,
                   14,   2,   6,   0,   6,  64,  25, 11,  11,   5,   0, 208,
                  112,  26,  11,   0,  45,   4, 110, 97, 109, 101,   1,  15,
                    2,   0,   5, 102, 117, 110,  99, 48,   1,   5, 102, 117,
                  110,  99,  49,   4,   8,   1,   0,  5, 116, 121, 112, 101,
                   48,  10,  11,   1,   0,   1,   0,  6, 102, 105, 101, 108,
                  100,  48
                    ]);

                const hasSupportOfAllRequiredWasmFeatures = () =>
                    typeof WebAssembly !== "undefined" &&
                    typeof WebAssembly?.validate === "function" &&
                    WebAssembly.validate(simpleWasmModule);

                const createScript = (src) => {
                    const script = document.createElement("script");
                    script.src = src;
                    script.type = "application/javascript";
                    return script;
                }

                document.body.appendChild(createScript(hasSupportOfAllRequiredWasmFeatures() ? "$wasmAppRenamed" : "$jsAppRenamed"));
            }

            if (document.readyState === "loading") {
                document.addEventListener("DOMContentLoaded", loadApp);
            } else {
                loadApp();
            }
            """.trimIndent()


        val outputDir = outputDir.get().asFile
        File(outputDir, jsAppFileName).writeText(fallbackResolverCode)
        File(outputDir, wasmAppFileName).writeText(fallbackResolverCode)
    }
}

private fun Project.registerWebCompatibilityTask(mppPlugin: KotlinMultiplatformExtension) =
    registerTask<WebCompatibilityTask>("composeCompatibilityBrowserDistribution") {
        group = "compose"
        description =
            "This task combines both js and wasm distributions into one so that wasm application fallback to js target if modern wasm feature are not supported"

        val webProductionDist = layout.buildDirectory.dir("dist/composeWebCompatibility/productionExecutable")
        outputDir.set(webProductionDist)

        mppPlugin.targets.withType(KotlinJsIrTarget::class.java).configureEach { target ->
            if (target.platformType == KotlinPlatformType.wasm) {
                tasks.withType(KotlinWebpack::class.java).findByName("${target.name}BrowserProductionWebpack")?.let {
                    wasmOutputName.set(it.mainOutputFileName)
                }

                val taskDistributionName = "${target.name}BrowserDistribution"
                wasmDistFiles.from(provider {
                    if (tasks.names.contains(taskDistributionName)) {
                        tasks.getByName(taskDistributionName).outputs.files
                    } else {
                        emptyList()
                    }
                })
            } else if (target.platformType == KotlinPlatformType.js) {
                tasks.withType(KotlinWebpack::class.java).findByName("${target.name}BrowserProductionWebpack")?.let {
                    jsOutputName.set(it.mainOutputFileName)
                }

                val taskDistributionName = "${target.name}BrowserDistribution"
                jsDistFiles.from(provider {
                    if (tasks.names.contains(taskDistributionName)) {
                        tasks.getByName(taskDistributionName).outputs.files
                    } else {
                        emptyList()
                    }
                })
            }

            onlyIf {
                val hasBothDistributions = !jsDistFiles.isEmpty && !wasmDistFiles.isEmpty
                val hasBothOutputs = jsOutputName.orNull != null && wasmOutputName.orNull != null

                if (!hasBothDistributions) {
                    logger.lifecycle("Task ${this.name} skipped: no js and wasm distributions found, both are required for compatibility")
                } else if (!hasBothOutputs) {
                    logger.lifecycle("Task ${this.name} skipped: no js and wasm output names specified")
                }
                hasBothDistributions && hasBothOutputs
            }

        }
    }

internal fun Project.configureWebCompatibility() {
    plugins.withId(KOTLIN_MPP_PLUGIN_ID) {
        project.registerWebCompatibilityTask(mppExt)
    }
}
