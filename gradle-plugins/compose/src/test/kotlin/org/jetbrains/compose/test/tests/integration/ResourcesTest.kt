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
                file("build/generated/compose/resourceGenerator/kotlin/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
            check.logContains("""
                java.lang.IllegalStateException: Unknown resource type: ignored
            """.trimIndent())
        }

        file("src/commonMain/resources/composeRes/images/vector_2.xml").renameTo(
            file("src/commonMain/resources/composeRes/images/vector_3.xml")
        )

        //check resource's accessors were regenerated
        gradle("generateComposeResClass").checks {
            assertNotEqualTextFiles(
                file("build/generated/compose/resourceGenerator/kotlin/generated/resources/Res.kt"),
                file("expected/Res.kt")
            )
        }

        file("src/commonMain/resources/composeRes/images/vector_3.xml").renameTo(
            file("src/commonMain/resources/composeRes/images/vector_2.xml")
        )

        //TODO: check a real build after a release a new version of the resources library
        //because generated accessors depend on classes from the new version
        gradle("assembleDebug", "--dry-run").checks {
            check.taskSkipped("copyFontsToAndroidAssets")
        }
    }
}