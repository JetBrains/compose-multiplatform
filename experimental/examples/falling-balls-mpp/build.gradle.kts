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
        mavenLocal()
    }

    configurations.all {
        val conf = this
        conf.resolutionStrategy.eachDependency {
            val it = this
            if (requested.group.contains("org.jetbrains.compose")) {
                useVersion("23.1.27")
            }
            val replace = (it.requested.module.name.contains("coroutines") ||
                    it.requested.module.name.contains("atomicfu")) &&
                    conf.name.contains("wasm", true)
            if (replace && !it.requested.version!!.contains("-wasm0")) {
                it.useVersion(it.requested.version!! + "-wasm0")
            }
        }
    }
}
