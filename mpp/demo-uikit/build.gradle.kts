import androidx.build.AndroidXComposePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("org.jetbrains.gradle.apple.applePlugin") version "222.849-0.15.1"
}

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)

dependencies {
    kotlinPlugin(project(":compose:compiler:compiler"))
    kotlinNativeCompilerPluginClasspath(project(":compose:compiler:compiler-hosted"))
}

repositories {
    mavenLocal()
}

kotlin {
    iosX64("uikitX64") {
        binaries {
            framework {
                baseName = "shared"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
            }
        }
    }
    iosArm64("uikitArm64") {
        binaries {
            framework {
                baseName = "shared"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(libs.kotlinCoroutinesCore)
            }
        }
        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.skikoCommon)
            }
        }
        val nativeMain by creating { dependsOn(skikoMain) }
        val darwinMain by creating { dependsOn(nativeMain) }
        val uikitMain by creating { dependsOn(darwinMain) }
        val uikitX64Main by getting { dependsOn(uikitMain) }
        val uikitArm64Main by getting { dependsOn(uikitMain) }
    }
}

apple {
    iosApp {
        println("sourceSet.name: ${sourceSet.name}")
        productName = "composeuikit"

        sceneDelegateClass = "SceneDelegate"
        launchStoryboard = "LaunchScreen"

        dependencies {
            implementation(project(":compose:mpp:demo-uikit"))
        }
    }
}
