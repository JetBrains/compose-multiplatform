
// We store Kotlin and Compose versions in gradle.properties to 
// be able to override them on CI.
// You probably won't need this, so you can get rid of `project` in this file.
import org.gradle.api.Project

object Deps {
    object JetBrains {
        class Kotlin(private val project: Project) {
            private val VERSION = project.properties["kotlin.version"]
            val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$VERSION"
            val testCommon = "org.jetbrains.kotlin:kotlin-test-common:$VERSION"
            val testJunit = "org.jetbrains.kotlin:kotlin-test-junit:$VERSION"
            val testJs = "org.jetbrains.kotlin:kotlin-test-js:$VERSION"
            val testAnnotationsCommon = "org.jetbrains.kotlin:kotlin-test-annotations-common:$VERSION"
        }

        class Compose(private val project: Project) {
            private val VERSION = project.properties["compose.version"]
            val gradlePlugin = "org.jetbrains.compose:compose-gradle-plugin:$VERSION"
        }
    }

    object Android {
        object Tools {
            object Build {
                const val gradlePlugin = "com.android.tools.build:gradle:4.1.0"
            }
        }
    }

    object AndroidX {
        object AppCompat {
            const val appCompat = "androidx.appcompat:appcompat:1.3.0"
        }

        object Activity {
            const val activityCompose = "androidx.activity:activity-compose:1.3.0"
        }
    }

    object ArkIvanov {
        object MVIKotlin {
            private const val VERSION = "3.0.0-alpha01"
            const val rx = "com.arkivanov.mvikotlin:rx:$VERSION"
            const val mvikotlin = "com.arkivanov.mvikotlin:mvikotlin:$VERSION"
            const val mvikotlinMain = "com.arkivanov.mvikotlin:mvikotlin-main:$VERSION"
            const val mvikotlinLogging = "com.arkivanov.mvikotlin:mvikotlin-logging:$VERSION"
            const val mvikotlinTimeTravel = "com.arkivanov.mvikotlin:mvikotlin-timetravel:$VERSION"
            const val mvikotlinExtensionsReaktive = "com.arkivanov.mvikotlin:mvikotlin-extensions-reaktive:$VERSION"
        }

        object Decompose {
            private const val VERSION = "0.3.1"
            const val decompose = "com.arkivanov.decompose:decompose:$VERSION"
            const val extensionsCompose = "com.arkivanov.decompose:extensions-compose-jetbrains:$VERSION"
        }
    }

    object Badoo {
        object Reaktive {
            private const val VERSION = "1.1.22"
            const val reaktive = "com.badoo.reaktive:reaktive:$VERSION"
            const val reaktiveTesting = "com.badoo.reaktive:reaktive-testing:$VERSION"
            const val utils = "com.badoo.reaktive:utils:$VERSION"
            const val coroutinesInterop = "com.badoo.reaktive:coroutines-interop:$VERSION"
        }
    }

    object Squareup {
        object SQLDelight {
            private const val VERSION = "1.5.0"

            const val gradlePlugin = "com.squareup.sqldelight:gradle-plugin:$VERSION"
            const val androidDriver = "com.squareup.sqldelight:android-driver:$VERSION"
            const val sqliteDriver = "com.squareup.sqldelight:sqlite-driver:$VERSION"
            const val nativeDriver = "com.squareup.sqldelight:native-driver:$VERSION"
            const val sqljsDriver = "com.squareup.sqldelight:sqljs-driver:$VERSION"
        }
    }
}
