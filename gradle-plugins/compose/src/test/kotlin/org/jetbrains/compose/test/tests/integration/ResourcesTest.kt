package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.*
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.io.path.relativeTo
import kotlin.test.*

class ResourcesTest : GradlePluginTestBase() {
    @Test
    fun testGeneratedAccessors(): Unit = with(testProject("misc/commonResources")) {
        //check generated resource's accessors
        gradle("generateComposeResClass").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources"),
                file("expected")
            )
        }

        //check resource's accessors were regenerated
        file("src/commonMain/composeResources/drawable/vector_2.xml").renameTo(
            file("src/commonMain/composeResources/drawable/vector_3.xml")
        )
        gradle("generateComposeResClass").checks {
            assertNotEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Drawable0.kt"),
                file("expected/Drawable0.kt")
            )
        }

        file("src/commonMain/composeResources/drawable-en").renameTo(
            file("src/commonMain/composeResources/drawable-rent")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                contains unknown qualifier: 'rent'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rent").renameTo(
            file("src/commonMain/composeResources/drawable-rUS-en")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                Region qualifier must be declared after language: 'en-rUS'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rUS-en").renameTo(
            file("src/commonMain/composeResources/drawable-rUS")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                Region qualifier must be used only with language.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-rUS").renameTo(
            file("src/commonMain/composeResources/drawable-en-fr")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                contains repetitive qualifiers: 'en' and 'fr'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/drawable-en-fr").renameTo(
            file("src/commonMain/composeResources/image")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                Unknown resource type: 'image'
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/image").renameTo(
            file("src/commonMain/composeResources/files-de")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                The 'files' directory doesn't support qualifiers: 'files-de'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/files-de").renameTo(
            file("src/commonMain/composeResources/strings")
        )
        gradle("generateComposeResClass").checks {
            check.logContains(
                """
                Unknown resource type: 'strings'.
            """.trimIndent()
            )
        }

        file("src/commonMain/composeResources/strings").renameTo(
            file("src/commonMain/composeResources/string-us")
        )
        gradle("generateComposeResClass").checks {
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
        gradle("generateComposeResClass").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources"),
                file("expected")
            )
        }
    }

    @Test
    fun testFinalArtefacts(): Unit = with(testProject("misc/commonResources")) {
        //https://developer.android.com/build/build-variants?utm_source=android-studio#product-flavors
        file("build.gradle.kts").appendText("""
            
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
        """.trimIndent())
        file("src/androidDemoDebug/composeResources/files/platform.txt").writeNewFile("android demo-debug")
        file("src/androidDemoRelease/composeResources/files/platform.txt").writeNewFile("android demo-release")
        file("src/androidFullDebug/composeResources/files/platform.txt").writeNewFile("android full-debug")
        file("src/androidFullRelease/composeResources/files/platform.txt").writeNewFile("android full-release")
        file("src/desktopMain/composeResources/files/platform.txt").writeNewFile("desktop")
        file("src/jsMain/composeResources/files/platform.txt").writeNewFile("js")

        val commonResourcesDir = file("src/commonMain/composeResources")
        val commonResourcesFiles = commonResourcesDir.walkTopDown()
            .filter { !it.isDirectory && !it.isHidden }
            .map { it.relativeTo(commonResourcesDir).invariantSeparatorsPath }

        gradle("build").checks {
            check.taskSuccessful(":copyDemoDebugFontsToAndroidAssets")
            check.taskSuccessful(":copyDemoReleaseFontsToAndroidAssets")
            check.taskSuccessful(":copyFullDebugFontsToAndroidAssets")
            check.taskSuccessful(":copyFullReleaseFontsToAndroidAssets")

            checkAndroidApk("demo", "debug", commonResourcesFiles)
            checkAndroidApk("demo", "release", commonResourcesFiles)
            checkAndroidApk("full", "debug", commonResourcesFiles)
            checkAndroidApk("full", "release", commonResourcesFiles)

            val desktopJar = file("build/libs/resources_test-desktop.jar")
            assertTrue(desktopJar.exists())
            ZipFile(desktopJar).use { zip ->
                commonResourcesFiles.forEach { res ->
                    assertNotNull(zip.getEntry(res))
                }
                val platformTxt = zip.getEntry("files/platform.txt")
                assertNotNull(platformTxt)
                val text = zip.getInputStream(platformTxt).readBytes().decodeToString()
                assertEquals("desktop", text)
            }

            val jsBuildDir = file("build/dist/js/productionExecutable")
            commonResourcesFiles.forEach { res ->
                assertTrue(jsBuildDir.resolve(res).exists())
            }
            assertEquals("js", jsBuildDir.resolve("files/platform.txt").readText())
        }
    }

    private fun File.writeNewFile(text: String) {
        parentFile.mkdirs()
        createNewFile()
        writeText(text)
    }

    private fun TestProject.checkAndroidApk(flavor: String, type: String, commonResourcesFiles: Sequence<String>) {
        val apk = file("build/outputs/apk/$flavor/$type/resources_test-$flavor-$type.apk")
        assertTrue(apk.exists())
        ZipFile(apk).use { zip ->
            commonResourcesFiles.forEach { res ->
                if (res == "font/emptyFont.otf") {
                    //android fonts should be only in assets
                    assertNull(zip.getEntry(res))
                } else {
                    assertNotNull(zip.getEntry(res))
                }
            }
            assertNotNull(zip.getEntry("assets/font/emptyFont.otf"))
            val platformTxt = zip.getEntry("files/platform.txt")
            assertNotNull(platformTxt)
            val text = zip.getInputStream(platformTxt).readBytes().decodeToString()
            assertEquals("android $flavor-$type", text)
        }
    }

    @Test
    fun testUpToDateChecks(): Unit = with(testProject("misc/commonResources")) {
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertTrue(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskUpToDate(":generateComposeResClass")
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "implementation(compose.components.resources)",
                "//implementation(compose.components.resources)"
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertFalse(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        gradle("prepareKotlinIdeaImport", "-Pcompose.resources.always.generate.accessors=true").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertTrue(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "//implementation(compose.components.resources)",
                "implementation(compose.components.resources)"
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskUpToDate(":generateComposeResClass")
            assertTrue(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "group = \"app.group\"",
                "group = \"io.company\""
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assertFalse(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
            assertTrue(file("build/generated/compose/resourceGenerator/kotlin/io/company/resources_test/generated/resources/Res.kt").exists())
        }
    }

    @Test
    fun testEmptyResClass(): Unit = with(testProject("misc/emptyResources")) {
        gradle("generateComposeResClass").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/empty_res/generated/resources"),
                file("expected")
            )
        }
    }

    @Test
    fun testJvmOnlyProject(): Unit = with(testProject("misc/jvmOnlyResources")) {
        gradle("generateComposeResClass").checks {
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin/me/app/jvmonlyresources/generated/resources"),
                file("expected")
            )
        }
        gradle("jar")
    }

    //https://github.com/JetBrains/compose-multiplatform/issues/4194
    //https://github.com/JetBrains/compose-multiplatform/issues/4285
    //
    // 1500 icons + 1500*20 strings!!!
    @Test
    fun testHugeNumberOfResources(): Unit = with(
        //disable cache for the test because the generateResourceFiles task doesn't support it
        testProject("misc/hugeResources", defaultTestEnvironment.copy(useGradleConfigurationCache = false))
    ) {
        gradle("compileKotlinDesktop").checks {
            check.taskSuccessful(":generateResourceFiles")
            check.taskSuccessful(":generateComposeResClass")
            assertDirectoriesContentEquals(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/huge/generated/resources"),
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
            .map { it.toPath().relativeTo(expectedPath) }.sorted().joinToString("\n")
        val actualFilesCount = actual.walkTopDown()
            .map { it.toPath().relativeTo(actualPath) }.sorted().joinToString("\n")
        assertEquals(expectedFilesCount, actualFilesCount)
    }
}