plugins {
    kotlin("multiplatform")
}

kotlin {
    configureTargets()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(getCommonLib())
                implementation(getLibDependencyForMain())
            }
        }
        val commonTest by getting {
            configureCommonTestDependencies()
        }
    }
}
