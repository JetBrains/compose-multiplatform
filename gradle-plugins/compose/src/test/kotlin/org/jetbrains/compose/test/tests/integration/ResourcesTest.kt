package org.jetbrains.compose.test.tests.integration

import org.gradle.util.GradleVersion
import org.jetbrains.compose.internal.utils.*
import org.jetbrains.compose.resources.XmlValuesConverterTask
import org.jetbrains.compose.test.utils.*
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.relativeTo
import kotlin.test.*

class ResourcesTest : GradlePluginTestBase() {
    @Test
    fun testGeneratedAccessors(): Unit = with(testProject("misc/commonResources")) {
        //check generated resource's accessors
        gradle("prepareKotlinIdeaImport").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                file("expected")
            )
        }

        //check resource's accessors were regenerated
        file("src/commonMain/composeResources/drawable/vector_2.xml").renameTo(
            file("src/commonMain/composeResources/drawable/vector_3.xml")
        )
        gradle("prepareKotlinIdeaImport").checks {
            assertNotEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/app/group/resources_test/generated/resources/Drawable0.commonMain.kt"),
                file("expected/commonMainResourceAccessors/app/group/resources_test/generated/resources/Drawable0.commonMain.kt")
            )
        }

        file("src/commonMain/composeResources/drawable-en").renameTo(
            file("src/commonMain/composeResources/drawable-rent")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                contains unknown qualifier: 'rent'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rent").renameTo(
            file("src/commonMain/composeResources/drawable-rUS-en")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Region qualifier must be declared after language: 'en-rUS'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rUS-en").renameTo(
            file("src/commonMain/composeResources/drawable-rUS")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Region qualifier must be used only with language.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rUS").renameTo(
            file("src/commonMain/composeResources/drawable-en-fr")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                contains repetitive qualifiers: 'en' and 'fr'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-en-fr").renameTo(
            file("src/commonMain/composeResources/image")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Unknown resource type: 'image'
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/image").renameTo(
            file("src/commonMain/composeResources/files-de")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                The 'files' directory doesn't support qualifiers: 'files-de'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/files-de").renameTo(
            file("src/commonMain/composeResources/strings")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Unknown resource type: 'strings'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/strings").renameTo(
            file("src/commonMain/composeResources/string-us")
        )
        gradle("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Forbidden directory name 'string-us'! String resources should be declared in 'values/strings.xml'.
            """.trimIndent()
            )
        }

        //restore defaults
        file("src/commonMain/composeResources/string-us").renameTo(
            file("src/commonMain/composeResources/drawable-en")
        )
        file("src/commonMain/composeResources/drawable/vector_3.xml").renameTo(
            file("src/commonMain/composeResources/drawable/vector_2.xml")
        )

        file("build.gradle.kts").modify { txt ->
            txt + """
                compose.resources {
                    publicResClass = true
                    packageOfResClass = "my.lib.res"
                }
            """.trimIndent()
        }

        gradle("prepareKotlinIdeaImport").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                file("expected-open-res")
            )
        }
    }

    @Test
    fun testMultiModuleResources() {
        val environment = defaultTestEnvironment.copy(
            kotlinVersion = "2.0.0-Beta5"
        )
        with(
            testProject("misc/kmpResourcePublication", environment)
        ) {
            if (environment.parsedGradleVersion < GradleVersion.version("7.6")) {
                val output = gradle(":tasks").output
                output.contains("Compose resources publication requires Gradle >= 7.6")
                output.contains("Current Kotlin Gradle Plugin is ${environment.gradleVersion}")
                return@with
            }

            gradle(":cmplib:publishAllPublicationsToMavenRepository").checks {
                val resDir = file("cmplib/src/commonMain/composeResources")
                val resourcesFiles = resDir.walkTopDown()
                    .filter { !it.isDirectory && !it.isHidden }
                    .getConvertedResources(resDir)
                val subdir = "me.sample.library.resources"

                fun libpath(target: String, ext: String) =
                    "my-mvn/me/sample/library/cmplib-$target/1.0/cmplib-$target-1.0$ext"

                val aar = file(libpath("android", ".aar"))
                val innerClassesJar = aar.parentFile.resolve("aar-inner-classes.jar")
                assertTrue(aar.exists(), "File not found: " + aar.path)
                ZipFile(aar).use { zip ->
                    resourcesFiles
                        .filter { it.startsWith("font") }
                        .forEach { fontRes ->
                            assertNotNull(
                                zip.getEntry("assets/composeResources/$subdir/$fontRes"),
                                "Resource not found: '$fontRes' in aar '${aar.path}'"
                            )
                        }

                    innerClassesJar.writeBytes(
                        zip.getInputStream(zip.getEntry("classes.jar")).readBytes()
                    )
                }
                ZipFile(innerClassesJar).use { zip ->
                    resourcesFiles
                        .filterNot { it.startsWith("font") }
                        .forEach { res ->
                            assertNotNull(
                                zip.getEntry("composeResources/$subdir/$res"),
                                "Resource not found: '$res' in aar/classes.jar '${aar.path}'"
                            )
                        }
                }

                val jar = file(libpath("jvm", ".jar"))
                checkResourcesZip(jar, resourcesFiles, subdir)

                if (currentOS == OS.MacOS) {
                    val iosx64ResZip = file(libpath("iosx64", "-kotlin_resources.kotlin_resources.zip"))
                    checkResourcesZip(iosx64ResZip, resourcesFiles, subdir)
                    val iosarm64ResZip = file(libpath("iosarm64", "-kotlin_resources.kotlin_resources.zip"))
                    checkResourcesZip(iosarm64ResZip, resourcesFiles, subdir)
                    val iossimulatorarm64ResZip = file(
                        libpath("iossimulatorarm64", "-kotlin_resources.kotlin_resources.zip")
                    )
                    checkResourcesZip(iossimulatorarm64ResZip, resourcesFiles, subdir)
                }
                val jsResZip = file(libpath("js", "-kotlin_resources.kotlin_resources.zip"))
                checkResourcesZip(jsResZip, resourcesFiles, subdir)
                val wasmjsResZip = file(libpath("wasm-js", "-kotlin_resources.kotlin_resources.zip"))
                checkResourcesZip(wasmjsResZip, resourcesFiles, subdir)
            }

            file("settings.gradle.kts").modify { content ->
                content.replace("//include(\":appModule\")", "include(\":appModule\")")
            }

            gradle(":appModule:jvmTest", "-i")

            if (currentOS == OS.MacOS) {
                val iosTask = if (currentArch == Arch.X64) {
                    ":appModule:iosX64Test"
                } else {
                    ":appModule:iosSimulatorArm64Test"
                }
                gradle(iosTask)
            }

            file("featureModule/src/commonMain/kotlin/me/sample/app/Feature.kt").modify { content ->
                content.replace(
                    "Text(txt + stringResource(Res.string.str_1), modifier)",
                    "Text(stringResource(Res.string.str_1), modifier)"
                )
            }

            gradleFailure(":appModule:jvmTest").checks {
                check.logContains("java.lang.AssertionError: Failed to assert the following: (Text + EditableText = [test text: Feature text str_1])")
                check.logContains("Text = '[Feature text str_1]'")
            }
        }
    }

    private fun checkResourcesZip(zipFile: File, resourcesFiles: Sequence<String>, subdir: String) {
        assertTrue(zipFile.exists(), "File not found: " + zipFile.path)
        ZipFile(zipFile).use { zip ->
            resourcesFiles.forEach { res ->
                assertNotNull(
                    zip.getEntry("composeResources/$subdir/$res"),
                    "Resource not found: '$res' in zip '${zipFile.path}'"
                )
            }
        }
    }

    @Test
    fun testFinalArtefacts(): Unit = with(testProject("misc/commonResources")) {
        //https://developer.android.com/build/build-variants?utm_source=android-studio#product-flavors
        file("build.gradle.kts").appendText(
            """
            
            kotlin {
                js {
                    browser {
                        testTask(Action {
                            enabled = false
                        })
                    }
                    binaries.executable()
                }
            }
            
            android {
                flavorDimensions += "version"
                productFlavors {
                    create("demo")
                    create("full")
                }
            }
        """.trimIndent()
        )
        file("src/androidDemoDebug/composeResources/files/platform.txt").writeNewFile("android demo-debug")
        file("src/androidDemoRelease/composeResources/files/platform.txt").writeNewFile("android demo-release")
        file("src/androidFullDebug/composeResources/files/platform.txt").writeNewFile("android full-debug")
        file("src/androidFullRelease/composeResources/files/platform.txt").writeNewFile("android full-release")
        file("src/desktopMain/composeResources/files/platform.txt").writeNewFile("desktop")
        file("src/jsMain/composeResources/files/platform.txt").writeNewFile("js")

        val commonResourcesDir = file("src/commonMain/composeResources")
        val commonResourcesFiles = commonResourcesDir.walkTopDown()
            .filter { !it.isDirectory && !it.isHidden }
            .getConvertedResources(commonResourcesDir)

        gradle("build").checks {
            check.taskSuccessful(":copyDemoDebugFontsToAndroidAssets")
            check.taskSuccessful(":copyDemoReleaseFontsToAndroidAssets")
            check.taskSuccessful(":copyFullDebugFontsToAndroidAssets")
            check.taskSuccessful(":copyFullReleaseFontsToAndroidAssets")

            getAndroidApk("demo", "debug", "Resources-Test").let { apk ->
                checkResourcesInZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android demo-debug",
                    readFileInZip(apk, "files/platform.txt").decodeToString()
                )
            }
            getAndroidApk("demo", "release", "Resources-Test").let { apk ->
                checkResourcesInZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android demo-release",
                    readFileInZip(apk, "files/platform.txt").decodeToString()
                )
            }
            getAndroidApk("full", "debug", "Resources-Test").let { apk ->
                checkResourcesInZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android full-debug",
                    readFileInZip(apk, "files/platform.txt").decodeToString()
                )
            }
            getAndroidApk("full", "release", "Resources-Test").let { apk ->
                checkResourcesInZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android full-release",
                    readFileInZip(apk, "files/platform.txt").decodeToString()
                )
            }

            file("build/libs/Resources-Test-desktop.jar").let { jar ->
                checkResourcesInZip(jar, commonResourcesFiles, false)
                assertEquals(
                    "desktop",
                    readFileInZip(jar, "files/platform.txt").decodeToString()
                )
            }

            val jsBuildDir = file("build/dist/js/productionExecutable")
            commonResourcesFiles.forEach { res ->
                assertTrue(jsBuildDir.resolve(res).exists())
            }
            assertEquals("js", jsBuildDir.resolve("files/platform.txt").readText())
        }
    }

    @Test
    fun testAndroidFonts(): Unit = with(testProject("misc/commonResources")) {
        val commonResourcesDir = file("src/commonMain/composeResources")
        val commonResourcesFiles = commonResourcesDir.walkTopDown()
            .filter { !it.isDirectory && !it.isHidden }
            .getConvertedResources(commonResourcesDir)

        gradle("assembleDebug").checks {
            check.taskSuccessful(":copyDebugFontsToAndroidAssets")

            getAndroidApk("", "debug", "Resources-Test").let { apk ->
                checkResourcesInZip(apk, commonResourcesFiles, true)
            }
        }

        file("src/commonMain/composeResources/font-en").renameTo(
            file("src/commonMain/composeResources/font-mdpi")
        )
        val newCommonResourcesFiles = commonResourcesDir.walkTopDown()
            .filter { !it.isDirectory && !it.isHidden }
            .getConvertedResources(commonResourcesDir)
        gradle("assembleDebug").checks {
            check.taskSuccessful(":copyDebugFontsToAndroidAssets")

            getAndroidApk("", "debug", "Resources-Test").let { apk ->
                checkResourcesInZip(apk, newCommonResourcesFiles, true)
            }
        }
    }

    private fun Sequence<File>.getConvertedResources(baseDir: File) = map { file ->
        val newFile = if (
            file.parentFile.name.startsWith("value") &&
            file.extension.equals("xml", true)
        ) {
            val cvrSuffix = file.parentFile.parentFile.parentFile.name
            file.parentFile.resolve("${file.nameWithoutExtension}.$cvrSuffix.${XmlValuesConverterTask.CONVERTED_RESOURCE_EXT}")
        } else {
            file
        }
        newFile.relativeTo(baseDir).invariantSeparatorsPath
    }


    private fun File.writeNewFile(text: String) {
        parentFile.mkdirs()
        createNewFile()
        writeText(text)
    }

    private fun TestProject.getAndroidApk(flavor: String, type: String, name: String): File {
        return if (flavor.isNotEmpty()) {
            file("build/outputs/apk/$flavor/$type/$name-$flavor-$type.apk")
        } else {
            file("build/outputs/apk/$type/$name-$type.apk")
        }
    }

    private fun checkResourcesInZip(file: File, commonResourcesFiles: Sequence<String>, isAndroid: Boolean) {
        println("check ZIP: '${file.path}'")
        assertTrue(file.exists())
        ZipFile(file).use { zip ->
            commonResourcesFiles.forEach { res ->
                println("check '$res' file")
                if (isAndroid && res.startsWith("font")) {
                    //android fonts should be only in assets
                    assertNull(zip.getEntry(res), "file = '$res'")
                    assertNotNull(zip.getEntry("assets/$res"), "file = 'assets/$res'")
                } else {
                    assertNotNull(zip.getEntry(res), "file = '$res'")
                }
            }
        }
    }

    private fun readFileInZip(file: File, path: String): ByteArray = ZipFile(file).use { zip ->
        val platformTxt = zip.getEntry(path)
        assertNotNull(platformTxt, "file = '$path'")
        zip.getInputStream(platformTxt).readBytes()
    }

    @Test
    fun testUpToDateChecks(): Unit = with(testProject("misc/commonResources")) {
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertTrue(file("build/generated/compose/resourceGenerator/kotlin/commonResClass/app/group/resources_test/generated/resources/Res.kt").exists())
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskUpToDate(":generateComposeResClass")
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "api(compose.components.resources)",
                "//api(compose.components.resources)"
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertFalse(file("build/generated/compose/resourceGenerator/kotlin/commonResClass/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "//api(compose.components.resources)",
                "api(compose.components.resources)"
            )
        }
        modifyText("build.gradle.kts") { str ->
            str.replace(
                "group = \"app.group\"",
                "group = \"io.company\""
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertFalse(file("build/generated/compose/resourceGenerator/kotlin/commonResClass/app/group/resources_test/generated/resources/Res.kt").exists())
            assertTrue(file("build/generated/compose/resourceGenerator/kotlin/commonResClass/io/company/resources_test/generated/resources/Res.kt").exists())
        }
    }

    @Test
    fun testEmptyResClass(): Unit = with(testProject("misc/emptyResources")) {
        gradle("generateComposeResClass").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin/commonResClass/app/group/empty_res/generated/resources"),
                file("expected")
            )
        }
    }

    @Test
    fun testJvmOnlyProject(): Unit = with(testProject("misc/jvmOnlyResources")) {
        gradle("jar").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                file("expected")
            )
        }
    }

    //https://github.com/gmazzo/gradle-buildconfig-plugin/issues/131
    @Test
    fun testBundledKotlinPoet(): Unit = with(testProject("misc/bundledKotlinPoet")) {
        gradle("generateBuildConfig")
    }

    private fun assertDirectoriesContentEquals(actual: File, expected: File) {
        require(expected.isDirectory)
        require(actual.isDirectory)
        assertEquals(expected.exists(), actual.exists())

        val expectedPath = expected.toPath()
        val actualPath = actual.toPath()
        expected.walkTopDown().forEach { expectedFile ->
            if (!expectedFile.isDirectory) {
                val actualFile = actualPath.resolve(expectedFile.toPath().relativeTo(expectedPath)).toFile()
                assertEqualTextFiles(actualFile, expectedFile)
            }
        }

        val expectedFilesCount = expected.walkTopDown()
            .filter { !it.isDirectory }
            .map { it.toPath().relativeTo(expectedPath) }.sorted().joinToString("\n")
        val actualFilesCount = actual.walkTopDown()
            .filter { !it.isDirectory }
            .map { it.toPath().relativeTo(actualPath) }.sorted().joinToString("\n")
        assertEquals(expectedFilesCount, actualFilesCount)
    }

    @Test
    fun testResourcesTaskDisabled() = with(testProject("misc/commonResources")) {
        file("build.gradle.kts").appendText(
            """
                compose {
                    resources {
                        generateResClass = never
                    }
                }
            """.trimIndent()
        )
        gradle("generateComposeResClass").checks {
            check.logContains("Generation Res class is disabled")
        }
    }

    @Test
    fun iosResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        val iosEnv = mapOf(
            "PLATFORM_NAME" to "iphonesimulator",
            "ARCHS" to "arm64",
            "CONFIGURATION" to "Debug",
        )
        val testEnv = defaultTestEnvironment.copy(
            additionalEnvVars = iosEnv
        )

        with(TestProject("misc/iosResources", testEnv)) {
            gradle(
                ":syncFramework",
                "-Pkotlin.native.cocoapods.platform=${iosEnv["PLATFORM_NAME"]}",
                "-Pkotlin.native.cocoapods.archs=${iosEnv["ARCHS"]}",
                "-Pkotlin.native.cocoapods.configuration=${iosEnv["CONFIGURATION"]}",
                "--dry-run"
            ).checks {
                check.taskSkipped(":generateComposeResClass")

                check.taskSkipped(":convertXmlValueResourcesForCommonMain")
                check.taskSkipped(":copyNonXmlValueResourcesForCommonMain")
                check.taskSkipped(":prepareComposeResourcesTaskForCommonMain")
                check.taskSkipped(":generateResourceAccessorsForCommonMain")

                check.taskSkipped(":convertXmlValueResourcesForNativeMain")
                check.taskSkipped(":copyNonXmlValueResourcesForNativeMain")
                check.taskSkipped(":prepareComposeResourcesTaskForNativeMain")
                check.taskSkipped(":generateResourceAccessorsForNativeMain")

                check.taskSkipped(":convertXmlValueResourcesForAppleMain")
                check.taskSkipped(":copyNonXmlValueResourcesForAppleMain")
                check.taskSkipped(":prepareComposeResourcesTaskForAppleMain")
                check.taskSkipped(":generateResourceAccessorsForAppleMain")

                check.taskSkipped(":convertXmlValueResourcesForIosMain")
                check.taskSkipped(":copyNonXmlValueResourcesForIosMain")
                check.taskSkipped(":prepareComposeResourcesTaskForIosMain")
                check.taskSkipped(":generateResourceAccessorsForIosMain")

                check.taskSkipped(":convertXmlValueResourcesForIosX64Main")
                check.taskSkipped(":copyNonXmlValueResourcesForIosX64Main")
                check.taskSkipped(":prepareComposeResourcesTaskForIosX64Main")
                check.taskSkipped(":generateResourceAccessorsForIosX64Main")

                check.taskSkipped(":syncPodComposeResourcesForIos")
            }
            gradle(":syncPodComposeResourcesForIos").checks {
                check.taskNoSource(":convertXmlValueResourcesForCommonMain")
                check.taskSuccessful(":copyNonXmlValueResourcesForCommonMain")
                check.taskSuccessful(":prepareComposeResourcesTaskForCommonMain")
                check.taskSkipped(":generateResourceAccessorsForCommonMain")

                check.taskNoSource(":convertXmlValueResourcesForNativeMain")
                check.taskNoSource(":copyNonXmlValueResourcesForNativeMain")
                check.taskNoSource(":prepareComposeResourcesTaskForNativeMain")
                check.taskSkipped(":generateResourceAccessorsForNativeMain")

                check.taskNoSource(":convertXmlValueResourcesForAppleMain")
                check.taskNoSource(":copyNonXmlValueResourcesForAppleMain")
                check.taskNoSource(":prepareComposeResourcesTaskForAppleMain")
                check.taskSkipped(":generateResourceAccessorsForAppleMain")

                check.taskNoSource(":convertXmlValueResourcesForIosMain")
                check.taskSuccessful(":copyNonXmlValueResourcesForIosMain")
                check.taskSuccessful(":prepareComposeResourcesTaskForIosMain")
                check.taskSkipped(":generateResourceAccessorsForIosMain")

                check.taskNoSource(":convertXmlValueResourcesForIosX64Main")
                check.taskNoSource(":copyNonXmlValueResourcesForIosX64Main")
                check.taskNoSource(":prepareComposeResourcesTaskForIosX64Main")
                check.taskSkipped(":generateResourceAccessorsForIosX64Main")

                file("build/compose/ios/shared/compose-resources/drawable/compose-multiplatform.xml").checkExists()
                file("build/compose/ios/shared/compose-resources/drawable/icon.xml").checkExists()
            }
        }
    }

    @Test
    fun iosTestResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(testProject("misc/iosResources")) {
            gradle(":linkDebugTestIosX64", "--dry-run").checks {
                check.taskSkipped(":copyTestComposeResourcesForIosX64")
                check.taskSkipped(":linkDebugTestIosX64")
            }
            gradle(":copyTestComposeResourcesForIosX64").checks {
                file("build/bin/iosX64/debugTest/compose-resources/drawable/compose-multiplatform.xml").checkExists()
                file("build/bin/iosX64/debugTest/compose-resources/drawable/icon.xml").checkExists()
            }
        }
    }
}