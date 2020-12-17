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
                baseName = "Todo"
                linkerOpts.add("-lsqlite3")
                export(project(":common:database"))
                export(project(":common:main"))
                export(project(":common:edit"))

                when (val target = this.compilation.target.name) {
                    "iosX64" -> {
                        export(Deps.ArkIvanov.Decompose.decomposeIosX64)
                        export(Deps.ArkIvanov.MVIKotlin.mvikotlinMainIosX64)
                    }

                    "iosArm64" -> {
                        export(Deps.ArkIvanov.Decompose.decomposeIosArm64)
                        export(Deps.ArkIvanov.MVIKotlin.mvikotlinMainIosArm64)
                    }

                    else -> error("Unsupported target: $target")
                }
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
            }
        }

        named("iosX64Main") {
            dependencies {
                api(Deps.ArkIvanov.Decompose.decomposeIosX64)
                api(Deps.ArkIvanov.MVIKotlin.mvikotlinMainIosX64)
            }
        }

        named("iosArm64Main") {
            dependencies {
                api(Deps.ArkIvanov.Decompose.decomposeIosArm64)
                api(Deps.ArkIvanov.MVIKotlin.mvikotlinMainIosArm64)
            }
        }
    }
}

fun getIosTarget(): String {
    val sdkName = System.getenv("SDK_NAME") ?: "iphonesimulator"

    return if (sdkName.startsWith("iphoneos")) "iosArm64" else "iosX64"
}

val packForXcode by tasks.creating(Sync::class) {
    group = "build"
    val mode = System.getenv("CONFIGURATION") ?: "DEBUG"
    val targetName = getIosTarget()
    val framework = kotlin.targets.getByName<KotlinNativeTarget>(targetName).binaries.getFramework(mode)
    inputs.property("mode", mode)
    dependsOn(framework.linkTask)
    val targetDir = File(buildDir, "xcode-frameworks")
    from(framework.outputDirectory)
    into(targetDir)
}
