package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.TestProject
import org.jetbrains.compose.test.utils.checks
import org.jetbrains.compose.test.utils.modify
import kotlin.test.Test

class RuntimeLibrariesCompatibilityCheckTest : GradlePluginTestBase() {

    @Test
    fun correctConfigurationDoesntPrintWarning(): Unit = with(
        testProject("misc/compatibilityLibCheck")
    ) {
        val logMsg = "w: Compose Multiplatform runtime dependencies' versions don't match with plugin version."
        gradle("assembleAndroidMain").checks {
            check.logDoesntContain("checkAndroidMainComposeLibrariesCompatibility")
            check.logDoesntContain(logMsg)
        }
        gradle("metadataMainClasses").checks {
            check.logDoesntContain("checkMetadataMainComposeLibrariesCompatibility")
            check.logDoesntContain(logMsg)
        }
        gradle("jvmTestClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.taskSuccessful(":checkJvmTestComposeLibrariesCompatibility")
            check.logDoesntContain(logMsg)
        }
        checkMainTargetsCompatibility(logMsg, warningExpected = false)

        file("build.gradle.kts").modify {
            it.replace(
                "api(\"org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}\")",
                "api(\"org.jetbrains.compose.ui:ui:1.9.3\")"
            ).replace(
                "api(\"org.jetbrains.compose.foundation:foundation:${defaultTestEnvironment.composeVersion}\")",
                "api(\"org.jetbrains.compose.foundation:foundation:1.9.3\")"
            )
        }
        val msg = buildString {
            appendLine("w: Compose Multiplatform runtime dependencies' versions don't match with plugin version.")
            appendLine("    expected: 'org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}'")
            appendLine("    actual:   'org.jetbrains.compose.ui:ui:1.9.3'")
            appendLine("")
            appendLine("    expected: 'org.jetbrains.compose.foundation:foundation:${defaultTestEnvironment.composeVersion}'")
            appendLine("    actual:   'org.jetbrains.compose.foundation:foundation:1.9.3'")
        }
        checkMainTargetsCompatibility(msg, warningExpected = true)
        checkMainTargetsCompatibility(msg, warningExpected = false, disabled = true)
    }

    @Test
    fun skikoIncompatibleWarning(): Unit = with(
        testProject("misc/compatibilityLibCheck")
    ) {
        val logMsg = "w: Skiko dependencies' versions are incompatible."
        gradle("metadataMainClasses").checks {
            check.logDoesntContain("checkMetadataMainComposeLibrariesCompatibility")
            check.logDoesntContain(logMsg)
        }
        checkMainTargetsCompatibility(logMsg, warningExpected = false)
        gradle("jvmTestClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.taskSuccessful(":checkJvmTestComposeLibrariesCompatibility")
            check.logDoesntContain(logMsg)
        }

        // In case of dependency to old compose:ui without skiko explicitly, no warning should be emitted.
        file("build.gradle.kts").modify {
            it.replace(
                "api(\"org.jetbrains.compose.foundation:foundation:${defaultTestEnvironment.composeVersion}\")",
                "api(\"org.jetbrains.compose.foundation:foundation:${defaultTestEnvironment.composeVersion}\")\n" +
                        "            implementation(\"$OLD_COMPOSE_DEPENDENCY\")",
            )
        }
        checkMainTargetsCompatibility(logMsg, warningExpected = false)

        // Direct explicit dependency on old skiko introduces requested-version mismatch versus selected version,
        // so warning should be emitted.
        file("build.gradle.kts").modify {
            it.replace(
                "implementation(\"$OLD_COMPOSE_DEPENDENCY\")",
                "implementation(\"$OLD_COMPOSE_DEPENDENCY\")\n" +
                        "            implementation(\"$OLD_SKIKO_DEPENDENCY\")",
            )
        }
        checkMainTargetsCompatibility(logMsg, warningExpected = true)
        checkMainTargetsCompatibility(logMsg, warningExpected = false, disabled = true)
    }

    private fun TestProject.checkMainTargetsCompatibility(
        warningMessage: String,
        warningExpected: Boolean,
        disabled: Boolean = false
    ) {
        val disableProperty = ComposeProperties.DISABLE_LIBRARY_COMPATIBILITY_CHECK
        val additionalArgs = if (disabled) arrayOf("-P${disableProperty}=true") else emptyArray()

        gradle("jvmMainClasses", *additionalArgs).checks {
            if (disabled) {
                check.taskSkipped(":checkJvmMainComposeLibrariesCompatibility")
            } else {
                check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            }
            if (warningExpected) check.logContains(warningMessage) else check.logDoesntContain(warningMessage)
        }

        gradle("wasmJsMainClasses", *additionalArgs).checks {
            if (disabled) {
                check.taskSkipped(":checkWasmJsMainComposeLibrariesCompatibility")
            } else {
                check.taskSuccessful(":checkWasmJsMainComposeLibrariesCompatibility")
            }
            if (warningExpected) check.logContains(warningMessage) else check.logDoesntContain(warningMessage)
        }

        if (currentOS == OS.MacOS) {
            gradle("compileKotlinIosSimulatorArm64", *additionalArgs).checks {
                if (disabled) {
                    check.taskSkipped(":checkIosSimulatorArm64MainComposeLibrariesCompatibility")
                } else {
                    check.taskSuccessful(":checkIosSimulatorArm64MainComposeLibrariesCompatibility")
                }
                if (warningExpected) check.logContains(warningMessage) else check.logDoesntContain(warningMessage)
            }
        }
    }
}

// lifecycle-viewmodel-compose:2.8.3 transitively pulls old compose.ui:1.6.11,
// which in turn depends on skiko:0.8.4.
private const val OLD_COMPOSE_DEPENDENCY = "org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3"
private const val OLD_SKIKO_DEPENDENCY = "org.jetbrains.skiko:skiko:0.8.4"
