plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    compileOnly(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    implementation("org.jetbrains.intellij.plugins:gradle-intellij-plugin:1.3.0")
}
