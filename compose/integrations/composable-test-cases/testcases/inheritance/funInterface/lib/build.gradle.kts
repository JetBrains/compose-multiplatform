plugins {
    kotlin("multiplatform")
    alias(libs.plugins.composeCompiler)
}

kotlin {
    configureTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(getCommonLib())
            }
        }
        val commonTest by getting {
            configureCommonTestDependencies()
        }
    }
}
