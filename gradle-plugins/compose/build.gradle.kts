plugins {
    kotlin("jvm")
    id("de.fuerstenau.buildconfig")
    id("com.gradle.plugin-publish")
    id("java-gradle-plugin")
    id("maven-publish")
}

gradlePluginConfig {
    pluginId = "org.jetbrains.compose"
    artifactId = "compose-gradle-plugin"
    displayName = "Jetpack Compose Plugin"
    description = "Jetpack Compose gradle plugin for easy configuration"
    implementationClass = "org.jetbrains.compose.ComposePlugin"
}

buildConfig {
    packageName = "org.jetbrains.compose"
    clsName = "ComposeBuildConfig"
    buildConfigField("String", "composeVersion", BuildProperties.composeVersion)
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(localGroovy())
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin")
    testImplementation(gradleTestKit())
}
