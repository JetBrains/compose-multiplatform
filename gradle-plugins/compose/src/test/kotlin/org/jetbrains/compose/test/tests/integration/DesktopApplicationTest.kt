/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.gradle.internal.impldep.org.testng.Assert
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.compose.desktop.application.internal.*
import org.jetbrains.compose.internal.uppercaseFirstChar
import org.jetbrains.compose.test.utils.*

import java.io.File
import java.util.*
import java.util.jar.JarFile
import kotlin.collections.HashSet
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

class DesktopApplicationTest : GradlePluginTestBase() {
    @Test
    fun smokeTestRunTask() = with(testProject(TestProjects.jvm)) {
        file("build.gradle").modify {
            it + """
                afterEvaluate {
                    tasks.getByName("run").doFirst {
                        throw new StopExecutionException("Skip run task")
                    }
                    
                    tasks.getByName("runDistributable").doFirst {
                        throw new StopExecutionException("Skip runDistributable task")
                    }
                }
            """.trimIndent()
        }
        gradle("run").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":run")?.outcome)
        }
        gradle("runDistributable").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":createDistributable")!!.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":runDistributable")?.outcome)
        }
    }

    @Test
    fun testRunMpp() = with(testProject(TestProjects.mpp)) {
        val logLine = "Kotlin MPP app is running!"
        gradle("run").build().checks { check ->
            check.taskOutcome(":run", TaskOutcome.SUCCESS)
            check.logContains(logLine)
        }
        gradle("runDistributable").build().checks { check ->
            check.taskOutcome(":createDistributable", TaskOutcome.SUCCESS)
            check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)
            check.logContains(logLine)
        }
    }

    @Test
    fun testAndroidxCompiler() = with(testProject(TestProjects.androidxCompiler, defaultAndroidxCompilerEnvironment)) {
        gradle(":runDistributable").build().checks { check ->
            val actualMainImage = file("main-image.actual.png")
            val expectedMainImage = file("main-image.expected.png")
            assert(actualMainImage.readBytes().contentEquals(expectedMainImage.readBytes())) {
                "The actual image '$actualMainImage' does not match the expected image '$expectedMainImage'"
            }
        }
    }

    @Test
    fun kotlinDsl(): Unit = with(testProject(TestProjects.jvmKotlinDsl)) {
        gradle(":packageDistributionForCurrentOS", "--dry-run").build()
        gradle(":packageReleaseDistributionForCurrentOS", "--dry-run").build()
    }

    @Test
    fun proguard(): Unit = with(testProject(TestProjects.proguard)) {
        val enableObfuscation = """
                compose.desktop {
                    application {
                        buildTypes.release.proguard {
                            obfuscate.set(true)
                        }
                    }
                }
            """.trimIndent()

        val actualMainImage = file("main-image.actual.png")
        val expectedMainImage = file("main-image.expected.png")

        fun checkImageBeforeBuild() {
            assertFalse(actualMainImage.exists(), "'$actualMainImage' exists")
        }
        fun checkImageAfterBuild() {
            assert(actualMainImage.readBytes().contentEquals(expectedMainImage.readBytes())) {
                "The actual image '$actualMainImage' does not match the expected image '$expectedMainImage'"
            }
        }

        checkImageBeforeBuild()
        gradle(":runReleaseDistributable").build().checks { check ->
            check.taskOutcome(":proguardReleaseJars", TaskOutcome.SUCCESS)
            checkImageAfterBuild()
            assertEqualTextFiles(file("main-methods.actual.txt"), file("main-methods.expected.txt"))
        }

        file("build.gradle").modify { "$it\n$enableObfuscation" }
        actualMainImage.delete()
        checkImageBeforeBuild()
        gradle(":runReleaseDistributable").build().checks { check ->
            check.taskOutcome(":proguardReleaseJars", TaskOutcome.SUCCESS)
            checkImageAfterBuild()
            assertNotEqualTextFiles(file("main-methods.actual.txt"), file("main-methods.expected.txt"))
        }
    }

    @Test
    fun packageJvm() = with(testProject(TestProjects.jvm)) {
        testPackageJvmDistributions()
    }

    @Test
    fun gradleBuildCache() = with(testProject(TestProjects.jvm)) {
        modifyGradleProperties {
            setProperty("org.gradle.caching", "true")
        }
        modifyText("settings.gradle") {
            it + "\n" + """
                buildCache {
                    local {
                        directory = new File(rootDir, 'build-cache')
                    }
                }
            """.trimIndent()
        }

        val packagingTask = ":packageDistributionForCurrentOS"
        gradle(packagingTask).build().checks { check ->
            check.taskOutcome(packagingTask, TaskOutcome.SUCCESS)
        }

        gradle("clean", packagingTask).build().checks { check ->
            check.taskOutcome(":checkRuntime", TaskOutcome.FROM_CACHE)
            check.taskOutcome(packagingTask, TaskOutcome.SUCCESS)
        }
    }

    @Test
    fun packageMpp() = with(testProject(TestProjects.mpp)) {
        testPackageJvmDistributions()
    }

    private fun TestProject.testPackageJvmDistributions() {
        val result = gradle(":packageDistributionForCurrentOS").build()
        val ext = when (currentOS) {
            OS.Linux -> "deb"
            OS.Windows -> "msi"
            OS.MacOS -> "dmg"
        }
        val packageDir = file("build/compose/binaries/main/$ext")
        val packageDirFiles = packageDir.listFiles() ?: arrayOf()
        check(packageDirFiles.size == 1) {
            "Expected single package in $packageDir, got [${packageDirFiles.joinToString(", ") { it.name }}]"
        }
        val packageFile = packageDirFiles.single()

        if (currentOS == OS.Linux) {
            val expectedName = "test-package_1.0.0-1_amd64.$ext"
            check(packageFile.name.equals(expectedName, ignoreCase = true)) {
                "Expected '$expectedName' package in $packageDir, got '${packageFile.name}'"
            }
        } else {
            Assert.assertEquals(packageFile.name, "TestPackage-1.0.0.$ext", "Unexpected package name")
        }
        assertEquals(TaskOutcome.SUCCESS, result.task(":package${ext.uppercaseFirstChar()}")?.outcome)
        assertEquals(TaskOutcome.SUCCESS, result.task(":packageDistributionForCurrentOS")?.outcome)
    }

    @Test
    fun testJdk15() = with(customJdkProject(15)) {
        testPackageJvmDistributions()
    }
    @Test
    fun testJdk18() = with(customJdkProject(18)) {
        testPackageJvmDistributions()
    }

    @Test
    fun testJdk19() = with(customJdkProject(19)) {
        testPackageJvmDistributions()
    }

    private fun customJdkProject(javaVersion: Int): TestProject =
        testProject(TestProjects.jvm).apply {
            appendText("build.gradle") {
                """
                    compose.desktop.application {
                        javaHome = javaToolchains.launcherFor {
                            languageVersion.set(JavaLanguageVersion.of($javaVersion))
                        }.get().metadata.installationPath.asFile.absolutePath
                    }
                """.trimIndent()
            }
        }

    @Test
    fun packageUberJarForCurrentOSJvm() = with(testProject(TestProjects.jvm)) {
        testPackageUberJarForCurrentOS()
    }

    @Test
    fun packageUberJarForCurrentOSMpp() = with(testProject(TestProjects.mpp)) {
        testPackageUberJarForCurrentOS()
    }

    private fun TestProject.testPackageUberJarForCurrentOS() {
        gradle(":packageUberJarForCurrentOS").build().let { result ->
            assertEquals(TaskOutcome.SUCCESS, result.task(":packageUberJarForCurrentOS")?.outcome)

            val resultJarFile = file("build/compose/jars/TestPackage-${currentTarget.id}-1.0.0.jar")
            resultJarFile.checkExists()

            JarFile(resultJarFile).use { jar ->
                val mainClass = jar.manifest.mainAttributes.getValue("Main-Class")
                assertEquals("MainKt", mainClass, "Unexpected main class")

                jar.entries().toList().mapTo(HashSet()) { it.name }.apply {
                    checkContains("MainKt.class", "org/jetbrains/skiko/SkiaLayer.class")
                }
            }
        }
    }

    @Test
    fun testModuleClash() = with(testProject(TestProjects.moduleClashCli)) {
        gradle(":app:runDistributable").build().checks { check ->
            check.taskOutcome(":app:createDistributable", TaskOutcome.SUCCESS)
            check.taskOutcome(":app:runDistributable", TaskOutcome.SUCCESS)
            check.logContains("Called lib1#util()")
            check.logContains("Called lib2#util()")
        }
    }

    @Test
    fun testJavaLogger() = with(testProject(TestProjects.javaLogger)) {
        gradle(":runDistributable").build().checks { check ->
            check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)
            check.logContains("Compose Gradle plugin test log warning!")
        }
    }

    @Test
    fun testMacOptions() {
        fun String.normalized(): String =
            trim().replace(
                "Copyright (C) ${Calendar.getInstance().get(Calendar.YEAR)}",
                "Copyright (C) CURRENT_YEAR"
            )

        Assumptions.assumeTrue(currentOS == OS.MacOS)

        with(testProject(TestProjects.macOptions)) {
            gradle(":runDistributable").build().checks { check ->
                check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)
                check.logContains("Hello, from Mac OS!")
                val appDir = testWorkDir.resolve("build/compose/binaries/main/app/TestPackage.app/Contents/")
                val actualInfoPlist = appDir.resolve("Info.plist").checkExists()
                val expectedInfoPlist = testWorkDir.resolve("Expected-Info.Plist")
                val actualInfoPlistNormalized = actualInfoPlist.readText().normalized()
                val expectedInfoPlistNormalized = expectedInfoPlist.readText().normalized()
                Assert.assertEquals(actualInfoPlistNormalized, expectedInfoPlistNormalized)
            }
        }
    }

    @Test
    fun testMacSign() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)

        fun security(vararg args: Any): ProcessRunResult {
            val args = args.map {
                if (it is File) it.absolutePath else it.toString()
            }
            return runProcess(MacUtils.security, args)
        }

        fun withNewDefaultKeychain(newKeychain: File, fn: () -> Unit) {
            val originalKeychain =
                security("default-keychain")
                    .out
                    .trim()
                    .trim('"')

            try {
                security("default-keychain", "-s", newKeychain)
                fn()
            } finally {
                security("default-keychain", "-s", originalKeychain)
            }
        }

        with(testProject(TestProjects.macSign)) {
            val keychain = file("compose.test.keychain")
            val password = "compose.test"

            withNewDefaultKeychain(keychain) {
                security("default-keychain", "-s", keychain)
                security("unlock-keychain", "-p", password, keychain)

                gradle(":createDistributable").build().checks { check ->
                    check.taskOutcome(":createDistributable", TaskOutcome.SUCCESS)
                    val appDir = testWorkDir.resolve("build/compose/binaries/main/app/TestPackage.app/")
                    val result = runProcess(MacUtils.codesign, args = listOf("--verify", "--verbose", appDir.absolutePath))
                    val actualOutput = result.err.trim()
                    val expectedOutput = """
                        |${appDir.absolutePath}: valid on disk
                        |${appDir.absolutePath}: satisfies its Designated Requirement
                    """.trimMargin().trim()
                    Assert.assertEquals(expectedOutput, actualOutput)
                }

                gradle(":runDistributable").build().checks { check ->
                    check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)
                    check.logContains("Signed app successfully started!")
                }
            }
        }
    }

    @Test
    fun testOptionsWithSpaces() {
        with(testProject(TestProjects.optionsWithSpaces)) {
            fun testRunTask(runTask: String) {
                gradle(runTask).build().checks { check ->
                    check.taskOutcome(runTask, TaskOutcome.SUCCESS)
                    check.logContains("Running test options with spaces!")
                    check.logContains("Arg #1=Value 1!")
                    check.logContains("Arg #2=Value 2!")
                    check.logContains("JVM system property arg=Value 3!")
                }
            }

            testRunTask(":runDistributable")
            testRunTask(":run")

            gradle(":packageDistributionForCurrentOS").build().checks { check ->
                check.taskOutcome(":packageDistributionForCurrentOS", TaskOutcome.SUCCESS)
            }
        }
    }

    @Test
    fun testDefaultArgs() {
        with(testProject(TestProjects.defaultArgs)) {
            fun testRunTask(runTask: String) {
                gradle(runTask).build().checks { check ->
                    check.taskOutcome(runTask, TaskOutcome.SUCCESS)
                    check.logContains("compose.application.configure.swing.globals=true")
                }
            }

            testRunTask(":runDistributable")
            testRunTask(":run")

            gradle(":packageDistributionForCurrentOS").build().checks { check ->
                check.taskOutcome(":packageDistributionForCurrentOS", TaskOutcome.SUCCESS)
            }
        }
    }

    @Test
    fun testDefaultArgsOverride() {
        with(testProject(TestProjects.defaultArgsOverride)) {
            fun testRunTask(runTask: String) {
                gradle(runTask).build().checks { check ->
                    check.taskOutcome(runTask, TaskOutcome.SUCCESS)
                    check.logContains("compose.application.configure.swing.globals=false")
                }
            }

            testRunTask(":runDistributable")
            testRunTask(":run")

            gradle(":packageDistributionForCurrentOS").build().checks { check ->
                check.taskOutcome(":packageDistributionForCurrentOS", TaskOutcome.SUCCESS)
            }
        }
    }

    @Test
    fun testSuggestModules() {
        with(testProject(TestProjects.jvm)) {
            gradle(":suggestRuntimeModules").build().checks { check ->
                check.taskOutcome(":suggestRuntimeModules", TaskOutcome.SUCCESS)
                check.logContains("Suggested runtime modules to include:")
                check.logContains("modules(\"java.instrument\", \"jdk.unsupported\")")
            }
        }
    }

    @Test
    fun testUnpackSkiko() {
        with(testProject(TestProjects.unpackSkiko)) {
            gradle(":runDistributable").build().checks { check ->
                check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)

                val libraryPathPattern = "Read skiko library path: '(.*)'".toRegex()
                val m = libraryPathPattern.find(check.log)
                val skikoDir = m?.groupValues?.get(1)?.let(::File)
                if (skikoDir == null || !skikoDir.exists()) {
                    error("Invalid skiko path: $skikoDir")
                }
                val filesToFind = when (currentOS) {
                    OS.Linux -> listOf("libskiko-linux-${currentArch.id}.so")
                    OS.Windows -> listOf("skiko-windows-${currentArch.id}.dll", "icudtl.dat")
                    OS.MacOS -> listOf("libskiko-macos-${currentArch.id}.dylib")
                }
                for (fileName in filesToFind) {
                    skikoDir.resolve(fileName).checkExists()
                }
            }
        }
    }

    @Test
    fun resources() = with(testProject(TestProjects.resources)) {
        gradle(":run").build().checks { check ->
            check.taskOutcome(":run", TaskOutcome.SUCCESS)
        }

        gradle(":runDistributable").build().checks { check ->
            check.taskOutcome(":runDistributable", TaskOutcome.SUCCESS)
        }
    }
}
