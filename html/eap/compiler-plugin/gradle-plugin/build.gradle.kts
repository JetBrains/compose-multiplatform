/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import java.util.Properties
import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.buildTimeConfig)
}

val htmlRootDir = rootProject.projectDir.resolve("../../..").normalize()
val htmlGradleProperties = Properties().apply {
    htmlRootDir.resolve("gradle.properties").inputStream().use(::load)
}
val eapVersion = providers.gradleProperty("compose.html.eap.version").orNull
    ?: htmlGradleProperties.getProperty("compose.html.eap.version")
    ?: error("compose.html.eap.version must be set")

group = "org.jetbrains.compose.html"
version = eapVersion

repositories {
    mavenCentral()
}

val composeRepoUrl = providers.gradleProperty("COMPOSE_REPO_URL").orNull
    ?: System.getenv("COMPOSE_REPO_URL")
    ?: "https://packages.jetbrains.team/maven/p/cmp/dev"
val composeRepoUsername = providers.gradleProperty("COMPOSE_REPO_USERNAME").orNull
    ?: System.getenv("COMPOSE_REPO_USERNAME")
    ?: ""
val composeRepoKey = providers.gradleProperty("COMPOSE_REPO_KEY").orNull
    ?: System.getenv("COMPOSE_REPO_KEY")
    ?: ""

publishing {
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Compose HTML lowering Gradle plugin")
            description.set("Gradle plugin for the Compose HTML compiler plugin")
            url.set("https://www.jetbrains.com/lp/compose-mpp/")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
        }
    }
    repositories {
        maven {
            name = "internal"
            url = uri(composeRepoUrl)
            credentials {
                username = composeRepoUsername
                password = composeRepoKey
            }
        }
    }
}

// The HTML root build targets JVM 11. Gradle requires Java to use the same target.
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin-api"))
    testImplementation(kotlin("gradle-plugin"))
    testImplementation(kotlin("test-junit"))
}

gradlePlugin {
    plugins {
        create("htmlLowering") {
            id = "org.jetbrains.compose.html.lowering"
            implementationClass = "org.jetbrains.compose.html.gradle.HtmlLoweringGradlePlugin"
        }
    }
}

tasks.test {
    // Gradle ProjectBuilder injects synthetic classes through java.lang on JDK 21.
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
}

buildTimeConfig {
    config {
        packageName.set("org.jetbrains.compose.html.gradle")
        objectName.set("BuildTimeConfig")
        destination.set(layout.buildDirectory.dir("generated/buildTimeConfig").get().asFile)
        configProperties {
            val compilerPluginVersion: String by string(eapVersion)
        }
    }
}
