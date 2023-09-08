plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

val copyResources = tasks.create("copyWasmResourcesWorkaround", Copy::class.java) {
    from(project(":resources:demo:shared").file("src/commonMain/resources"))
    into("build/processedResources/wasmJs/main")
}

afterEvaluate {
    project.tasks.getByName("wasmJsProcessResources").finalizedBy(copyResources)
}

kotlin {
    wasm {
        moduleName = "myapp"
        browser()
        binaries.executable()
    }
    sourceSets {
        val wasmJsMain by getting  {
            dependencies {
                implementation(compose.ui)
                implementation(project(":resources:demo:shared"))
            }
        }
    }
}

compose.experimental {
    web.application {}
}

//project.configurations.forEach { conf ->
//    conf.resolutionStrategy.eachDependency {
//        if (requested.module.name.contains("kotlin-stdlib")) {
//            useVersion("1.8.20-Beta")
//        }
//    }
//}
