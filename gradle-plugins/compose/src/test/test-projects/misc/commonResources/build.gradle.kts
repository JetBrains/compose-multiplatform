plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
}

group = "app.group"

kotlin {
    androidLibrary {
        compileSdk = 35
        namespace = "org.jetbrains.compose.resources.test"
        minSdk = 23
        androidResources.enable = true
    }
    jvm("desktop")

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.material)
                //there is the api to check correctness of the api configuration
                //https://github.com/JetBrains/compose-multiplatform/issues/4405
                api(compose.components.resources)
            }
        }
    }
}

abstract class GenerateAndroidRes : DefaultTask() {
    @get:Inject
    abstract val layout: ProjectLayout

    @get:OutputDirectory
    val outputDir = layout.buildDirectory.dir("generatedAndroidResources")

    @TaskAction
    fun run() {
        val dir = outputDir.get().asFile
        dir.deleteRecursively()
        File(dir, "values/strings.xml").apply {
            parentFile.mkdirs()
            writeText(
                """
                    <resources>
                        <string name="android_str">Android string</string>
                    </resources>
                """.trimIndent()
            )
        }
    }
}
compose.resources.customDirectory(
    sourceSetName = "androidMain",
    directoryProvider = tasks.register<GenerateAndroidRes>("generateAndroidRes").map { it.outputDir.get() }
)
