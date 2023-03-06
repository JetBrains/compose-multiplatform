plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

@OptIn(org.jetbrains.kotlin.gradle.kpm.external.ExternalVariantApi::class)
fun org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.configureDefaultTargets() {
    jvm("desktop")
    ios()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    macosX64()
    macosArm64()
    // We use linux agents on CI. So it doesn't run the tests, but it builds the klib anyway which is time consuming.
    if (project.isInIdea) mingwX64()
    linuxX64()
}

kotlin {
    if (project.isFailingJsCase) {
        configureJsTargets()
    } else {
        configureDefaultTargets()
    }

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
    }
}
