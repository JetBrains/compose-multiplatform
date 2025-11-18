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
            check.logDoesntContain("w: runtime dependency version mismatch!")
        }
        gradle("metadataMainClasses").checks {
            check.logDoesntContain("checkMetadataMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: runtime dependency version mismatch!")
        }
        gradle("jvmMainClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: runtime dependency version mismatch!")
        }
        gradle("jvmTestClasses").checks {
            check.taskSuccessful(":checkJvmMainComposeLibrariesCompatibility")
            check.taskSuccessful(":checkJvmTestComposeLibrariesCompatibility")
            check.logDoesntContain("w: runtime dependency version mismatch!")
        }
        gradle("wasmJsMainClasses").checks {
            check.taskSuccessful(":checkWasmJsMainComposeLibrariesCompatibility")
            check.logDoesntContain("w: runtime dependency version mismatch!")
        }

        if (currentOS == OS.MacOS) {
            gradle("compileKotlinIosSimulatorArm64").checks {
                check.taskSuccessful(":checkIosSimulatorArm64MainComposeLibrariesCompatibility")
                check.logDoesntContain("w: runtime dependency version mismatch!")
            }
        }

        file("build.gradle.kts").modify {
            it.replace(
                "api(\"org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}\")",
                "api(\"org.jetbrains.compose.ui:ui\") { version { strictly(\"1.9.3\") } }"
            )
        }
        val msg = "w: runtime dependency version mismatch!\n\t" +
                "expected: 'org.jetbrains.compose.ui:ui:${defaultTestEnvironment.composeVersion}', " +
                "actual: 'org.jetbrains.compose.ui:ui:1.9.3'"
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