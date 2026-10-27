import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm")
}

// Default to the Kotlin Gradle plugin's version; compiler APIs are version-specific.
val compilerVersion: String = providers.gradleProperty("harness.compiler.version").getOrElse(kotlin.coreLibrariesVersion)

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib:$compilerVersion")
    api("org.jetbrains.kotlin:kotlin-compiler-embeddable:$compilerVersion")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // Only the example tests depend on Compose; the harness API remains Compose-independent.
    testImplementation("org.jetbrains.compose.runtime:runtime:${rootProject.property("compose.version")}")
}

kotlin {
    jvmToolchain(11)
    sourceSets {
        main { kotlin.setSrcDirs(listOf("src")) }
        test { kotlin.setSrcDirs(listOf("test")) }
    }
    compilerOptions {
        optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}

tasks.test {
    useJUnitPlatform()
    workingDir = projectDir
    inputs.dir("testData").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty(
        "kotlin.plugin.tests.update.goldens",
        providers.gradleProperty("kotlin.plugin.tests.update.goldens").getOrElse("false"),
    )
}
