/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import java.io.File

data class TestEnvironment(
    val workingDir: File,
    val kotlinVersion: String = TestKotlinVersions.Default,
    val composeGradlePluginVersion: String = TestProperties.composeGradlePluginVersion,
    val composeCompilerArtifact: String? = null,
    val customJavaToolchainVersion: Int? = null
) {
    private val placeholders = linkedMapOf(
        "COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER" to composeGradlePluginVersion,
        "KOTLIN_VERSION_PLACEHOLDER" to kotlinVersion,
        "COMPOSE_COMPILER_ARTIFACT_PLACEHOLDER" to composeCompilerArtifact,
        "CUSTOM_JAVA_TOOLCHAIN_VERSION_PLACEHOLDER" to customJavaToolchainVersion?.toString()
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
        "-P${ComposeProperties.VERBOSE}=true"
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

    fun gradle(vararg args: String): GradleRunner =
        GradleRunner.create().apply {
            withGradleVersion(TestProperties.gradleVersionForTests)
            withProjectDir(testEnvironment.workingDir)
            withArguments(args.toList() + additionalArgs)
            forwardOutput()
        }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Do not commit!")
    fun gradleDebug(vararg args: String): GradleRunner =
        gradle(*args).withDebug(true)

    fun file(path: String): File =
        testEnvironment.workingDir.resolve(path)
}

