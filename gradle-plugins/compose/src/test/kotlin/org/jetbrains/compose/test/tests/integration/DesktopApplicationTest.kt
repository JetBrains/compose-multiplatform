/*
 * Copyright 2020-2022 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.test.tests.integration

import org.gradle.internal.impldep.org.testng.Assert
import org.jetbrains.compose.internal.utils.MacUtils
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentArch
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.internal.utils.currentTarget
import org.jetbrains.compose.internal.utils.uppercaseFirstChar
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.JDK_11_BYTECODE_VERSION
import org.jetbrains.compose.test.utils.ProcessRunResult
import org.jetbrains.compose.test.utils.TestProject
import org.jetbrains.compose.test.utils.assertEqualTextFiles
import org.jetbrains.compose.test.utils.assertNotEqualTextFiles
import org.jetbrains.compose.test.utils.checkContains
import org.jetbrains.compose.test.utils.checkExists
import org.jetbrains.compose.test.utils.checkNotExists
import org.jetbrains.compose.test.utils.checks
import org.jetbrains.compose.test.utils.modify
import org.jetbrains.compose.test.utils.readClassFileVersion
import org.jetbrains.compose.test.utils.runProcess
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*
import java.util.jar.JarFile

class DesktopApplicationTest : GradlePluginTestBase() {
    @Test
    fun smokeTestRunTask() = with(testProject("application/jvm")) {
        file("build.gradle").modify {
            it + """
                afterEvaluate {
                    tasks.getByName("run").doFirst {
                        throw new StopExecutionException("Skip run task")
                    }
                    tasks.getByName("runRelease").doFirst {
                        throw new StopExecutionException("Skip runRelease task")
                    }
                    
                    tasks.getByName("runDistributable").doFirst {
                        throw new StopExecutionException("Skip runDistributable task")
                    }
                    tasks.getByName("runReleaseDistributable").doFirst {
                        throw new StopExecutionException("Skip runReleaseDistributable task")
                    }
                }
            """.trimIndent()
        }
        gradle("run").checks {
            check.taskSuccessful(":run")
        }
        gradle("runRelease").checks {
            check.taskSuccessful(":runRelease")
        }
        gradle("runDistributable").checks {
            check.taskSuccessful(":createDistributable")
            check.taskSuccessful(":runDistributable")
        }
        gradle("runReleaseDistributable").checks {
            check.taskSuccessful(":createReleaseDistributable")
            check.taskSuccessful(":runReleaseDistributable")
        }
    }

    @Test
    fun testRunMpp() = with(testProject("application/mpp")) {
        val logLine = "Kotlin MPP app is running!"
        gradle("run").checks {
            check.taskSuccessful(":run")
            check.logContains(logLine)
        }
        gradle("runRelease").checks {
            check.taskSuccessful(":runRelease")
            check.logContains(logLine)
        }
        gradle("runDistributable").checks {
            check.taskSuccessful(":createDistributable")
            check.taskSuccessful(":runDistributable")
            check.logContains(logLine)
        }
        gradle("runReleaseDistributable").checks {
            check.taskSuccessful(":createReleaseDistributable")
            check.taskSuccessful(":runReleaseDistributable")
            check.logContains(logLine)
        }
    }

    @Test
    fun kotlinDsl(): Unit = with(testProject("application/jvmKotlinDsl")) {
        gradle(":packageDistributionForCurrentOS", "--dry-run")
        gradle(":packageReleaseDistributionForCurrentOS", "--dry-run")
    }

    @Test
    fun proguard(): Unit = with(
        testProject(
            "application/proguard",
            testEnvironment = defaultTestEnvironment.copy(composeVerbose = false))
    ) {
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
        gradle(":runReleaseDistributable").checks {
            check.taskSuccessful(":proguardReleaseJars")
            checkImageAfterBuild()
            assertEqualTextFiles(file("main-methods.actual.txt"), file("main-methods.expected.txt"))
        }

        file("build.gradle").modify { "$it\n$enableObfuscation" }
        actualMainImage.delete()
        checkImageBeforeBuild()
        gradle(":runReleaseDistributable").checks {
            check.taskSuccessful(":proguardReleaseJars")
            checkImageAfterBuild()
            assertNotEqualTextFiles(file("main-methods.actual.txt"), file("main-methods.expected.txt"))
        }
    }

    @Test
    fun joinOutputJarsJvm() = with(testProject("application/jvm")) {
        joinOutputJars()
    }

    @Test
    fun joinOutputJarsMpp() = with(testProject("application/mpp")) {
        joinOutputJars()
    }

    private fun TestProject.joinOutputJars() {
        enableJoinOutputJars()
        gradle(":createReleaseDistributable").checks {
            check.taskSuccessful(":createReleaseDistributable")

            val distributionPathPattern = "The distribution is written to (.*)".toRegex()
            val m = distributionPathPattern.find(check.log)
            val distributionDir = m?.groupValues?.get(1)?.let(::File)
            if (distributionDir == null || !distributionDir.exists()) {
                error("Invalid distribution path: $distributionDir")
            }
            val appDirSubPath = when (currentOS) {
                OS.Linux -> "TestPackage/lib/app"
                OS.Windows -> "TestPackage/app"
                OS.MacOS -> "TestPackage.app/Contents/app"
            }
            val appDir = distributionDir.resolve(appDirSubPath)
            val jarsCount = appDir.listFiles()?.count { it.name.endsWith(".jar", ignoreCase = true) } ?: 0
            assert(jarsCount == 1)
        }
    }

    @Test
    fun gradleBuildCache() = with(testProject("application/jvm")) {
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
        gradle(packagingTask).checks {
            check.taskSuccessful(packagingTask)
        }

        gradle("clean", packagingTask).checks {
            check.taskFromCache(":checkRuntime")
            check.taskSuccessful(packagingTask)
        }
    }

    @Test
    fun packageJvm() = with(testProject("application/jvm")) {
        testPackageJvmDistributions()
    }

    @Test
    fun packageMpp() = with(testProject("application/mpp")) {
        testPackageJvmDistributions()
    }


    private fun TestProject.testPackageJvmDistributions() {
        val result = gradle(":packageDistributionForCurrentOS")

        val mainClass = file("build/classes").walk().single { it.isFile && it.name == "MainKt.class" }
        val bytecodeVersion = readClassFileVersion(mainClass)
        assertEquals(JDK_11_BYTECODE_VERSION, bytecodeVersion, "$mainClass bytecode version")

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
            // The default naming scheme was changed in JDK 18
            // https://bugs.openjdk.org/browse/JDK-8276084
            // This test might be used with different JDKs,
            // so as a workaround we check that the
            // package name is either one of two expected values.
            // TODO: Check a corresponding value for each JDK
            val possibleNames = listOf(
                "test-package_1.0.0-1_amd64.$ext",
                "test-package_1.0.0_amd64.$ext",
            )
            check(possibleNames.any { packageFile.name.equals(it, ignoreCase = true) }) {
                "Unexpected package name '${packageFile.name}' in $packageDir\n" +
                        "Possible names: ${possibleNames.joinToString(", ") { "'$it'" }}"
            }
        } else {
            Assert.assertEquals(packageFile.name, "TestPackage-1.0.0.$ext", "Unexpected package name")
        }
        result.checks {
            check.taskSuccessful(":package${ext.uppercaseFirstChar()}")
            check.taskSuccessful(":packageDistributionForCurrentOS")
        }
    }

    @Test
    fun testJdk19() = with(customJdkProject(19)) {
        testPackageJvmDistributions()
    }

    private fun customJdkProject(javaVersion: Int): TestProject =
        testProject("application/jvm").apply {
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
    fun packageUberJarForCurrentOSJvm() = with(testProject("application/jvm")) {
        testPackageUberJarForCurrentOS(false)
    }

    @Test
    fun packageUberJarForCurrentOSMpp() = with(testProject("application/mpp")) {
        testPackageUberJarForCurrentOS(false)
    }

    @Test
    fun packageReleaseUberJarForCurrentOSJvm() = with(testProject("application/jvm")) {
        testPackageUberJarForCurrentOS(true)
    }

    @Test
    fun packageReleaseUberJarForCurrentOSMpp() = with(testProject("application/mpp")) {
        testPackageUberJarForCurrentOS(true)
    }

    private fun TestProject.testPackageUberJarForCurrentOS(release: Boolean) {
        val task = when {
            release -> ":packageReleaseUberJarForCurrentOS"
            else -> ":packageUberJarForCurrentOS"
        }

        val jarFileName = when {
            release -> "build/compose/jars/TestPackage-${currentTarget.id}-1.0.0-release.jar"
            else -> "build/compose/jars/TestPackage-${currentTarget.id}-1.0.0.jar"
        }

        gradle(task).checks {
            check.taskSuccessful(task)

            val resultJarFile = file(jarFileName)
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
    fun testModuleClash() = with(testProject("application/moduleClashCli")) {
        gradle(":app:runDistributable").checks {
            check.taskSuccessful(":app:createDistributable")
            check.taskSuccessful(":app:runDistributable")
            check.logContains("Called lib1#util()")
            check.logContains("Called lib2#util()")
        }
    }

    @Test
    fun testJavaLogger() = with(testProject("application/javaLogger")) {
        gradle(":runDistributable").checks {
            check.taskSuccessful(":runDistributable")
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

        with(testProject("application/macOptions")) {
            gradle(":runDistributable").checks {
                check.taskSuccessful(":runDistributable")
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

    private fun macSignProject(
        identity: String,
        keychainFilename: String,
        javaVersion: String = "17"
    ) = testProject("application/macSign").apply {
        modifyText("build.gradle") {
            it
                .replace("%IDENTITY%", identity)
                .replace("%KEYCHAIN%", keychainFilename)
                .replace("%JAVA_VERSION%", javaVersion)
        }
    }

    @Test
    fun testMacSignConfiguration() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)

        with(macSignProject(identity = "Compose Test", keychainFilename = "compose.test.keychain")) {
            gradle("--dry-run", ":createDistributable")
        }
    }

    private fun testMacSign(
        identity: String,
        keychainFilename: String,
        keychainPassword: String,
        javaVersion: String = "17"
    ) {
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

        with(macSignProject(identity = identity, keychainFilename = keychainFilename, javaVersion = javaVersion)) {
            val keychain = file(keychainFilename)

            withNewDefaultKeychain(keychain) {
                security("default-keychain", "-s", keychain)
                security("unlock-keychain", "-p", keychainPassword, keychain)

                gradle(":createDistributable").checks {
                    check.taskSuccessful(":createDistributable")
                    val appDir = testWorkDir.resolve("build/compose/binaries/main/app/TestPackage.app/")
                    val result = runProcess(MacUtils.codesign, args = listOf("--verify", "--verbose", appDir.absolutePath))
                    val actualOutput = result.err.trim()
                    val expectedOutput = """
                        |${appDir.absolutePath}: valid on disk
                        |${appDir.absolutePath}: satisfies its Designated Requirement
                    """.trimMargin().trim()
                    Assert.assertEquals(expectedOutput, actualOutput)
                }

                gradle(":runDistributable").checks {
                    check.taskSuccessful(":runDistributable")
                    check.logContains("Signed app successfully started!")
                }
            }
        }
    }

    @Test
    @Disabled
    // the test does not work on CI and locally unless test keychain is opened manually
    fun testMacSign() {
        testMacSign(
            identity = "Compose Test",
            keychainFilename = "compose.test.keychain",
            keychainPassword = "compose.test"
        )
    }

    @Test
    @Disabled
    // the test does not work on CI and locally unless test keychain is opened manually
    fun testMacSignWithNonAsciiDeveloperId() {
        testMacSign(
            identity = "Cömpose Test",
            keychainFilename = "compose.test-non-ascii.keychain",
            keychainPassword = "compose.test",
            javaVersion = "21",  // https://bugs.openjdk.org/browse/JDK-8308042 fixed in JDK 21
        )
    }

    @Test
    fun testOptionsWithSpaces() {
        with(testProject("application/optionsWithSpaces")) {
            fun testRunTask(runTask: String) {
                gradle(runTask).checks {
                    check.taskSuccessful(runTask)
                    check.logContains("Running test options with spaces!")
                    check.logContains("Arg #1=Value 1!")
                    check.logContains("Arg #2=Value 2!")
                    check.logContains("JVM system property arg=Value 3!")
                }
            }

            testRunTask(":runDistributable")
            testRunTask(":run")

            gradle(":packageDistributionForCurrentOS").checks {
                check.taskSuccessful(":packageDistributionForCurrentOS")
            }
        }
    }

    @Test
    fun testDefaultArgs() {
        with(testProject("application/defaultArgs")) {
            fun testRunTask(runTask: String) {
                gradle(runTask).checks {
                    check.taskSuccessful(runTask)
                    check.logContains("compose.application.configure.swing.globals=true")
                }
            }

            testRunTask(":runDistributable")
            testRunTask(":run")

            gradle(":packageDistributionForCurrentOS").checks {
                check.taskSuccessful(":packageDistributionForCurrentOS")
            }
        }
    }

    @Test
    fun testDefaultArgsOverride() {
        with(testProject("application/defaultArgsOverride")) {
            fun testRunTask(runTask: String) {
                gradle(runTask).checks {
                    check.taskSuccessful(runTask)
                    check.logContains("compose.application.configure.swing.globals=false")
                }
            }

            testRunTask(":runDistributable")
            testRunTask(":run")

            gradle(":packageDistributionForCurrentOS").checks {
                check.taskSuccessful(":packageDistributionForCurrentOS")
            }
        }
    }

    @Test
    fun testSuggestModules() {
        with(testProject("application/jvm")) {
            gradle(":suggestRuntimeModules").checks {
                check.taskSuccessful(":suggestRuntimeModules")
                check.logContains("Suggested runtime modules to include:")
                check.logContains("modules(\"java.instrument\", \"jdk.unsupported\")")
            }
        }
    }

    @Test
    fun testUnpackSkiko() = with(testProject("application/unpackSkiko")) {
        testUnpackSkiko(":runDistributable")
    }

    @Test
    fun testUnpackSkikoFromUberJar() = with(testProject("application/unpackSkiko")) {
        enableJoinOutputJars()
        testUnpackSkiko(":runReleaseDistributable")
    }

    private fun TestProject.testUnpackSkiko(runDistributableTask: String) {
        gradle(runDistributableTask).checks {
            check.taskSuccessful(runDistributableTask)

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

    @Test
    fun resources() = with(testProject("application/resources")) {
        gradle(":run").checks {
            check.taskSuccessful(":run")
        }

        gradle(":runDistributable").checks {
            check.taskSuccessful(":runDistributable")
        }
    }

    @Test
    fun emptyResources() = with(testProject("application/emptyAppResources")) {
        gradle(":run").checks {
            check.taskSuccessful(":run")
        }

        gradle(":runDistributable").checks {
            check.taskSuccessful(":runDistributable")
        }
    }

    @Test
    fun testWixUnzip() {
        Assumptions.assumeTrue(currentOS == OS.Windows) { "The test is only relevant for Windows" }

        with(testProject("application/jvm")) {
            gradle(":unzipWix").checks {
                check.taskSuccessful(":unzipWix")

                file("build/wix311").checkExists()
                file("build/wix311/light.exe").checkExists()
                file("wix311").checkNotExists()
            }
        }
    }

    private fun TestProject.enableJoinOutputJars() {
        val enableJoinOutputJars = """
                    compose.desktop {
                        application {
                            buildTypes.release.proguard {
                                joinOutputJars.set(true)
                            }
                        }
                    }
                """.trimIndent()
        file("build.gradle").modify { "$it\n$enableJoinOutputJars" }
    }
}
