import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

kotlin {
    jvm {}
    sourceSets {
        named("jvmMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(project(":CodeEditor:library"))
                implementation("org.jetbrains.compose.components:components-codeeditor-platform-api:${version}")

                runtimeOnly("org.jetbrains.compose.components:components-codeeditor-platform-lib:${version}")
                runtimeOnly("org.jetbrains.compose.components:components-codeeditor-platform-lib:${version}:idea")
                runtimeOnly("org.jetbrains.compose.components:components-codeeditor-platform-lib:${version}:java")
                runtimeOnly("org.jetbrains.compose.components:components-codeeditor-platform-lib:${version}:kotlin")
                runtimeOnly("org.jetbrains.compose.components:components-codeeditor-platform-lib:${version}:properties")
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.compose.codeeditor.demo.MainKt"
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}
