import internal.composeRuntimeDependency

plugins {
    kotlin("multiplatform")
}

group = "com.example"
version = "1.0-SNAPSHOT"

kotlin {
    configureTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project.composeRuntimeDependency)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
            }
        }
        val commonTest by getting {
            configureCommonTestDependencies()
        }
    }
}
