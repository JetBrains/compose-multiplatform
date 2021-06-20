import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
//    val onPhone = System.getenv("SDK_NAME")?.startsWith("iphoneos") ?: false
//    if (onPhone) {
//        iosArm64("darwin")
//    } else {
        iosX64("darwin")
//    }
    jvm()

    // iosX64("darwin") {
    //     binaries {
    //         this.staticLib {
    //             baseName = "PlaygroundExampleKit"
    //             // embedBitcode = org.jetbrains.kotlin.gradle.plugin.mpp.Framework.BitcodeEmbeddingMode.DISABLE
    //         }
    //     }
    // }
//    val knTargets = listOf(
//        iosX64()
//    )

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(kotlin("stdlib-common"))

                implementation( "org.jetbrains.kotlinx:kotlinx-coroutines-core") {
                    version { strictly("1.4.3-native-mt") }
                }
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }

        /*val darwinMain = sourceSets.getOrCreate("darwinMain")
        darwinMain.dependsOn(commonMain)
        val darwinTest = sourceSets.getOrCreate("darwinTest")
        darwinMain.dependsOn(commonTest)*/



//        knTargets.forEach { target ->
//                target.compilations.getByName("main").source(darwinMain)
//                target.compilations.getByName("test").source(darwinTest)
//        }



/*
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(npm("css-typed-om", "0.4.0"))
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }*/
    }

}

typealias SourceSets = NamedDomainObjectContainer<KotlinSourceSet>
fun SourceSets.getOrCreate(name: String): KotlinSourceSet = findByName(name) ?: create(name)

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=true"
        )
    }
}
