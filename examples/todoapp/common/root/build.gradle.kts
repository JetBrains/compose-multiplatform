import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("multiplatform-setup")
    id("android-setup")
    id("kotlin-parcelize")
}

kotlin {
    ios {
        binaries {
            framework {
                baseName = "KotlinCommon"
                linkerOpts.add("-lsqlite3")
                export(project(":common:database"))
                export(project(":common:main"))
                export(project(":common:edit"))
                export(Deps.ArkIvanov.Decompose.decompose)
                export(Deps.ArkIvanov.MVIKotlin.mvikotlinMain)
                export(Deps.ArkIvanov.Essenty.lifecycle)
            }
        }
    }

    sourceSets {
        named("commonMain") {
            dependencies {
                implementation(project(":common:utils"))
                implementation(project(":common:database"))
                implementation(project(":common:main"))
                implementation(project(":common:edit"))
                implementation(Deps.ArkIvanov.MVIKotlin.mvikotlin)
                implementation(Deps.ArkIvanov.Decompose.decompose)
                implementation(Deps.Badoo.Reaktive.reaktive)
            }
        }
    }

    sourceSets {
        named("iosMain") {
            dependencies {
                api(project(":common:database"))
                api(project(":common:main"))
                api(project(":common:edit"))
                api(Deps.ArkIvanov.Decompose.decompose)
                api(Deps.ArkIvanov.MVIKotlin.mvikotlinMain)
                api(Deps.ArkIvanov.Essenty.lifecycle)
            }
        }
    }
}
