plugins {
    id("com.android.library")
    id("kotlin-multiplatform")
}

kotlin {
    jvm("desktop")
    android()
    ios()

    js(IR) {
        browser()
    }

    sourceSets {
        named("commonTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin(project).testCommon)
                implementation(Deps.JetBrains.Kotlin(project).testAnnotationsCommon)
            }
        }

        named("androidTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin(project).testJunit)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin(project).testJunit)
            }
        }
        named("jsTest") {
            dependencies {
                implementation(Deps.JetBrains.Kotlin(project).testJs)
            }
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "1.8"
    }
}
