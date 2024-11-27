repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev/")
}

plugins {
    `kotlin-dsl`
//    alias(libs.plugins.multiplatform)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
//    implementation(libs.kotlin.gradle.plugin)
}
