/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

plugins {
    kotlin("jvm")
}

val eapVersion = providers.gradleProperty("compose.html.eap.version").get()

group = "org.jetbrains.compose.html"
version = eapVersion

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

kotlin {
    jvmToolchain(11)
}

dependencies {
    implementation(project(":internal-html-serialization-eap"))
    compileOnly(kotlin("compiler-embeddable"))

    testImplementation(project(":compiler-plugin-test-harness"))
    testImplementation(project(":html-core-eap"))
    testImplementation("org.jetbrains.compose.runtime:runtime:${rootProject.property("compose.version")}")
    testImplementation(
        "org.jetbrains.compose.html:kotlinx-browser-common-subset:" +
                rootProject.property("compose.html.eap.kotlinx-browser-common-subset.version")
    )
    testImplementation(kotlin("test"))
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}

tasks.named("check") {
    dependsOn("$compilerPluginPath:tests:check")
    // Run the local Gradle plugin tests only when its build is included (local mode).
    gradle.includedBuilds
        .find { it.name == "compose-html-lowering-gradle-plugin" }
        ?.let { dependsOn(it.task(":test")) }
}

tasks.test {
    // Golden IR tests resolve testData/ paths relative to this directory.
    workingDir = projectDir
    inputs.dir("testData").withPathSensitivity(PathSensitivity.RELATIVE)
}
