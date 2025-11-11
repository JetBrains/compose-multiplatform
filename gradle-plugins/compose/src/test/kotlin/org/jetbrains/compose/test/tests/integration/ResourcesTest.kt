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
import org.jetbrains.compose.test.utils.checkNotExists
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
    fun testAndroidAppWithResources() {
        //FIXME delete the filter when https://issuetracker.google.com/456657404 is fixed
        Assumptions.assumeFalse {
            currentOS == OS.Windows && defaultTestEnvironment.agpVersion.contains("9.0.0")
        }
        with(testProject("misc/androidAppWithResources", defaultTestEnvironment)) {
            gradle(":appModule:assembleDebug").checks {
                check.logContains("Configure compose resources with KotlinMultiplatformAndroidComponentsExtension")

                val resourcesFiles = sequenceOf(
                    "composeResources/newagpresources.featuremodule.generated.resources/values/strings.commonMain.cvr"
                )
                val apk = file("appModule/build/outputs/apk/debug/appModule-debug.apk")
                checkResourcesZip(apk, resourcesFiles, true)
            }
        }
    }

    @Test
    fun testAndroidPreviewCallsResourcesPackaging() {
        // Valid for AGP < 9.0.0 only
        // https://youtrack.jetbrains.com/issue/CMP-7170
        Assumptions.assumeTrue { Version.fromString(defaultTestEnvironment.agpVersion).major < 9 }
        with(testProject("misc/oldAndroidTargetAppWithResources", defaultTestEnvironment)) {
            //AndroidStudio previews call `compileDebugSources` task
            gradle(":appModule:compileDebugSources").checks {
                check.taskSuccessful(":appModule:packageDebugResources")
                check.taskSuccessful(":featureModule:packageDebugResources")
                check.taskSuccessful(":featureModule:copyDebugComposeResourcesToAndroidAssets")

                val resourceFile = "composeResources/oldagpresources.featuremodule.generated.resources/values/strings.commonMain.cvr"
                assertTrue {
                    file(
                        "featureModule/build/generated/assets/copyDebugComposeResourcesToAndroidAssets/$resourceFile"
                    ).exists()
                }
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
    fun testFinalArtefacts(): Unit = with(testProject("misc/fullTargetsAppWithResources")) {
        val commonResourcesDir = file("src/commonMain/composeResources")
        val repackDir = "composeResources/multiplatform_app.sharedui.generated.resources"
        val commonResourcesFiles = commonResourcesDir.walkTopDown()
            .filter { !it.isDirectory && !it.isHidden }
            .getConvertedResources(commonResourcesDir, repackDir)

        //FIXME delete the filter when https://issuetracker.google.com/456657404 is fixed
        val skipAndroidCheck = currentOS == OS.Windows && defaultTestEnvironment.agpVersion.contains("9.0.0")
        if (!skipAndroidCheck) {
            gradle(":androidApp:assemble").checks {
                check.taskSuccessful(":sharedUI:copyAndroidMainComposeResourcesToAndroidAssets")

                listOf(
                    "androidApp/build/outputs/apk/demo/debug/androidApp-demo-debug.apk",
                    "androidApp/build/outputs/apk/full/debug/androidApp-full-debug.apk",
                    "androidApp/build/outputs/apk/demo/release/androidApp-demo-release.apk",
                    "androidApp/build/outputs/apk/full/release/androidApp-full-release.apk"
                ).forEach { path ->
                    val apk = file(path)
                    checkResourcesZip(apk, commonResourcesFiles, true)
                    assertEquals(
                        "android",
                        readFileInZip(apk, "assets/$repackDir/files/platform.txt").decodeToString()
                    )
                }
            }
        }

        gradle(":desktopApp:build").checks {
            check.taskSuccessful(":sharedUI:jvmProcessResources")

            file("sharedUI/build/libs/sharedUI-jvm.jar").let { jar ->
                checkResourcesZip(jar, commonResourcesFiles, false)
                assertEquals(
                    "desktop",
                    readFileInZip(jar, "$repackDir/files/platform.txt").decodeToString()
                )
            }
        }

        gradle(":webApp:build").checks {
            check.taskSuccessful(":sharedUI:wasmJsCopyHierarchicalMultiplatformResources")
            check.taskSuccessful(":sharedUI:jsCopyHierarchicalMultiplatformResources")

            val jsBuildDir = file("webApp/build/dist/js/productionExecutable")
            commonResourcesFiles.forEach { res ->
                assertTrue(jsBuildDir.resolve(res).exists())
            }
            assertEquals("js", jsBuildDir.resolve("$repackDir/files/platform.txt").readText())

            val wasmJsBuildDir = file("webApp/build/dist/wasmJs/productionExecutable")
            commonResourcesFiles.forEach { res ->
                assertTrue(wasmJsBuildDir.resolve(res).exists())
            }
            assertEquals("wasm", wasmJsBuildDir.resolve("$repackDir/files/platform.txt").readText())
        }


        if (currentOS == OS.MacOS) {
            gradle(":sharedUI:assembleSharedUIDebugXCFramework").checks {
                check.taskSuccessful(":sharedUI:iosArm64AggregateResources")
                check.taskSuccessful(":sharedUI:iosSimulatorArm64AggregateResources")
                check.taskSuccessful(":sharedUI:iosX64AggregateResources")

                val iosDeviceFramework =
                    file("sharedUI/build/XCFrameworks/debug/SharedUI.xcframework/ios-arm64/SharedUI.framework")
                commonResourcesFiles.forEach { res ->
                    assertTrue(iosDeviceFramework.resolve(res).exists())
                }
                assertEquals("ios", iosDeviceFramework.resolve("$repackDir/files/platform.txt").readText())

                val iosSimFramework =
                    file("sharedUI/build/XCFrameworks/debug/SharedUI.xcframework/ios-arm64_x86_64-simulator/SharedUI.framework")
                commonResourcesFiles.forEach { res ->
                    assertTrue(iosSimFramework.resolve(res).exists())
                }
                assertEquals("ios", iosSimFramework.resolve("$repackDir/files/platform.txt").readText())
            }
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
        val disableProperty = ComposeProperties.DISABLE_RESOURCE_CONTENT_HASH_GENERATION
        gradle("prepareKotlinIdeaImport", "-P$disableProperty=false").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                file("expected-with-hash")
            )
        }

        gradle("prepareKotlinIdeaImport", "-P$disableProperty=true").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin"),
                file("expected")
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
            val buildPath =
                "build/generated/compose/resourceGenerator/kotlin/commonMainResourceAccessors/app/group/huge/generated/resources"
            assertEqualTextFiles(file("$buildPath/Drawable0.commonMain.kt"), file("expected/Drawable0.commonMain.kt"))
            assertEqualTextFiles(
                file("$buildPath/Drawable100.commonMain.kt"),
                file("expected/Drawable100.commonMain.kt")
            )
            assertEqualTextFiles(file("$buildPath/String0.commonMain.kt"), file("expected/String0.commonMain.kt"))
            assertEqualTextFiles(file("$buildPath/String100.commonMain.kt"), file("expected/String100.commonMain.kt"))
        }
        gradle(":generateActualResourceCollectorsForDesktopMain").checks {
            val desktopPath =
                "build/generated/compose/resourceGenerator/kotlin/desktopMainResourceCollectors/app/group/huge/generated/resources"
            assertEqualTextFiles(
                file("$desktopPath/ActualResourceCollectors.kt"),
                file("expected/ActualResourceCollectors.kt")
            )
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
                defaultTestEnvironment.copy(kotlinVersion = "2.1.21")
            )
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
        with(testProject("misc/appleResources")) {
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
        with(testProject("misc/appleResources")) {
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
    fun macosExecutableResources() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(testProject("misc/macosNativeResources")) {
            val appName = "Test Resources"
            gradle(":createDistributableNativeDebugMacosX64").checks {
                val targetResourcesDir =
                    "build/compose/binaries/main/native-macosX64-debug-app-image/${appName}.app/Contents/Resources"
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
            }
        }
    }

    @Test
    fun macosExecutableResourcesWithResourceChanged() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(testProject("misc/macosNativeResources")) {
            val appName = "Test Resources"
            val taskName = ":createDistributableNativeDebugMacosX64"
            val comment = "<!-- Test resources changed -->"
            val fileNames = listOf(
                "compose-multiplatform.xml",
                "icon.xml"
            )
            val targetResourcesDir =
                "build/compose/binaries/main/native-macosX64-debug-app-image/${appName}.app/Contents/Resources/compose-resources/composeResources/appleresources.generated.resources/drawable/"
            gradle(taskName).checks {
                fileNames.forEach { name ->
                    check(!file(targetResourcesDir + name).readText().startsWith(comment)) {
                        "The resources file contains the test content before change"
                    }
                }
            }

            listOf(
                "src/commonMain/composeResources/drawable/compose-multiplatform.xml",
                "src/macosMain/composeResources/drawable/icon.xml"
            ).forEach { path ->
                file(path).modify {
                    comment + it
                }
            }
            gradle(taskName).checks {
                check.taskSuccessful(taskName)
                fileNames.forEach { name ->
                    check(file(targetResourcesDir + name).readText().startsWith(comment)) {
                        "The resources file does not contain the test content after changed"
                    }
                }
            }
        }
    }

    @Test
    fun macosExecutableResourcesWithResourceDeleted() {
        Assumptions.assumeTrue(currentOS == OS.MacOS)
        with(testProject("misc/macosNativeResources")) {
            val appName = "Test Resources"
            val taskName = ":createDistributableNativeDebugMacosX64"

            val targetResource = "src/commonMain/composeResources/drawable/compose-multiplatform2.xml"
            file(targetResource).apply {
                check(createNewFile())
                writeText(file(targetResource.replace("compose-multiplatform2", "compose-multiplatform")).readText())
            }

            gradle(taskName).checks {
                val targetResourcesDir =
                    "build/compose/binaries/main/native-macosX64-debug-app-image/${appName}.app/Contents/Resources"
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform2.xml").checkExists()
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
            }
            check(file(targetResource).delete())
            gradle(taskName).checks {
                check.taskSuccessful(taskName)
                val targetResourcesDir =
                    "build/compose/binaries/main/native-macosX64-debug-app-image/${appName}.app/Contents/Resources"
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform.xml").checkExists()
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/compose-multiplatform2.xml").checkNotExists()
                file("$targetResourcesDir/compose-resources/composeResources/appleresources.generated.resources/drawable/icon.xml").checkExists()
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