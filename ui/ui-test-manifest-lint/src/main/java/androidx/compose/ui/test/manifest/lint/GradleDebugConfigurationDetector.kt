/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.test.manifest.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.GradleContext
import com.android.tools.lint.detector.api.GradleScanner
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity

@Suppress("UnstableApiUsage")
class GradleDebugConfigurationDetector : Detector(), GradleScanner {
    private val supportedConfigurations = setOf(
        "implementation",
        "api",
        "compileOnly",
        "runtimeOnly",
        "annotationProcessor",
        "lintChecks",
        "lintPublish",
    ).map { it.lowercase() }

    private val blockPrefixes = setOf(
        "android",
        "test"
    )

    private val TargetConfigutation = "debugImplementation".lowercase()

    companion object {
        val ISSUE = Issue.create(
            id = "TestManifestGradleConfiguration",
            briefDescription = "The ui-test-manifest library should be included using the " +
                "debugImplementation configuration.",
            explanation = "The androidx.compose.ui:ui-test-manifest dependency is needed for " +
                "launching a Compose host, such as with createComposeRule. " +
                "However, it only needs to be present in testing configurations " +
                "therefore use this dependency with the debugImplementation configuration",
            category = Category.CORRECTNESS,
            severity = Severity.WARNING,
            implementation = Implementation(
                GradleDebugConfigurationDetector::class.java, Scope.GRADLE_SCOPE
            ),
            androidSpecific = true
        ).addMoreInfo("https://developer.android.com/jetpack/compose/testing#setup")
    }

    override fun checkDslPropertyAssignment(
        context: GradleContext,
        property: String,
        value: String,
        parent: String,
        parentParent: String?,
        valueCookie: Any,
        statementCookie: Any
    ) {
        // 1) Check if library is correct
        val library = getStringLiteralValue(value)
        if (!library.startsWith("androidx.compose.ui:ui-test-manifest")) return

        val cleanedProperty = property.lowercase()
        // 2) Check if property is within allowList types (catches custom configurations)
        if (!supportedConfigurations.any { cleanedProperty.contains(it) }) return

        // At this point only *implementation configurations with manifest library are here
        // 3) Search for debugImplementation explicetly

        if (cleanedProperty.contains(TargetConfigutation)) return

        // 3) Only throw the error if we start with the blocked configs
        if (blockPrefixes.any { cleanedProperty.startsWith(it) } || supportedConfigurations.any {
                cleanedProperty.startsWith(
                    it
                )
            }) {
            context.report(
                ISSUE, statementCookie, context.getLocation(statementCookie),
                "Please use debugImplementation.",
                fix().replace()
                    .text(property)
                    .with("debugImplementation")
                    .build()
            )
        }
    }

    private fun getStringLiteralValue(value: String): String {
        if (value.length > 2 && (
                value.startsWith("'") && value.endsWith("'") ||
                    value.startsWith("\"") && value.endsWith("\"")
                )
        ) {
            return value.substring(1, value.length - 1)
        }
        return ""
    }
}