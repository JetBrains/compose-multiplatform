plugins {
    kotlin("multiplatform")
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

        val nativeMain by creating {
            dependsOn(commonMain)
        }
        val iosMain by getting {
            dependsOn(nativeMain)
        }
        val iosX64Main by getting {
            dependsOn(iosMain)
        }
        val iosArm64Main by getting {
            dependsOn(iosMain)
        }
        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
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
