plugins {
    id("multiplatform-compose-setup")
    id("android-setup")
}

val os = org.gradle.internal.os.OperatingSystem.current()

val platform = when {
    os.isWindows -> "win"
    os.isMacOsX -> "mac"
    else -> "linux"
}

val jdkVersion = "11.0.2"

kotlin {
    sourceSets {

        named("commonMain") {
            dependencies {
            }
        }

        named("androidMain") {
            dependencies {
                implementation(Deps.AndroidX.Activity.activityCompose)
            }
        }

        named("desktopMain") {
            dependencies {
                implementation("org.openjfx:javafx-base:$jdkVersion:${platform}")
                implementation("org.openjfx:javafx-graphics:$jdkVersion:${platform}")
                implementation("org.openjfx:javafx-controls:$jdkVersion:${platform}")
                implementation("org.openjfx:javafx-fxml:$jdkVersion:${platform}")
                implementation("org.openjfx:javafx-media:$jdkVersion:${platform}")
                implementation("org.openjfx:javafx-web:$jdkVersion:${platform}")
                implementation("org.openjfx:javafx-swing:$jdkVersion:${platform}")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-javafx:1.6.4")
            }
        }
    }
}

