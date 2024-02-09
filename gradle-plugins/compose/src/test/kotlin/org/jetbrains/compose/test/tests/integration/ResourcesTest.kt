package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.*
import org.jetbrains.compose.test.utils.assertEqualTextFiles
import org.jetbrains.compose.test.utils.assertNotEqualTextFiles
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.io.path.Path

class ResourcesTest : GradlePluginTestBase() {
    @Test
    fun testGeneratedAccessors(): Unit = with(testProject("misc/commonResources")) {
        //check generated resource's accessors
        gradle("generateComposeResClass").checks {
            assertEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
        }

        //check resource's accessors were regenerated
        file("src/commonMain/composeResources/drawable/vector_2.xml").renameTo(
            file("src/commonMain/composeResources/drawable/vector_3.xml")
        )
        gradle("generateComposeResClass").checks {
            assertNotEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
        }

        file("src/commonMain/composeResources/drawable-en").renameTo(
            file("src/commonMain/composeResources/drawable-ren")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                contains unknown qualifier: 'ren'.
            """.trimIndent())
        }

        file("src/commonMain/composeResources/drawable-ren").renameTo(
            file("src/commonMain/composeResources/drawable-rUS-en")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                Region qualifier must be declared after language: 'en-rUS'.
            """.trimIndent())
        }

