import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
    }
    js(IR) {
        browser()
        nodejs()
    }
    ios()
    macosX64()
    macosArm64()
    mingwX64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                val ktxSerializationVer = project.property("kotlinx.serializationCore")
                implementation(compose.runtime)
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$ktxSerializationVer")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose {
    val compilerPluginVersion = project.properties["compose.kotlinCompilerPluginVersion"] as? String
    if (!compilerPluginVersion.isNullOrEmpty()) {
        println("using compilerPluginVersion = $compilerPluginVersion")
        kotlinCompilerPlugin.set(compilerPluginVersion)
    }
}
