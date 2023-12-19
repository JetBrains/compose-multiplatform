package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.assertEqualTextFiles
import org.jetbrains.compose.test.utils.assertNotEqualTextFiles
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test
import kotlin.io.path.Path

class ResourcesTest : GradlePluginTestBase() {
    @Test
    fun testGeneratedAccessorsAndCopiedFonts() = with(testProject("misc/commonResources")) {
        //check generated resource's accessors
        gradle("generateComposeResClass").checks {
            assertEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
            check.logContains("""
                Unknown resource type: ignored
            """.trimIndent())
        }

        file("src/commonMain/resources/composeRes/drawable/vector_2.xml").renameTo(
            file("src/commonMain/resources/composeRes/drawable/vector_3.xml")
        )

        //check resource's accessors were regenerated
        gradle("generateComposeResClass").checks {
            assertNotEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/app/group/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
        }

        file("src/commonMain/resources/composeRes/drawable-en").renameTo(
            file("src/commonMain/resources/composeRes/drawable-ren")
        )

        gradle("generateComposeResClass").checks {
            check.logContains("""
                contains unknown qualifier: ren
            """.trimIndent())
        }

        file("src/commonMain/resources/composeRes/drawable-ren").renameTo(
            file("src/commonMain/resources/composeRes/drawable-rUS-en")
        )

        gradle("generateComposeResClass").checks {
            check.logContains("""
                Region qualifier must be declared after language: 'en-rUS'
            """.trimIndent())
        }

        file("src/commonMain/resources/composeRes/drawable-rUS-en").renameTo(
            file("src/commonMain/resources/composeRes/drawable-rUS")
        )

        gradle("generateComposeResClass").checks {
            check.logContains("""
                Region qualifier must be used only with language
            """.trimIndent())
        }

        file("src/commonMain/resources/composeRes/drawable-rUS").renameTo(
            file("src/commonMain/resources/composeRes/drawable-en-fr")
        )

        gradle("generateComposeResClass").checks {
            check.logContains("""
                contains repetitive qualifiers: en and fr
            """.trimIndent())
        }

        file("src/commonMain/resources/composeRes/drawable-en-fr").renameTo(
            file("src/commonMain/resources/composeRes/drawable-en")
        )

        file("src/commonMain/resources/composeRes/drawable/vector_3.xml").renameTo(
            file("src/commonMain/resources/composeRes/drawable/vector_2.xml")
        )

        //TODO: check a real build after a release a new version of the resources library
        //because generated accessors depend on classes from the new version
        gradle("assembleDebug", "--dry-run").checks {
            check.taskSkipped("copyFontsToAndroidAssets")
        }
    }
}