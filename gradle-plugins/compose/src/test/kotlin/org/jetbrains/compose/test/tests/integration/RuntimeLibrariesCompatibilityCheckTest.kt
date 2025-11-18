package org.jetbrains.compose.test.tests.integration

import org.jetbrains.compose.desktop.application.internal.ComposeProperties
import org.jetbrains.compose.internal.utils.OS
import org.jetbrains.compose.internal.utils.currentOS
import org.jetbrains.compose.test.utils.GradlePluginTestBase
import org.jetbrains.compose.test.utils.checks
import org.jetbrains.compose.test.utils.modify
import kotlin.test.Test

class RuntimeLibrariesCompatibilityCheckTest : GradlePluginTestBase() {

    @Test
    fun correctConfigurationDoesntPrintWarning(): Unit = with(
        testProject("misc/compatibilityLibCheck")
    ) {
        gradle("assembleAndroidMain").checks {
            check.logDoesntContain("checkAndroidMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
        }
        gradle("metadataMainClasses").checks {
            check.logDoesntContain("checkMetadataMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
        }
        gradle("jvmMainClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
        }
        gradle("jvmTestClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.taskSuccessful(":checkJvmTestComposeLibrariesCompatibility")
            check.logDoesntContain("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
        }
        gradle("wasmJsMainClasses").checks {
            check.taskSuccessful(":checkWasmJsMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
        }

        if (currentOS == OS.MacOS) {
            gradle("compileKotlinIosSimulatorArm64").checks {
                check.taskSuccessful(":checkIosSimulatorArm64MainComposeLibrariesCompatibility")
                check.logDoesntContain("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
            }
        }

        file("build.gradle.kts").modify {
            it.replace(
                "api(\"org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}\")",
                "api(\"org.jetbrains.compose.ui:ui\") { version { strictly(\"1.9.3\") } }"
            )
        }
        val msg = buildString {
            appendLine("w: Compose Multiplatform runtime dependencies version didn't match with plugin version.")
            appendLine("    expected: 'org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}'")
            appendLine("    actual:   'org.jetbrains.compose.ui:ui:1.9.3'")
        }
        gradle("jvmMainClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.logContains(msg)
        }
        gradle("wasmJsMainClasses").checks {
            check.taskSuccessful(":checkWasmJsMainComposeLibrariesCompatibility")
            check.logContains(msg)
        }

        if (currentOS == OS.MacOS) {
            gradle("compileKotlinIosSimulatorArm64").checks {
                check.taskSuccessful(":checkIosSimulatorArm64MainComposeLibrariesCompatibility")
                check.logContains(msg)
            }
        }
        val disableProperty = ComposeProperties.DISABLE_LIBRARY_COMPATIBILITY_CHECK
        gradle("jvmMainClasses", "-P${disableProperty}=true").checks {
            check.taskSkipped(":checkJvmMainComposeLibrariesCompatibility")
            check.logDoesntContain(msg)
        }
    }
}