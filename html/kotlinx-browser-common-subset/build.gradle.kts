@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.api.publish.maven.MavenPublication
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

val browserIdentityTestSources = layout.projectDirectory.dir("src/browserIdentityTest/kotlin")
val karmaConfigDirectory = layout.projectDirectory.dir("karma.config.d")
val htmlProperties = Properties().apply {
    layout.projectDirectory.file("../gradle.properties").asFile.inputStream().use(::load)
}
val generatorProperties = Properties().apply {
    layout.projectDirectory.file("generator/gradle.properties").asFile.inputStream().use(::load)
}
val kotlinxBrowserVersion = requireNotNull(generatorProperties.getProperty("kotlinx.browser.version")) {
    "kotlinx.browser.version is missing from generator/gradle.properties"
}

group = "org.jetbrains.compose.html"
version = providers.gradleProperty("compose.version").orNull ?: requireNotNull(
    htmlProperties.getProperty("compose.version"),
) {
    "compose.version is missing from html/gradle.properties"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    js(IR) {
        browser {
            testTask {
                useKarma {
                    useConfigDirectory(karmaConfigDirectory.asFile.absolutePath)
                    useChromeHeadless()
                }
            }
        }
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }
    wasmJs {
        browser {
            testTask {
                useKarma {
                    useConfigDirectory(karmaConfigDirectory.asFile.absolutePath)
                    useChromeHeadless()
                }
            }
        }
        compilerOptions {
            optIn.add("kotlin.js.ExperimentalWasmJsInterop")
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        val webMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-browser:$kotlinxBrowserVersion")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jsTest by getting {
            kotlin.srcDir(browserIdentityTestSources)
        }
        val wasmJsTest by getting {
            kotlin.srcDir(browserIdentityTestSources)
        }
    }
}

val generatorBuild = gradle.includedBuild("kotlinx-browser-common-subset-generator")
val generateSubset = generatorBuild.task(":ksp-runner:generateKotlinxBrowserCommonSubset")
val checkSubset = generatorBuild.task(":ksp-runner:checkKotlinxBrowserCommonSubset")
val updateSubset = generatorBuild.task(":ksp-runner:updateKotlinxBrowserCommonSubset")
val checkGenerator = generatorBuild.task(":check")

tasks.register("generateKotlinxBrowserCommonSubset") {
    group = "generation"
    description = "Generates the common browser subset into the generator build directory."
    dependsOn(generateSubset)
}

tasks.register("checkKotlinxBrowserCommonSubset") {
    group = "verification"
    description = "Checks checked-in common browser subset sources against fresh generation."
    dependsOn(checkSubset)
}

tasks.register("updateKotlinxBrowserCommonSubset") {
    group = "generation"
    description = "Explicitly updates checked-in common browser subset sources and manifest."
    dependsOn(updateSubset)
}

tasks.named("check") {
    dependsOn("checkKotlinxBrowserCommonSubset", checkGenerator)
}

publishing {
    repositories {
        maven {
            name = "internal"
            url = uri(
                providers.gradleProperty("COMPOSE_REPO_URL").orNull
                    ?: System.getenv("COMPOSE_REPO_URL")
                    ?: "https://packages.jetbrains.team/maven/p/cmp/dev",
            )
            credentials {
                username = providers.gradleProperty("COMPOSE_REPO_USERNAME").orNull
                    ?: System.getenv("COMPOSE_REPO_USERNAME")
                    ?: ""
                password = providers.gradleProperty("COMPOSE_REPO_KEY").orNull
                    ?: System.getenv("COMPOSE_REPO_KEY")
                    ?: ""
            }
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("Compose HTML common kotlinx-browser subset")
            description.set(
                "Common browser types for Compose HTML common, JavaScript, WebAssembly, and JVM source sets.",
            )
            url.set("https://www.jetbrains.com/lp/compose-mpp/")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("JetBrains")
                    name.set("JetBrains Compose Team")
                    organization.set("JetBrains")
                    organizationUrl.set("https://www.jetbrains.com")
                }
            }
            scm {
                connection.set("scm:git://github.com/JetBrains/compose-multiplatform.git")
                developerConnection.set("scm:git://github.com/JetBrains/compose-multiplatform.git")
                url.set("https://github.com/jetbrains/compose-multiplatform")
            }
        }
    }
}
