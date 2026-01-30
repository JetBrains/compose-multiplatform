plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    mavenLocal()
}

kotlin {
    js(IR) {
        nodejs {}
        browser() {}
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(compose.runtime)
                implementation(project(":lib"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            }
        }
    }
}

tasks.findByName("jsBrowserProductionWebpack")!!.mustRunAfter("jsDevelopmentExecutableCompileSync")
tasks.findByName("jsNodeDevelopmentRun")!!.mustRunAfter("jsProductionExecutableCompileSync")
