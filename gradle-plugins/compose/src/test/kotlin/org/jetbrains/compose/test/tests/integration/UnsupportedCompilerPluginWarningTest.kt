package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.createWarningAboutNonCompatibleCompiler
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProjects
import org.jetbrains.compose.test.utils.checks
import org.junit.jupiter.api.Test

class UnsupportedCompilerPluginWarningTest : GradlePluginTestBase() {

    private val androidxComposeCompilerGroupId = "androidx.compose.compiler"
    private val androidxComposeCompilerPlugin = "$androidxComposeCompilerGroupId:compiler:1.4.8"

    @Suppress("RedundantUnitExpression")
    @Test
    fun testKotlinJs_shows_warning_for_androidx_compose_compiler() = testProject(
        TestProjects.customCompilerArgs, defaultTestEnvironment.copy(
            kotlinVersion = "1.8.22",
            composeCompilerPlugin = "\"$androidxComposeCompilerPlugin\"",
            composeCompilerArgs = "\"suppressKotlinVersionCompatibilityCheck=1.8.22\""
        )
    ).let {
        it.gradle(":compileKotlinJs").checks {
            check.taskSuccessful(":compileKotlinJs")
            check.logContains(createWarningAboutNonCompatibleCompiler(androidxComposeCompilerGroupId))
        }
        Unit
    }

    @Suppress("RedundantUnitExpression")
    @Test
    fun testKotlinJvm_doesnt_show_warning_for_androidx_compose_compiler() = testProject(
        TestProjects.customCompiler, defaultTestEnvironment.copy(
            kotlinVersion = "1.8.22",
            composeCompilerPlugin = "\"$androidxComposeCompilerPlugin\"",
        )
    ).let {
        it.gradle(":run").checks {
            check.taskSuccessful(":run")
            check.logDoesntContain(createWarningAboutNonCompatibleCompiler(androidxComposeCompilerGroupId))
        }
        Unit
    }
}