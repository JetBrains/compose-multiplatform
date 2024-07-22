plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.application")
    id("org.jetbrains.compose")
}

group = "app.group"

kotlin {
    androidTarget {
        compilations.all {
            compileTaskProvider {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
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

android {
    compileSdk = 34
    namespace = "org.jetbrains.compose.resources.test"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    defaultConfig {
        applicationId = "org.example.project"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    signingConfigs {
        create("testkey") {
            storeFile = project.file("key/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("testkey")
        }
        getByName("debug") {
            signingConfig = signingConfigs.getByName("testkey")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
