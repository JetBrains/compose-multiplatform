/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File

inline fun <reified T> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}

val javaHomeForTests: String? = when {
    // __COMPOSE_NATIVE_DISTRIBUTIONS_MIN_JAVA_VERSION__
    JavaVersion.current() >= JavaVersion.VERSION_15 -> System.getProperty("java.home")
    else -> System.getenv("JDK_15")
        ?: System.getenv("JDK_FOR_GRADLE_TESTS")
}

val isWindows = DefaultNativePlatform.getCurrentOperatingSystem().isWindows

fun Test.configureJavaForComposeTest() {
    if (javaHomeForTests != null) {
        val executableFileName = if (isWindows) "java.exe" else "java"
        executable = File(javaHomeForTests).resolve("bin/$executableFileName").absolutePath
    } else {
        doFirst { error("Use JDK 15+ to run tests or set up JDK_15/JDK_FOR_GRADLE_TESTS env. var") }
    }
}

fun Project.configureJUnit() {
    fun DependencyHandler.testImplementation(notation: Any) =
        add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, notation)

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.7.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}