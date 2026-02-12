import kotlinx.html.link

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("com.varabyte.kobweb.application") version libs.versions.kobweb.get()
}

group = "com.adrianwitaszak.ballast.web"

kotlin {
    js(IR) {
        moduleName = project.name
        browser {
            commonWebpackConfig {
                outputFileName = "$moduleName.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        named("jsMain") {
            dependencies {
                implementation(project(":feature:router"))
                implementation(project(":feature:counter"))
                implementation(project(":feature:home"))
                implementation(libs.kobweb.core)
                implementation(libs.kobweb.silk)
                implementation(libs.ballast.core)
                implementation(libs.ballast.navigation)
            }
        }
    }
}

kobweb {
    app {
        index {
            description.set("Ballast Web UI")
        }
    }
}
