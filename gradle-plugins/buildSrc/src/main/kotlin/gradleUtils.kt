/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File

inline fun <reified T> Project.configureIfExists(fn: T.() -> Unit) {
    extensions.findByType(T::class.java)?.fn()
}

val isWindows = DefaultNativePlatform.getCurrentOperatingSystem().isWindows

fun Project.configureAllTests(fn: Test.() -> Unit = {}) {
    fun DependencyHandler.testImplementation(notation: Any) =
        add(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, notation)

    dependencies {
        testImplementation(platform("org.junit:junit-bom:5.7.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
        fn()
    }
}

fun Test.systemProperties(map: Map<String, Any>) {
    for ((k, v) in map) {
        systemProperty(k, v)
    }
}

fun TaskProvider<*>.dependsOn(vararg dependencies: Any) {
    configure {
        dependsOn(dependencies)
    }
}

inline fun <reified T : Task> TaskContainer.registerVerificationTask(
    name: String,
    crossinline fn: T.() -> Unit
): TaskProvider<T> =
    register(name, T::class) {
        group = "verification"
        fn()
    }.apply {
        named("check").dependsOn(this)
    }

val Provider<out Jar>.archiveFile: Provider<RegularFile>
    get() = flatMap { it.archiveFile }