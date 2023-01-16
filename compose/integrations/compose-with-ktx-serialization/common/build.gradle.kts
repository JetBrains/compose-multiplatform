import org.jetbrains.compose.compose

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization") version "1.8.0"
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

    sourceSets {
        val commonMain by getting {
            dependencies {
                val ktxSerializationVer = project.property("kotlinx.serializationCore")
                api(compose.runtime)
                api("org.jetbrains.kotlinx:kotlinx-serialization-core:$ktxSerializationVer")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

compose.kotlinCompilerPlugin.set("1.4.0-alpha03")