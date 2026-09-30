import org.gradle.api.tasks.Sync
import java.io.File
import kotlin.text.replace

val jsJodaEsmFile = rootProject.layout.buildDirectory.file(
    "wasm/node_modules/@js-joda/core/dist/js-joda.esm.js"
)
val wasmResourcesDirectory = project.layout.buildDirectory.dir("processedResources/wasmJs/main")
val skikoRuntimeDirectory = project.layout.buildDirectory.dir(
    "compose/skiko-runtime-processed-wasmjs"
)

fun File.replaceRequired(oldValue: String, newValue: String) {
    check(isFile) { "Expected D8 runtime file does not exist: $this" }

    val original = readText()
    check(oldValue in original) {
        "Cannot prepare the D8 runtime: expected content was not found in $this"
    }
    writeText(original.replace(oldValue, newValue))
}

fun patchD8Runtime(directory: File) {
    directory.resolve("compose-benchmarks-benchmarks.import-object.mjs").apply {
        replaceRequired("from './skiko.mjs'", "from './skikod8.mjs'")
        replaceRequired("from '@js-joda/core'", "from './js-joda.esm.js'")
    }

    directory.resolve("skikod8.mjs").replaceRequired(
        oldValue = """
            export const awaitSkiko = loadSkikoWASM().then((module) => {
                loadedWasm._ = module.wasmExports;
                skikoGl = module.GL;
                return module
            });
        """.trimIndent(),
        newValue = """
            const _skikoWasmPath = String(import.meta.url).replace(/^file:\/\//, '').replace(/\/[^\/]+${'$'}/, '/skiko.wasm');
            const _skikoWasmBytes = read(_skikoWasmPath, 'binary');
            const _skikoWasmModule = loadSkikoWASM({
                instantiateWasm(info, receiveInstance) {
                    const instance = new WebAssembly.Instance(
                        new WebAssembly.Module(
                            _skikoWasmBytes instanceof Uint8Array
                                ? _skikoWasmBytes
                                : new Uint8Array(_skikoWasmBytes)
                        ),
                        info
                    );
                    receiveInstance(instance);
                    return instance.exports;
                }
            });
            export const awaitSkiko = _skikoWasmModule.then((module) => {
                loadedWasm._ = module.wasmExports;
                skikoGl = module.GL;
                return module
            });
        """.trimIndent()
    )
}

fun registerD8Runtime(variant: String, compiledPath: String, outputPath: String) =
    tasks.register<Sync>("prepareWasmJsD8${variant}Runtime") {
        dependsOn("wasmJs${variant}ExecutableCompileSync", ":kotlinWasmNpmInstall")
        from(project.layout.buildDirectory.dir(compiledPath))
        from(wasmResourcesDirectory)
        from(skikoRuntimeDirectory)
        from(jsJodaEsmFile)
        into(project.layout.buildDirectory.dir(outputPath))
        doLast {
            patchD8Runtime(destinationDir)
        }
    }

val d8DevelopmentDirectory = project.layout.buildDirectory.dir("d8/development")
val d8ProductionDirectory = project.layout.buildDirectory.dir("d8/production")
val prepareWasmJsD8DevelopmentRuntime = registerD8Runtime(
    variant = "Development",
    compiledPath = "compileSync/wasmJs/main/developmentExecutable/kotlin",
    outputPath = "d8/development"
)
val prepareWasmJsD8ProductionRuntime = registerD8Runtime(
    variant = "Production",
    compiledPath = "compileSync/wasmJs/main/productionExecutable/optimized",
    outputPath = "d8/production"
)

tasks.register("buildD8Distribution", org.gradle.api.tasks.bundling.Zip::class.java) {
    dependsOn(prepareWasmJsD8ProductionRuntime)
    from(d8ProductionDirectory)
    archiveFileName.set("d8-distribution.zip")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))
}
