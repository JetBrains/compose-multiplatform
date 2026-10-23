/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import java.util.Properties

plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.compose") apply false
}

val pluginProperties = Properties().apply {
    file("plugin.properties").inputStream().use(::load)
}
val kotlinVersion = Properties().apply {
    file("../../buildSrc/gradle.properties").inputStream().use(::load)
}.getProperty("kotlin.version")

group = "org.jetbrains.compose.html"
version = pluginProperties.getProperty("version")

repositories {
    mavenCentral()
}

subprojects {
    // Resolve the compiler artifact from this build while developing the Gradle plugin.
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.jetbrains.compose.html:compose-html-compiler-plugin")).using(project(":"))
        }
    }
}

val sharedSerializationSources = tasks.register<Sync>("syncSerializationSources") {
    from("../core/src/commonMain/kotlin") {
        include("org/jetbrains/compose/web/dom/HtmlSerializationRules.kt")
        include("org/jetbrains/compose/web/dom/RawTextTagName.kt")
    }
    into(layout.buildDirectory.dir("generated/serializationSources"))
}

kotlin {
    jvmToolchain(21)
    sourceSets.main {
        kotlin.srcDir(sharedSerializationSources)
    }
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
}

tasks.named("check") {
    dependsOn(":tests:check", gradle.includedBuild("gradle-plugin").task(":test"))
}
