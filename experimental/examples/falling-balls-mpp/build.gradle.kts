import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")

//        mavenLocal()
        maven("https://packages.jetbrains.team/maven/p/karpovich-sandbox/ksandbox")
    }

    configurations.all {
        val conf = this
        conf.resolutionStrategy.eachDependency {
            val it = this
            val isWasm = conf.name.contains("wasm", true)
            if (isWasm &&
                it.requested.module.group == "org.jetbrains.kotlinx" &&
                it.requested.module.name.contains("atomicfu", true)) {
                it.useVersion("0.18.5-wasm0")
            }
            if (it.requested.module.group == "org.jetbrains.skiko") {
                // skiko-with-wasm is not published for k/native because of kotlin 1.9.0-dev-1
                if (!isWasm) useVersion("0.7.50")
            }
            if (it.requested.module.group == "org.jetbrains.kotlinx" &&
                it.requested.module.name.contains("kotlinx-coroutines", true)) {
                if (!isWasm) useVersion("1.6.4")
            }
        }
    }

    // With decoys disabled, we have IdSignature clashes,
    // so disable signature-clash-checks (decoys are going to be removed in the future )
    tasks.withType<KotlinJsCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += listOf(
            "-Xklib-enable-signature-clash-checks=false",
        )
    }
}

