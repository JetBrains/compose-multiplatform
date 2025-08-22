import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    js(name = "webJs", IR) {
        outputModuleName.set("composeApp")
        browser {
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs(name = "webWasm") {
        outputModuleName.set("composeApp")
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
        }

        val webMain by creating {
            resources.setSrcDirs(resources.srcDirs)
            dependsOn(commonMain.get())
        }

        val webJsMain by getting {
            dependsOn(webMain)
        }

        val webWasmMain by getting {
            dependsOn(webMain)
        }
    }
}


val wasmRepack = tasks.register<RepackTask>("wasmRepack") {
    sourceFiles.from(project.tasks.named("webWasmBrowserDistribution").map { it.outputs.files })
    outputDir.set(project.layout.buildDirectory.dir("dist/repackedWasm"))
}

val jsRepack = tasks.register<RepackTask>("jsRepack") {
    sourceFiles.from(project.tasks.named("webJsBrowserDistribution").map { it.outputs.files })
    outputDir.set(project.layout.buildDirectory.dir("dist/repackedJs"))
}

project.tasks.withType<org.jetbrains.compose.web.tasks.WebCompatibilityTask>().configureEach {
    jsOutputName.set("repackedApp.js")
    wasmOutputName.set("repackedApp.js")
    jsDistFiles.setFrom(jsRepack)
    wasmDistFiles.setFrom(wasmRepack)
}

abstract class RepackTask : DefaultTask() {
    @get:Inject
    internal abstract val fileOperations: FileSystemOperations

    @get:InputFiles
    abstract val sourceFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        fileOperations.copy {
            from(sourceFiles) {
                this.rename { name ->
                    when (name) {
                        "composeApp.js" -> "repackedApp.js"
                        "composeApp.js.map" -> "repackedApp.js.map"
                        else -> name
                    }
                }
            }
            into(outputDir)
        }
    }
}
