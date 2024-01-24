package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.assertEqualTextFiles
import org.jetbrains.compose.test.utils.assertNotEqualTextFiles
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ResourcesTest : GradlePluginTestBase() {
    @Test
    fun testGeneratedAccessorsAndCopiedFonts(): Unit = with(testProject("misc/commonResources")) {
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
}