/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import java.util.Properties

plugins {
    kotlin("jvm")
}

val pluginProperties = Properties().apply {
    file("plugin.properties").inputStream().use(::load)
}
val kotlinVersion = Properties().apply {
    rootProject.file("buildSrc/gradle.properties").inputStream().use(::load)
}.getProperty("kotlin.version")

group = "org.jetbrains.compose.html"
version = pluginProperties.getProperty("version")

val compilerPluginPath = path
subprojects {
    // Resolve the compiler artifact from this build while developing the Gradle plugin.
    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.jetbrains.compose.html:compose-html-compiler-plugin"))
                .using(project(compilerPluginPath))
        }
    }
}

// The HTML root build targets JVM 11. Gradle requires Java to use the same target.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

dependencies {
    implementation(project(":internal-html-serialization-eap"))
    compileOnly("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")

    testImplementation(project(":compiler-plugin-test-harness"))
    testImplementation(project(":html-core-eap"))
    testImplementation("org.jetbrains.compose.runtime:runtime:${rootProject.property("compose.version")}")
    testImplementation(
        "org.jetbrains.compose.html:kotlinx-browser-common-subset:" +
                rootProject.property("compose.html.eap.kotlinx-browser-common-subset.version")
    )
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.named("check") {
    dependsOn(
        "$compilerPluginPath:tests:check",
        gradle.includedBuild("compose-html-lowering-gradle-plugin").task(":test")
    )
}

tasks.test {
    useJUnitPlatform()
    // Golden IR tests resolve testData/ paths relative to this directory.
    workingDir = projectDir
    inputs.dir("testData").withPathSensitivity(PathSensitivity.RELATIVE)
}
