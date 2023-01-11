/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.util.GradleVersion
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import java.io.File
import java.util.Properties

data class TestEnvironment(
    val workingDir: File,
    val kotlinVersion: String = TestKotlinVersions.Default,
    val composeGradlePluginVersion: String = TestProperties.composeGradlePluginVersion,
    val composeCompilerPlugin: String? = null,
    val composeCompilerArgs: String? = null,
    val composeVerbose: Boolean = true,
) {
    private val placeholders = linkedMapOf(
        "COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER" to composeGradlePluginVersion,
        "KOTLIN_VERSION_PLACEHOLDER" to kotlinVersion,
        "COMPOSE_COMPILER_PLUGIN_PLACEHOLDER" to composeCompilerPlugin,
        "COMPOSE_COMPILER_PLUGIN_ARGS_PLACEHOLDER" to composeCompilerArgs,
    )

    fun replacePlaceholdersInFile(file: File) {
        var content = file.readText()
        for ((placeholder, value) in placeholders.entries) {
            if (value != null) {
                content = content.replace(placeholder, value)
            }
        }
        file.writeText(content)
    }
}

class TestProject(
    private val name: String,
    private val testEnvironment: TestEnvironment
) {
    private val testProjectsRootDir = File("src/test/test-projects")
    private val additionalArgs = listOf(
        "--stacktrace",
        "--init-script", testProjectsRootDir.resolve("init.gradle").absolutePath,
        "-P${ComposeProperties.VERBOSE}=${testEnvironment.composeVerbose}"
    )

    init {
        val originalTestRoot = testProjectsRootDir.resolve(name).also {
            check(it.exists()) { "Test project is not found: ${it.absolutePath}" }
        }
        for (orig in originalTestRoot.walk()) {
            if (!orig.isFile) continue

            val target = testEnvironment.workingDir.resolve(orig.relativeTo(originalTestRoot))
            target.parentFile.mkdirs()
            orig.copyTo(target)

            if (orig.name.endsWith(".gradle") || orig.name.endsWith(".gradle.kts")) {
                testEnvironment.replacePlaceholdersInFile(target)
            }
        }
    }

    internal fun gradle(vararg args: String): BuildResult {
        if (TestProperties.gradleConfigurationCache) {
            if (GradleVersion.version(TestProperties.gradleVersionForTests) < GradleVersion.version("8.0-rc-1")) {
                // Gradle 7.* does not use the configuration cache in the same build.
                // In other words, if cache misses, Gradle performs configuration,
                // but does not, use the serialized task graph.
                // So in order to test the cache, we need to perform dry-run before the actual run.
                // This should be fixed in https://github.com/gradle/gradle/issues/21985 (which is planned for 8.0 RC 1)
                gradleRunner(args.withDryRun()).build()
            }
        }

        return gradleRunner(args).build()
    }

    private fun Array<out String>.withDryRun(): Array<String> {
        var sawDryRun = false
        val dryRunArgs = ArrayList<String>(size)
        for (arg in this) {
            sawDryRun = sawDryRun || arg.trim() in listOf("-m", "--dry-run")
            dryRunArgs.add(arg)
        }
        if (!sawDryRun) {
            dryRunArgs.add("--dry-run")
        }
        return dryRunArgs.toTypedArray()
    }

    private fun gradleRunner(args: Array<out String>): GradleRunner {
        val allArgs = args.toMutableList()
        allArgs.addAll(additionalArgs)
        if (TestProperties.gradleConfigurationCache) {
            allArgs.add("--configuration-cache")
        }

        return GradleRunner.create().apply {
            withGradleVersion(TestProperties.gradleVersionForTests)
            withProjectDir(testEnvironment.workingDir)
            withArguments(allArgs)
            forwardOutput()
        }
    }

    fun file(path: String): File =
        testEnvironment.workingDir.resolve(path)

    fun modifyText(path: String, fn: (String) -> String) {
        val file = file(path)
        val oldContent = file.readText()
        val newContent = fn(oldContent)
        file.writeText(newContent)
    }

    fun appendText(path: String, fn: () -> String) {
        val file = file(path)
        val oldContent = file.readText()
        val newContent = oldContent + "\n" + fn()
        file.writeText(newContent)
    }

    fun modifyGradleProperties(fn: Properties.() -> Unit) {
        val propertiesFile = file("gradle.properties")
        val properties = Properties()
        if (propertiesFile.exists()) {
            propertiesFile.bufferedReader().use { reader ->
                properties.load(reader)
            }
        }
        fn(properties)
        propertiesFile.delete()

        if (properties.isNotEmpty()) {
            propertiesFile.bufferedWriter().use { writer ->
                properties.store(writer, null)
            }
        }
    }
}

