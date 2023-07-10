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

package androidx.build.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Incident
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LocationType
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.isKotlin
import com.android.tools.lint.model.LintModelMavenName
import com.intellij.psi.PsiJvmModifiersOwner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod

/**
 * This lint check is meant to help maintain binary compatibility in a one-time transition to using
 * `-Xjvm-default=all`. Applicable interfaces which existed before `-Xjvm-default=all` was used must
 * be annotated with @JvmDefaultWithCompatibility. However, after the initial change, new interfaces
 * should not use @JvmDefaultWithCompatibility.
 *
 * Because this check is only meant to be used once, it should not be added to the issue registry.
 */
class MissingJvmDefaultWithCompatibilityDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes() = listOf(UClass::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return InterfaceChecker(context)
    }

    fun LintModelMavenName.asProjectString() = "$groupId.$artifactId"

    private inner class InterfaceChecker(val context: JavaContext) : UElementHandler() {
        override fun visitClass(node: UClass) {
            // Don't run lint on the set of projects that already used `-Xjvm-default=all` before
            // all projects were switched over.
            if (alreadyDefaultAll.contains(context.project.mavenCoordinate?.asProjectString())) {
                return
            }

            if (!isKotlin(node)) return
            if (!node.isInterface) return
            if (node.annotatedWithAnyOf(
                    // If the interface is not stable, it doesn't need the annotation
                    BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS +
                        // If the interface already has the annotation, it doesn't need it again
                        JVM_DEFAULT_WITH_COMPATIBILITY)
            ) return

            val stableMethods = node.stableMethods()
            if (stableMethods.any { it.hasDefaultImplementation() }) {
                val reason = "This interface must be annotated with @JvmDefaultWithCompatibility " +
                    "because it has a stable method with a default implementation"
                reportIncident(node, reason)
                return
            }

            if (stableMethods.any { it.hasParameterWithDefaultValue() }) {
                val reason = "This interface must be annotated with @JvmDefaultWithCompatibility " +
                    "because it has a stable method with a parameter with a default value"
                reportIncident(node, reason)
                return
            }

            // This only checks the interfaces that this interface directly extends, which means if
            // A extends B extends C and C is @JvmDefaultWithCompatibility, there will need to be
            // two passes of running the check to annotate A and B.
            if (node.interfaces.any {
                    it.annotatedWithAnyOf(listOf(JVM_DEFAULT_WITH_COMPATIBILITY))
            }) {
                val reason = "This interface must be annotated with @JvmDefaultWithCompatibility " +
                    "because it implements an interface which uses this annotation"
                reportIncident(node, reason)
                return
            }
        }

        private fun reportIncident(node: UClass, reason: String) {
            val location = context.getLocation(node, LocationType.ALL)
            val fix = fix()
                .name("Annotate with @JvmDefaultWithCompatibility")
                .annotate(JVM_DEFAULT_WITH_COMPATIBILITY)
                .range(location)
                .autoFix()
                .build()

            val incident = Incident(context)
                .fix(fix)
                .issue(ISSUE)
                .location(location)
                .message(reason)
                .scope(node)

            context.report(incident)
        }

        /**
         * Returns a list of the class's stable methods (methods not labelled as experimental).
         */
        private fun UClass.stableMethods(): List<UMethod> =
            methods.filter {
                !it.annotatedWithAnyOf(BanInappropriateExperimentalUsage.APPLICABLE_ANNOTATIONS)
            }

        /**
         * Checks if the element is annotated with any of the provided (fully qualified) annotation
         * names. This uses `PsiJvmModifiersOwner` because it seems to be the one common parent of
         * `UClass` and `UMethod` with an `annotations` property.
         */
        private fun PsiJvmModifiersOwner.annotatedWithAnyOf(
            qualifiedAnnotationNames: List<String>
        ): Boolean = annotations.any { qualifiedAnnotationNames.contains(it.qualifiedName) }

        private fun UMethod.hasDefaultImplementation(): Boolean =
            uastBody != null

        private fun UMethod.hasParameterWithDefaultValue(): Boolean =
            uastParameters.any { param -> param.uastInitializer != null }
    }

    companion object {
        val ISSUE = Issue.create(
            "MissingJvmDefaultWithCompatibility",
            "The @JvmDefaultWithCompatibility needs to be used with on applicable " +
                "interfaces when `-Xjvm-default=all` is turned on to preserve compatibility.",
            "Libraries that pass `-Xjvm-default=all` to the Kotlin compiler must " +
                "use the @JvmDefaultWithCompatibility annotation on previously existing " +
                "interfaces with stable methods with default implementations or default parameter" +
                " values, and interfaces that extend other @JvmDefaultWithCompatibility " +
                "interfaces. See go/androidx-api-guidelines#kotlin-jvm-default for more details.",
            Category.CORRECTNESS, 5, Severity.ERROR,
            Implementation(
                MissingJvmDefaultWithCompatibilityDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )

        const val JVM_DEFAULT_WITH_COMPATIBILITY = "kotlin.jvm.JvmDefaultWithCompatibility"

        // This set of projects was created by running `grep "Xjvm-default=all" . -r` in the
        // `frameworks/support` directory and converting the `build.gradle` files in that list to
        // this format.
        private val alreadyDefaultAll = setOf(
            "androidx.room.room-compiler-processing",
            "androidx.room.room-migration",
            "androidx.room.room-testing",
            "androidx.room.room-compiler",
            "androidx.room.room-ktx",
            "androidx.room.room-common",
            "androidx.room.room-runtime",
            "androidx.compose.ui.ui",
            "androidx.compose.ui.ui-unit",
            "androidx.compose.ui.ui-tooling-preview",
            "androidx.compose.ui.ui-tooling-data",
            "androidx.compose.ui.ui-util",
            "androidx.compose.ui.ui-test",
            "androidx.compose.ui.ui-test-manifest",
            "androidx.compose.ui.ui-inspection",
            "androidx.compose.ui.ui-viewbinding",
            "androidx.compose.ui.ui-geometry",
            "androidx.compose.ui.ui-graphics",
            "androidx.compose.ui.ui-text",
            "androidx.compose.ui.ui-text-google-fonts",
            "androidx.compose.ui.ui-test-junit4",
            "androidx.compose.ui.ui-tooling",
            "androidx.compose.test-utils",
            "androidx.compose.runtime.runtime",
            "androidx.compose.runtime.runtime-livedata",
            "androidx.compose.runtime.runtime-saveable",
            "androidx.compose.runtime.runtime-rxjava2",
            "androidx.compose.runtime.runtime-tracing",
            "androidx.compose.runtime.runtime-rxjava3",
            "androidx.compose.animation.animation-tooling-internal",
            "androidx.compose.animation.animation",
            "androidx.compose.animation.animation-graphics",
            "androidx.compose.animation.animation-core",
            "androidx.compose.foundation.foundation",
            "androidx.compose.foundation.foundation-layout",
            "androidx.compose.material3.material3-window-size-class",
            "androidx.compose.material3.material3.integration-tests.material3-catalog",
            "androidx.compose.material3.material3",
            "androidx.compose.material.material-ripple",
            "androidx.lifecycle.lifecycle-viewmodel",
            "androidx.sqlite.sqlite-ktx",
            "androidx.sqlite.sqlite-framework",
            "androidx.sqlite.integration-tests.inspection-sqldelight-testapp",
            "androidx.sqlite.integration-tests.inspection-room-testapp",
            "androidx.sqlite.sqlite",
            "androidx.sqlite.sqlite-inspection",
            "androidx.tv.tv-foundation",
            "androidx.tv.tv-material",
            "androidx.window.window",
            "androidx.credentials.credentials",
            "androidx.wear.compose.compose-material",
            "androidx.wear.watchface.watchface-complications-data-source",
            "androidx.wear.watchface.watchface",
            "androidx.wear.watchface.watchface-client",
            "androidx.lifecycle.lifecycle-common",
            // These projects didn't already have "Xjvm-default=al", but the only have the error in
            // integration tests, where the annotation isn't needed.
            "androidx.annotation.annotation-experimental-lint-integration-tests",
            "androidx.annotation.annotation-experimental-lint",
            "androidx.camera.integration-tests.camera-testapp-camera2-pipe",
            "androidx.compose.integration-tests.docs-snippets",
            // These projects are excluded due to b/259578592
            "androidx.camera.camera-camera2-pipe",
            "androidx.camera.camera-camera2-pipe-integration",
            "androidx.camera.camera-camera2-pipe-testing",
        )
    }
}