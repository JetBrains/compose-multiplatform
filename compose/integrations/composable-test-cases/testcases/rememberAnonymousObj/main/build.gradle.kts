plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    configureTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(getCommonLib())
                implementation(getLibDependencyForMain())
            }
        }
        val commonTest by getting {
            configureCommonTestDependencies()
        }
    }
}
