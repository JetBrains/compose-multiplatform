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

        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
        maven("https://packages.jetbrains.team/maven/p/karpovich-sandbox/ksandbox")
    }

    configurations.all {
        val conf = this
        conf.resolutionStrategy.eachDependency {
            val it = this
            if (it.requested.module.name.contains("material-icons-extended")) {
                // it's a large module that takes long to build for all targets,
                // so use an older published version for now
                useVersion("1.3.0-dev-wasm-01")
            }
        }
    }
}
