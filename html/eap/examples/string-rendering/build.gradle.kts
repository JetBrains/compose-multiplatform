/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

val kotlinxBrowserCommonSubsetVersion: String =
    providers.gradleProperty("compose.html.eap.kotlinx-browser-common-subset.version").get()
val composeVersion: String = providers.gradleProperty("compose.version").get()

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose.html.lowering") // optional, lower static parts at compile time for faster rendering
    application
}

// Use the compiler plugin from this checkout, like the EAP runtime below.
configurations.configureEach {
    resolutionStrategy.dependencySubstitution {
        substitute(module("org.jetbrains.compose.html:compose-html-compiler-plugin"))
            .using(project(":compose-html-compiler-plugin"))
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation("org.jetbrains.compose.runtime:runtime:$composeVersion")
    implementation(project(":html-core-eap"))
    implementation(
        "org.jetbrains.compose.html:kotlinx-browser-common-subset:$kotlinxBrowserCommonSubsetVersion"
    )
}

application {
    mainClass.set("MainKt")
}
