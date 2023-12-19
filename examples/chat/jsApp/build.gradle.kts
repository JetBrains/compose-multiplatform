plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting  {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.ui)
                implementation(compose.foundation)
            }
        }
    }
}

compose.experimental {
    web.application {}
}

compose.kotlinCompilerPlugin.set(project.properties["compose.compiler.version"] as String)
