plugins {
    kotlin("multiplatform")
    alias(libs.plugins.compose.compiler)
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":common"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
