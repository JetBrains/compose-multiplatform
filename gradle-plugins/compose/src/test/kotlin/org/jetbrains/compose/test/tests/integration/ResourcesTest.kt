package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.assertEqualTextFiles
import org.jetbrains.compose.test.utils.assertNotEqualTextFiles
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
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
    fun testCopyFontsInAndroidApp(): Unit = with(testProject("misc/commonResources")) {
        gradle("assembleDebug").checks {
            check.taskSuccessful(":copyFontsToAndroidAssets")
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