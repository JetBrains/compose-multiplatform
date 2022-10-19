plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

initDeps(project)

kotlin {
    jvm("desktop")
    android()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    js(IR) {
        browser()
    }

    sourceSets {
        create("iosMain") {
            dependsOn(getByName("commonMain"))
        }
        create("iosTest") {
            dependsOn(getByName("commonTest"))
        }

        getByName("iosX64Main") {
            dependsOn(getByName("iosMain"))
        }
        getByName("iosX64Test") {
            dependsOn(getByName("iosTest"))
        }

        getByName("iosArm64Main") {
            dependsOn(getByName("iosMain"))
        }
        getByName("iosArm64Test") {
            dependsOn(getByName("iosTest"))
        }

        getByName("iosSimulatorArm64Main") {
            dependsOn(getByName("iosMain"))
        }
        getByName("iosSimulatorArm64Test") {
            dependsOn(getByName("iosTest"))
        }

        named("commonTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin.testCommon)
                implementation(Deps.JetBrains.Kotlin.testAnnotationsCommon)
            }
        }

        named("androidTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin.testJunit)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin.testJunit)
            }
        }
        named("jsTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin.testJs)
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }
}
