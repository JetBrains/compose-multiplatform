package org.jetbrains.compose.test.tests.integration

import org.gradle.util.GradleVersion
import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.Version
import org.jetbrains.compose.internal.utils.Arch
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentArch
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.resources.XmlValuesConverterTask
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProject
import org.jetbrains.compose.test.utils.assertEqualTextFiles
import org.jetbrains.compose.test.utils.assertNotEqualTextFiles
import org.jetbrains.compose.test.utils.checkExists
import org.jetbrains.compose.test.utils.checks
import org.jetbrains.compose.test.utils.modify
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.relativeTo
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResourcesTest : GradlePluginTestBase() {
    @Test
    fun testSafeImport() {
        with(testProject("misc/commonResources")) {
            file("src/commonMain/composeResources/drawable-en").renameTo(
                file("src/commonMain/composeResources/drawable-rent")
            )
            gradleFailure("prepareKotlinIdeaImport").checks {
                check.logContains("e: generateResourceAccessorsForCommonMain task was failed:")
                check.logContains("contains unknown qualifier: 'rent'.")
            }

            gradle("prepareKotlinIdeaImport", "-Didea.sync.active=true").checks {
                check.logContains("e: generateResourceAccessorsForCommonMain task was failed:")
                check.logContains("contains unknown qualifier: 'rent'.")
            }
        }
    }

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
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                contains unknown qualifier: 'rent'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rent").renameTo(
            file("src/commonMain/composeResources/drawable-rUS-en")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Region qualifier must be declared after language: 'en-rUS'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rUS-en").renameTo(
            file("src/commonMain/composeResources/drawable-rUS")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Region qualifier must be used only with language.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rUS").renameTo(
            file("src/commonMain/composeResources/drawable-en-fr")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                contains repetitive qualifiers: 'en' and 'fr'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-en-fr").renameTo(
            file("src/commonMain/composeResources/image")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Unknown resource type: 'image'
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/image").renameTo(
            file("src/commonMain/composeResources/files-de")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                The 'files' directory doesn't support qualifiers: 'files-de'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/files-de").renameTo(
            file("src/commonMain/composeResources/strings")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains(
                """
                Unknown resource type: 'strings'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/strings").renameTo(
            file("src/commonMain/composeResources/string-us")
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
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

        val testXml = file("src/commonMain/composeResources/values/test.xml")
        testXml.writeText("")
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains("${testXml.name} is not valid. Check the file content.")
        }

        testXml.writeText("invalid")
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains("${testXml.name} is not valid. Check the file content.")
        }

        testXml.writeText(
            """
            <resources>
                <aaa name="v">aaa</aaa>
            </resources>
        """.trimIndent()
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains("${testXml.name} is not valid. Unknown resource type: 'aaa'.")
        }

        testXml.writeText(
            """
            <resources>
                <drawable name="v">aaa</drawable>
            </resources>
        """.trimIndent()
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains("${testXml.name} is not valid. Unknown string resource type: 'drawable'.")
        }

        testXml.writeText(
            """
            <resources>
                <string name="v1">aaa</string>
                <string name="v2">aaa</string>
                <string name="v3">aaa</string>
                <string name="v1">aaa</string>
            </resources>
        """.trimIndent()
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains("${testXml.name} is not valid. Duplicated key 'v1'.")
        }

        testXml.writeText(
            """
            <resources>
                <string name="v1">aaa</string>
                <string foo="v2">aaa</string>
            </resources>
        """.trimIndent()
        )
        gradleFailure("prepareKotlinIdeaImport").checks {
            check.logContains("${testXml.name} is not valid. Attribute 'name' not found.")
        }
        testXml.delete()

        file("build.gradle.kts").modify { txt ->
            txt + """
                compose.resources {
                    publicResClass = true
                    packageOfResClass = "my.lib.res"
                    nameOfResClass = "MyRes"
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
        val environment = defaultTestEnvironment
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
                check.logContains("Configure multi-module compose resources")

                val resDir = file("cmplib/src/commonMain/composeResources")
                val resourcesFiles = resDir.walkTopDown()
                    .filter { !it.isDirectory && !it.isHidden }
                    .getConvertedResources(resDir, "composeResources/me.sample.library.resources")

                fun libpath(target: String, ext: String) =
                    "my-mvn/me/sample/library/cmplib-$target/1.0/cmplib-$target-1.0$ext"

                val aar = file(libpath("android", ".aar"))
                checkResourcesZip(aar, resourcesFiles, true)

                val jar = file(libpath("jvm", ".jar"))
                checkResourcesZip(jar, resourcesFiles, false)

                if (currentOS == OS.MacOS) {
                    val iosx64ResZip = file(libpath("iosx64", "-kotlin_resources.kotlin_resources.zip"))
                    checkResourcesZip(iosx64ResZip, resourcesFiles, false)
                    val iosarm64ResZip = file(libpath("iosarm64", "-kotlin_resources.kotlin_resources.zip"))
                    checkResourcesZip(iosarm64ResZip, resourcesFiles, false)
                    val iossimulatorarm64ResZip = file(
                        libpath("iossimulatorarm64", "-kotlin_resources.kotlin_resources.zip")
                    )
                    checkResourcesZip(iossimulatorarm64ResZip, resourcesFiles, false)

                    val macosx64ResZip =
                        file(libpath("macosx64", "-kotlin_resources.kotlin_resources.zip"))
                    checkResourcesZip(macosx64ResZip, resourcesFiles, false)
                    val macosarm64ResZip =
                        file(libpath("macosarm64", "-kotlin_resources.kotlin_resources.zip"))
                    checkResourcesZip(macosarm64ResZip, resourcesFiles, false)
                }
                val jsResZip = file(libpath("js", "-kotlin_resources.kotlin_resources.zip"))
                checkResourcesZip(jsResZip, resourcesFiles, false)
                val wasmjsResZip = file(libpath("wasm-js", "-kotlin_resources.kotlin_resources.zip"))
                checkResourcesZip(wasmjsResZip, resourcesFiles, false)
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

                val macosTask = if (currentArch == Arch.X64) {
                    ":appModule:macosX64Test"
                } else {
                    ":appModule:macosArm64Test"
                }
                gradle(macosTask)
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

    @Test
    fun testNewAgpResources() {
        Assumptions.assumeTrue(defaultTestEnvironment.parsedGradleVersion >= GradleVersion.version("8.10.2"))

        val agpVersion = Version.fromString(defaultTestEnvironment.agpVersion)
        Assumptions.assumeTrue(agpVersion >= Version.fromString("8.8.0-alpha08"))

        with(testProject("misc/newAgpResources", defaultTestEnvironment)) {
            gradle(":appModule:assembleDebug").checks {
                if (agpVersion >= Version.fromString("8.10")) {
                    check.logContains("Configure compose resources with KotlinMultiplatformAndroidComponentsExtension")
                } else {
                    check.logContains("Configure compose resources with outdated KotlinMultiplatformAndroidComponentsExtension < 8.10")
                }

                val resourcesFiles = sequenceOf(
                    "composeResources/newagpresources.appmodule.generated.resources/values/strings.commonMain.cvr",
                    "composeResources/newagpresources.featuremodule.generated.resources/values/strings.commonMain.cvr"
                )
                val apk = file("appModule/build/outputs/apk/debug/appModule-debug.apk")
                checkResourcesZip(apk, resourcesFiles, true)
            }
        }
    }

    @Test
    fun testDisableMultimoduleResources() {
        with(testProject("misc/commonResources")) {
            file("gradle.properties").modify { content ->
                content + "\n" + ComposeProperties.DISABLE_MULTIMODULE_RESOURCES + "=true"
            }
            gradle("desktopJar").checks {
                check.logContains("Configure single-module compose resources")

                val resDir = file("src/commonMain/composeResources")
                val resourcesFiles = resDir.walkTopDown()
                    .filter { !it.isDirectory && !it.isHidden }
                    .getConvertedResources(resDir, "")

                val jar = file("build/libs/Resources-Test-desktop.jar")
                checkResourcesZip(jar, resourcesFiles, false)
            }
        }
    }

    private fun checkResourcesZip(zipFile: File, resourcesFiles: Sequence<String>, isAndroid: Boolean) {
        println("check ZIP: '${zipFile.path}'")
        assertTrue(zipFile.exists(), "File not found: " + zipFile.path)
        ZipFile(zipFile).use { zip ->
            resourcesFiles.forEach { res ->
                println("check '$res' file")
                if (isAndroid) {
                    //android resources should be only in assets
                    assertNull(zip.getEntry(res), "file = '$res'")
                    assertNotNull(zip.getEntry("assets/$res"), "file = 'assets/$res'")
                } else {
                    assertNotNull(zip.getEntry(res), "file = '$res'")
                }
            }
        }
    }


    @Test
    fun testJvmBuildWithResourcesCustomization(): Unit = with(testProject("misc/commonResources")) {
        file("build.gradle.kts").appendText(
            """
                compose.resources {
                    publicResClass = true
                    packageOfResClass = "io.customized"
                    nameOfResClass = "CustomName"
                }
            """.trimIndent()
        )
        file("src/commonMain/kotlin").deleteRecursively() //empty project

        gradle("desktopJar").checks {
            check.logContains("Generate CustomName.kt")
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
        val repackDir = "composeResources/app.group.resources_test.generated.resources"
        val commonResourcesFiles = commonResourcesDir.walkTopDown()
            .filter { !it.isDirectory && !it.isHidden }
            .getConvertedResources(commonResourcesDir, repackDir)

        gradle("build").checks {
            check.taskSuccessful(":copyDemoDebugComposeResourcesToAndroidAssets")
            check.taskSuccessful(":copyDemoReleaseComposeResourcesToAndroidAssets")
            check.taskSuccessful(":copyFullDebugComposeResourcesToAndroidAssets")
            check.taskSuccessful(":copyFullReleaseComposeResourcesToAndroidAssets")

            getAndroidApk("demo", "debug", "Resources-Test").let { apk ->
                checkResourcesZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android demo-debug",
                    readFileInZip(apk, "assets/$repackDir/files/platform.txt").decodeToString()
                )
            }
            getAndroidApk("demo", "release", "Resources-Test").let { apk ->
                checkResourcesZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android demo-release",
                    readFileInZip(apk, "assets/$repackDir/files/platform.txt").decodeToString()
                )
            }
            getAndroidApk("full", "debug", "Resources-Test").let { apk ->
                checkResourcesZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android full-debug",
                    readFileInZip(apk, "assets/$repackDir/files/platform.txt").decodeToString()
                )
            }
            getAndroidApk("full", "release", "Resources-Test").let { apk ->
                checkResourcesZip(apk, commonResourcesFiles, true)
                assertEquals(
                    "android full-release",
                    readFileInZip(apk, "assets/$repackDir/files/platform.txt").decodeToString()
                )
            }

            file("build/libs/Resources-Test-desktop.jar").let { jar ->
                checkResourcesZip(jar, commonResourcesFiles, false)
                assertEquals(
                    "desktop",
                    readFileInZip(jar, "$repackDir/files/platform.txt").decodeToString()
                )
            }

            val jsBuildDir = file("build/dist/js/productionExecutable")
            commonResourcesFiles.forEach { res ->
                assertTrue(jsBuildDir.resolve(res).exists())
            }
            assertEquals("js", jsBuildDir.resolve("$repackDir/files/platform.txt").readText())
        }
    }

    private fun Sequence<File>.getConvertedResources(baseDir: File, repackDir: String) = map { file ->
        val newFile = if (
            file.parentFile.name.startsWith("value") &&
            file.extension.equals("xml", true)
        ) {
            val cvrSuffix = file.parentFile.parentFile.parentFile.name
            file.parentFile.resolve("${file.nameWithoutExtension}.$cvrSuffix.${XmlValuesConverterTask.CONVERTED_RESOURCE_EXT}")
        } else {
            file
        }
        Path(repackDir, newFile.relativeTo(baseDir).path).invariantSeparatorsPathString
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
            check.taskSkipped(":generateComposeResClass")
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
        gradle("prepareKotlinIdeaImport").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                file("expected")
            )
        }
    }

    @Test
    fun testGeneratedAccessorsAnnotatedWithResourceContentHash(): Unit = with(testProject("misc/commonResources")) {
        //check generated resource's accessors
        gradle("prepareKotlinIdeaImport", "-Dcompose.resources.generate.ResourceContentHash.annotation=true").checks {
            val expected = if (System.getProperty("os.name").lowercase().contains("windows")) {
                // Windows has different line endings in comparison with Unixes,
                // thus the XML resource files differ and produce different content hashes,
                // so we have different test data for it.
                file("expected-with-hash-windows")
            } else {
                file("expected-with-hash")
            }

            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                expected
            )
        }
    }

    @Test
    fun testJvmOnlyProject(): Unit = with(testProject("misc/jvmOnlyResources")) {
        gradle("jar").checks {
            check.logContains("Configure java-only compose resources")
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

    //https://github.com/JetBrains/compose-multiplatform/issues/4194
    //https://github.com/JetBrains/compose-multiplatform/issues/4285
    //https://youtrack.jetbrains.com/issue/CMP-7934
    //
    // 25_000 icons + (25_000 * 80) strings!!!
    @Test
    fun testHugeNumberOfResources(): Unit = with(testProject("misc/hugeResources")) {
        gradle(":generateResourceFiles")
        gradle(":generateResourceAccessorsForCommonMain").checks {
            val buildPath = "build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/app/group/huge/generated/resources"
            assertEqualTextFiles(file("$buildPath/Drawable0.commonMain.kt"), file("expected/Drawable0.commonMain.kt"))
            assertEqualTextFiles(file("$buildPath/Drawable100.commonMain.kt"), file("expected/Drawable100.commonMain.kt"))
            assertEqualTextFiles(file("$buildPath/String0.commonMain.kt"), file("expected/String0.commonMain.kt"))
            assertEqualTextFiles(file("$buildPath/String100.commonMain.kt"), file("expected/String100.commonMain.kt"))
        }
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
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSkipped(":generateComposeResClass")
            check.taskSkipped(":generateResourceAccessorsForCommonMain")
            check.taskSkipped(":generateResourceAccessorsForDesktopMain")
            check.taskSkipped(":generateResourceAccessorsForAndroidMain")
            check.taskSkipped(":generateExpectResourceCollectorsForCommonMain")
            check.taskSkipped(":generateActualResourceCollectorsForDesktopMain")
            check.taskSkipped(":generateActualResourceCollectorsForAndroidMain")
        }
    }

    @Test
    fun xcframeworkResourcesAreNotSupported() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)

        with(
            testProject(
                "misc/appleResources",
                defaultTestEnvironment.copy(kotlinVersion = "2.1.21"))
        ) {
            file("build.gradle.kts").modify { content ->
                """
                    |import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                    |
                    |plugins {
                    |    kotlin("multiplatform")
                    |    kotlin("plugin.compose")
                    |    id("org.jetbrains.compose")
                    |}
                    |
                    |kotlin {
                    |    val xcf = XCFramework("ComposeApp")
                    |
                    |    macosArm64()
                    |
                    |    listOf(
                    |        iosX64(),
                    |        iosArm64(),
                    |        iosSimulatorArm64()
                    |    ).forEach {
                    |        it.binaries.framework {
                    |            baseName = "ComposeApp"
                    |            isStatic = true
                    |            xcf.add(this)
                    |        }
                    |    }
                    |
                    |    sourceSets {
                    |        commonMain {
                    |            dependencies {
                    |                implementation(compose.runtime)
                    |                implementation(compose.material)
                    |                implementation(compose.components.resources)
                    |            }
                    |        }
                    |    }
                    |}
                    |
                """.trimMargin()
            }
            gradle(":assembleComposeAppDebugXCFramework", "--dry-run").checks {
                check.logContains("Compose resources are supported in XCFrameworks since '2.2.0-Beta2-1' Kotlin Gradle plugin version")
            }
        }
    }

    @Test
    fun xcframeworkResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(
            testProject(
                "misc/appleResources",
                defaultTestEnvironment.copy(kotlinVersion = "2.2.0-RC2"))
        ) {
            file("build.gradle.kts").modify { content ->
                """
                    |import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework
                    |
                    |plugins {
                    |    kotlin("multiplatform")
                    |    kotlin("plugin.compose")
                    |    id("org.jetbrains.compose")
                    |}
                    |
                    |kotlin {
                    |    val xcf = XCFramework("ComposeApp")
                    |
                    |    macosX64()
                    |
                    |    listOf(
                    |        macosArm64(),
                    |        iosArm64()
                    |    ).forEach {
                    |        it.binaries.framework {
                    |            baseName = "ComposeApp"
                    |            isStatic = true
                    |            xcf.add(this)
                    |        }
                    |    }
                    |
                    |    sourceSets {
                    |        commonMain {
                    |            dependencies {
                    |                implementation(compose.runtime)
                    |                implementation(compose.material)
                    |                implementation(compose.components.resources)
                    |            }
                    |        }
                    |    }
                    |}
                    |
                """.trimMargin()
            }
            gradle(":assembleComposeAppDebugXCFramework", "--dry-run").checks {
                check.logContains("Configure compose resources in assembleComposeAppDebugXCFramework")
            }
            gradle(":assembleComposeAppDebugXCFramework").checks {
                assertDirectoriesContentEquals(
                    file("build/XCFrameworks/debug/ComposeApp.xcframework/ios-arm64/ComposeApp.framework/composeResources"),
                    file("expected/XCFrameworks/iosComposeResources")
                )
                assertDirectoriesContentEquals(
                    file("build/XCFrameworks/debug/ComposeApp.xcframework/macos-arm64/ComposeApp.framework/composeResources"),
                    file("expected/XCFrameworks/macosComposeResources")
                )
            }
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

        with(TestProject("misc/appleResources", testEnv)) {
            gradle(":podspec", "-Pkotlin.native.cocoapods.generate.wrapper=true").checks {
                assertEqualTextFiles(
                    file("appleResources.podspec"),
                    file("expected/appleResources.podspec")
                )
                file("build/compose/cocoapods/compose-resources").checkExists()
            }

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

                file("build/compose/cocoapods/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("build/compose/cocoapods/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
            }
        }
    }

    @Test
    fun cocoapodsIosXCFrameworkResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)

        with(
            testProject(
                "misc/appleResources",
                defaultTestEnvironment.copy(kotlinVersion = "2.2.0-RC2"))
        ) {
            gradle(":podPublishDebugXCFramework").checks {
                assertDirectoriesContentEquals(
                    file("build/cocoapods/publish/debug/shared.xcframework/ios-arm64/shared.framework/composeResources"),
                    file("expected/XCFrameworks/iosComposeResources")
                )
                assertDirectoriesContentEquals(
                    file("build/cocoapods/publish/debug/shared.xcframework/ios-arm64_x86_64-simulator/shared.framework/composeResources"),
                    file("expected/XCFrameworks/iosComposeResources")
                )
            }
        }
    }

    @Test
    fun macosResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        val macosEnv = mapOf(
            "PLATFORM_NAME" to "macosx",
            "ARCHS" to "arm64",
            "CONFIGURATION" to "Debug",
        )
        val testEnv = defaultTestEnvironment.copy(
            additionalEnvVars = macosEnv
        )

        with(TestProject("misc/appleResources", testEnv)) {
            file("build.gradle.kts").modify { content ->
                content.replace(
                    """
                        |    iosX64()
                        |    iosArm64()
                        |    iosSimulatorArm64()
                    """.trimMargin(),
                    """
                        |    macosX64()
                        |    macosArm64()
                    """.trimMargin()
                )
            }
            file("src/iosMain").renameTo(file("src/macosMain"))

            gradle(":podspec", "-Pkotlin.native.cocoapods.generate.wrapper=true").checks {
                assertEqualTextFiles(
                    file("appleResources.podspec"),
                    file("expected/appleResources.podspec")
                )
                file("build/compose/cocoapods/compose-resources").checkExists()
            }

            gradle(
                ":syncFramework",
                "-Pkotlin.native.cocoapods.platform=${macosEnv["PLATFORM_NAME"]}",
                "-Pkotlin.native.cocoapods.archs=${macosEnv["ARCHS"]}",
                "-Pkotlin.native.cocoapods.configuration=${macosEnv["CONFIGURATION"]}",
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

                check.taskSkipped(":convertXmlValueResourcesForMacosMain")
                check.taskSkipped(":copyNonXmlValueResourcesForMacosMain")
                check.taskSkipped(":prepareComposeResourcesTaskForMacosMain")
                check.taskSkipped(":generateResourceAccessorsForMacosMain")

                check.taskSkipped(":convertXmlValueResourcesForMacosX64Main")
                check.taskSkipped(":copyNonXmlValueResourcesForMacosX64Main")
                check.taskSkipped(":prepareComposeResourcesTaskForMacosX64Main")
                check.taskSkipped(":generateResourceAccessorsForMacosX64Main")

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

                check.taskNoSource(":convertXmlValueResourcesForMacosMain")
                check.taskSuccessful(":copyNonXmlValueResourcesForMacosMain")
                check.taskSuccessful(":prepareComposeResourcesTaskForMacosMain")
                check.taskSkipped(":generateResourceAccessorsForMacosMain")

                check.taskNoSource(":convertXmlValueResourcesForMacosX64Main")
                check.taskNoSource(":copyNonXmlValueResourcesForMacosX64Main")
                check.taskNoSource(":prepareComposeResourcesTaskForMacosX64Main")
                check.taskSkipped(":generateResourceAccessorsForMacosX64Main")

                file("build/compose/cocoapods/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("build/compose/cocoapods/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
            }
        }
    }

    @Test
    fun iosTestResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(testProject("misc/appleResources")) {
            gradle(":linkDebugTestIosX64", "--dry-run").checks {
                check.taskSkipped(":copyTestComposeResourcesForIosX64")
                check.taskSkipped(":linkDebugTestIosX64")
            }
            gradle(":copyTestComposeResourcesForIosX64").checks {
                file("build/bin/iosX64/debugTest/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("build/bin/iosX64/debugTest/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
            }
        }
    }

    @Test
    fun macosTestResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(testProject("misc/appleResources")) {
            file("build.gradle.kts").modify { content ->
                content.replace(
                    """
                        |    iosX64()
                        |    iosArm64()
                        |    iosSimulatorArm64()
                    """.trimMargin(),
                    """
                        |    macosX64()
                        |    macosArm64()
                    """.trimMargin()
                )
            }
            file("src/iosMain").renameTo(file("src/macosMain"))
            gradle(":linkDebugTestMacosX64", "--dry-run").checks {
                check.taskSkipped(":copyTestComposeResourcesForMacosX64")
                check.taskSkipped(":linkDebugTestMacosX64")
            }
            gradle(":copyTestComposeResourcesForMacosX64").checks {
                file("build/bin/macosX64/debugTest/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("build/bin/macosX64/debugTest/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
            }
        }
    }

    @Test
    fun checkTestResources() {
        with(testProject("misc/testResources")) {
            gradle("check").checks {
                check.logContains("Configure main resources for 'desktop' target")
                check.logContains("Configure test resources for 'desktop' target")
                check.logContains("Configure main resources for 'iosX64' target")
                check.logContains("Configure test resources for 'iosX64' target")
                check.logContains("Configure main resources for 'iosArm64' target")
                check.logContains("Configure test resources for 'iosArm64' target")
                check.logContains("Configure main resources for 'iosSimulatorArm64' target")
                check.logContains("Configure test resources for 'iosSimulatorArm64' target")
                check.logContains("Configure main resources for 'macosX64' target")
                check.logContains("Configure test resources for 'macosX64' target")
                check.logContains("Configure main resources for 'macosArm64' target")
                check.logContains("Configure test resources for 'macosArm64' target")

                check.taskSuccessful(":desktopTest")
            }
        }
    }
}