// We store Kotlin and Compose versions in gradle.properties to
// be able to override them on CI.
// You probably won't need this, so you can get rid of `project` in this file.
import org.gradle.api.Project

lateinit var properties: Map<String, *>

fun initDeps(project: Project) {
    properties = project.properties
}

object Deps {
    object JetBrains {
        object Kotlin {
            private val VERSION get() = properties["kotlin.version"]
            val gradlePlugin get() = "org.jetbrains.kotlin:kotlin-gradle-plugin:$VERSION"
        }

        object Compose {
            private val VERSION get() = properties["compose.version"]
            val gradlePlugin get() = "org.jetbrains.compose:compose-gradle-plugin:$VERSION"
        }
    }

    object Android {
        object Tools {
            object Build {
                const val gradlePlugin = "com.android.tools.build:gradle:7.0.4"
            }
        }
    }

    object AndroidX {
        object AppCompat {
            const val appCompat = "androidx.appcompat:appcompat:1.4.2"
        }

        object Activity {
            const val activityCompose = "androidx.activity:activity-compose:1.5.1"
        }
    }

}
