import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.apollographql)
}

kotlin {
    jvm("desktop")

    android {
        namespace = "com.example.myapplication.common"
        compileSdk = 37
        minSdk = 26

        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material)
            implementation(libs.material.icons.core)
            implementation(libs.apollo.runtime)
            implementation(libs.prettytime)
        }
        androidMain {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
            dependencies {
                api(libs.androidx.activity.compose)
                api(libs.appcompat)
                api(libs.core.ktx)
            }
        }
        val desktopMain by getting {
            kotlin.srcDirs("src/jvmAndAndroidMain/kotlin")
        }
    }
}

apollo {
    generateKotlinModels.set(true)
    customTypeMapping.putAll(mapOf("DateTime" to "java.util.Date"))
}
