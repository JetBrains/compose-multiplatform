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
            }
        }
        val commonTest by getting {
            configureCommonTestDependencies()
        }

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val iosMain by getting {
            dependsOn(nativeMain)
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosX64Main by getting {
            dependsOn(nativeMain)
        }
        val macosArm64Main by getting {
            dependsOn(nativeMain)
        }

        if (project.isMingwX64Enabled) {
            val mingwX64Main by getting {
                dependsOn(nativeMain)
            }
        }
    }
}
