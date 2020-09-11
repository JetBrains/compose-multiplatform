import org.jetbrains.compose.compose
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.0"
    id("org.jetbrains.compose") version "0.1.0-unmerged30"
    application
}

repositories {
    google()
    jcenter()
    maven("https://packages.jetbrains.team/maven/p/ui/dev")
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(compose.desktop.all)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

val libraryPath = "third_party/java-cef/jcef_build/native/Release"

application {
    applicationDefaultJvmArgs = listOf("-Djava.library.path=$libraryPath")
    mainClassName = "org.jetbrains.compose.desktop.AppKt"
}