        file("src/commonMain/composeResources/drawable-rUS-en").renameTo(
            file("src/commonMain/composeResources/drawable-rUS")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                Region qualifier must be used only with language.
            """.trimIndent())
        }

        file("src/commonMain/composeResources/drawable-rUS").renameTo(
            file("src/commonMain/composeResources/drawable-en-fr")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                contains repetitive qualifiers: 'en' and 'fr'.
            """.trimIndent())
        }

        file("src/commonMain/composeResources/drawable-en-fr").renameTo(
            file("src/commonMain/composeResources/image")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                Unknown resource type: 'image'
            """.trimIndent())
        }

        file("src/commonMain/composeResources/image").renameTo(
            file("src/commonMain/composeResources/files-de")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                The 'files' directory doesn't support qualifiers: 'files-de'.
            """.trimIndent())
        }

        file("src/commonMain/composeResources/files-de").renameTo(
            file("src/commonMain/composeResources/strings")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                Unknown resource type: 'strings'.
            """.trimIndent())
        }

        file("src/commonMain/composeResources/strings").renameTo(
            file("src/commonMain/composeResources/string-us")
        )
        gradle("generateComposeResClass").checks {
            check.logContains("""
                Forbidden directory name 'string-us'! String resources should be declared in 'values/strings.xml'.
            """.trimIndent())
        }

        //restore defaults
        file("src/commonMain/composeResources/string-us").renameTo(
            file("src/commonMain/composeResources/drawable-en")
        )
        file("src/commonMain/composeResources/drawable/vector_3.xml").renameTo(
            file("src/commonMain/composeResources/drawable/vector_2.xml")
        )
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
            .map { it.relativeTo(commonResourcesDir).path }

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
            assert(desktopJar.exists())
            ZipFile(desktopJar).let { zip ->
                commonResourcesFiles.forEach { res ->
                    assert(zip.getEntry(res) != null)
                }
                assert(zip.getEntry("files/platform.txt") != null)
                val text = zip.getInputStream(
                    zip.getEntry("files/platform.txt")
                ).readBytes().decodeToString()
                assert(text == "desktop")
            }

            val jsBuildDir = file("build/dist/js/productionExecutable")
            commonResourcesFiles.forEach { res ->
                assert(jsBuildDir.resolve(res).exists())
            }
            assert(jsBuildDir.resolve("files/platform.txt").readText() == "js")
        }
    }

    private fun File.writeNewFile(text: String) {
        parentFile.mkdirs()
        createNewFile()
        writeText(text)
    }

    private fun TestProject.checkAndroidApk(flavor: String, type: String, commonResourcesFiles: Sequence<String>) {
        val typeFilePostfix = if (type == "release") "$type-unsigned" else type
        val apk = file("build/outputs/apk/$flavor/$type/resources_test-$flavor-$typeFilePostfix.apk")
        assert(apk.exists())
        ZipFile(apk).let { zip ->
            commonResourcesFiles.forEach { res ->
                assert(zip.getEntry(res) != null)
                //todo fix duplicate fonts
            }
            assert(zip.getEntry("assets/font/emptyFont.otf") != null)
            assert(zip.getEntry("files/platform.txt") != null)
            val text = zip.getInputStream(
                zip.getEntry("files/platform.txt")
            ).readBytes().decodeToString()
            assert(text == "android $flavor-$type")
        }
    }

    @Test
    fun testUpToDateChecks(): Unit = with(testProject("misc/commonResources")) {
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assert(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
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
            assert(!file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        gradle("prepareKotlinIdeaImport", "-Pcompose.resources.always.generate.accessors=true").checks {
            check.taskSuccessful(":generateComposeResClass")
            assert(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "//implementation(compose.components.resources)",
                "implementation(compose.components.resources)"
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskUpToDate(":generateComposeResClass")
            assert(file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
        }

        modifyText("build.gradle.kts") { str ->
            str.replace(
                "group = \"app.group\"",
                "group = \"io.company\""
            )
        }
        gradle("prepareKotlinIdeaImport").checks {
            check.taskSuccessful(":generateComposeResClass")
            assert(!file("build/generated/compose/resourceGenerator/kotlin/app/group/resources_test/generated/resources/Res.kt").exists())
            assert(file("build/generated/compose/resourceGenerator/kotlin/io/company/resources_test/generated/resources/Res.kt").exists())
        }
    }

    @Test
    fun testEmptyResClass(): Unit = with(testProject("misc/emptyResources")) {
        gradle("generateComposeResClass").checks {
            assertEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/empty_res/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
        }
    }

    @Test
    fun testJvmOnlyProject(): Unit = with(testProject("misc/jvmOnlyResources")) {
        gradle("generateComposeResClass").checks {
            assertEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/me/app/jvmonlyresources/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
        }
        gradle("jar")
    }

    //https://github.com/JetBrains/compose-multiplatform/issues/4194
    @Test
    fun testHugeNumberOfStrings(): Unit = with(
        //disable cache for the test because the generateStringFiles task doesn't support it
        testProject("misc/commonResources", defaultTestEnvironment.copy(useGradleConfigurationCache = false))
    ) {
        file("build.gradle.kts").let { f ->
            val originText = f.readText()
            f.writeText(
                buildString {
                    appendLine("import java.util.Locale")
                    append(originText)
                    appendLine()
                    append("""
                        val template = ""${'"'}
                            <resources>
                                <string name="app_name">Compose Resources App</string>
                                <string name="hello">😊 Hello world!</string>
                                <string name="multi_line">Lorem ipsum dolor sit amet,
                                    consectetur adipiscing elit.
                                    Donec eget turpis ac sem ultricies consequat.</string>
                                <string name="str_template">Hello, %1${'$'}{"$"}s! You have %2${'$'}{"$"}d new messages.</string>
                                <string-array name="str_arr">
                                    <item>item 1</item>
                                    <item>item 2</item>
                                    <item>item 3</item>
                                </string-array>
                                [ADDITIONAL_STRINGS]
                            </resources>    
                        ""${'"'}.trimIndent()

                        val generateStringFiles = tasks.register("generateStringFiles") {
                            val numberOfLanguages = 20
                            val numberOfStrings = 500
                            val langs = Locale.getAvailableLocales()
                                .map { it.language }
                                .filter { it.count() == 2 }
                                .sorted()
                                .distinct()
                                .take(numberOfLanguages)
                                .toList()

                            val resourcesFolder = project.file("src/commonMain/composeResources")

                            doLast {
                                // THIS REMOVES THE `values` FOLDER IN `composeResources`
                                // THIS REMOVES THE `values` FOLDER IN `composeResources`
                                // Necessary when reducing the number of languages.
                                resourcesFolder.listFiles()?.filter { it.name.startsWith("values") }?.forEach {
                                    it.deleteRecursively()
                                }

                                langs.forEachIndexed { langIndex, lang ->
                                    val additionalStrings =
                                        (0 until numberOfStrings).joinToString(System.lineSeparator()) { index ->
                                            ""${'"'}
                                            <string name="string_${'$'}{index.toString().padStart(4, '0')}">String ${'$'}index in lang ${'$'}lang</string>
                                            ""${'"'}.trimIndent()
                                        }

                                    val langFile = if (langIndex == 0) {
                                        File(resourcesFolder, "values/strings.xml")
                                    } else {
                                        File(resourcesFolder, "values-${'$'}lang/strings.xml")
                                    }
                                    langFile.parentFile.mkdirs()
                                    langFile.writeText(template.replace("[ADDITIONAL_STRINGS]", additionalStrings))
                                }
                            }
                        }

                        tasks.named("generateComposeResClass") {
                            dependsOn(generateStringFiles)
                        }
                    """.trimIndent())
                }
            )
        }
        gradle("desktopJar").checks {
            check.taskSuccessful(":generateStringFiles")
            check.taskSuccessful(":generateComposeResClass")
            assert(file("src/commonMain/composeResources/values/strings.xml").readLines().size == 513)
        }
    }

    //https://github.com/gmazzo/gradle-buildconfig-plugin/issues/131
    @Test
    fun testBundledKotlinPoet(): Unit = with(testProject("misc/bundledKotlinPoet")) {
        gradle("generateBuildConfig")
    }
}