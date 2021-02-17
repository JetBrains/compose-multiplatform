package org.jetbrains.compose.test

import org.gradle.testkit.runner.GradleRunner
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import java.io.File

data class TestProject(
    private val name: String,
    private val workingDir: File
) {
    private val additionalArgs = listOf(
        "--stacktrace",
        "-P${ComposeProperties.VERBOSE}=true"
    )

    init {
        val originalTestRoot = File("src/test/test-projects").resolve(name).also {
            check(it.exists()) { "Test project is not found: ${it.absolutePath}" }
        }
        for (orig in originalTestRoot.walk()) {
            if (!orig.isFile) continue

            val target = workingDir.resolve(orig.relativeTo(originalTestRoot))
            target.parentFile.mkdirs()

            if (orig.name.endsWith(".gradle") || orig.name.endsWith(".gradle.kts")) {
                val origContent = orig.readText()
                val newContent = origContent
                    .replace("COMPOSE_VERSION_PLACEHOLDER", TestProperties.composeVersion)
                    .replace("KOTLIN_VERSION_PLACEHOLDER", TestProperties.kotlinVersion)
                target.writeText(newContent)
            } else {
                orig.copyTo(target)
            }
        }
    }

    fun gradle(vararg args: String): GradleRunner =
        GradleRunner.create().apply {
            withGradleVersion(TestProperties.gradleVersionForTests)
            withProjectDir(workingDir)
            withArguments(args.toList() + additionalArgs)
            forwardOutput()
        }

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Do not commit!")
    fun gradleDebug(vararg args: String): GradleRunner =
        gradle(*args).withDebug(true)

    fun file(path: String): File =
        workingDir.resolve(path)
}

