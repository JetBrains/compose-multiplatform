/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import java.util.Properties

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}

val pluginProperties = Properties().apply {
    file("../plugin.properties").inputStream().use(::load)
}
val kotlinVersion = Properties().apply {
    file("../../../buildSrc/gradle.properties").inputStream().use(::load)
}.getProperty("kotlin.version")

group = "org.jetbrains.compose.html"
version = pluginProperties.getProperty("version")

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    testImplementation(kotlin("test-junit"))
}

gradlePlugin {
    plugins {
        create("htmlLowering") {
            id = pluginProperties.getProperty("id")
            implementationClass = "org.jetbrains.compose.html.gradle.HtmlLoweringGradlePlugin"
        }
    }
}

tasks.test {
    // Gradle ProjectBuilder injects synthetic classes through java.lang on JDK 21.
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

// The plugin needs its coordinates after it has been packaged and installed elsewhere.
tasks.processResources {
    from("../plugin.properties") { rename { "compose-html-lowering.properties" } }
}
