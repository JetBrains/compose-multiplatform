/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.utils

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import java.io.File
import java.util.Properties

data class TestEnvironment(
    val workingDir: File,
    val kotlinVersion: String = TestKotlinVersions.Default,
    val composeGradlePluginVersion: String = TestProperties.composeGradlePluginVersion,
    val composeCompilerArtifact: String? = null
)

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

            if (orig.name.endsWith(".gradle") || orig.name.endsWith(".gradle.kts")) {
                val origContent = orig.readText()
                var newContent = origContent
                    .replace("COMPOSE_GRADLE_PLUGIN_VERSION_PLACEHOLDER", testEnvironment.composeGradlePluginVersion)
                    .replace("KOTLIN_VERSION_PLACEHOLDER", testEnvironment.kotlinVersion)
                if (testEnvironment.composeCompilerArtifact != null) {
                    newContent = newContent.replace("COMPOSE_COMPILER_ARTIFACT_PLACEHOLDER", testEnvironment.composeCompilerArtifact)
                }
                target.writeText(newContent)
            } else {
                orig.copyTo(target)
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

    fun modifyTextFile(path: String, fn: (String) -> String) {
        val file = file(path)
        val oldContent = file.readText()
        val newContent = fn(oldContent)
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

