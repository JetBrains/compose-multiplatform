/*
 * Copyright 2018 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class AndroidXIssueRegistry : IssueRegistry() {
    override val minApi = CURRENT_API
    override val api = 14
    override val issues get(): List<Issue> {
        return Issues
    }
    override val vendor = Vendor(
        feedbackUrl = "https://issuetracker.google.com/issues/new?component=1147525",
        identifier = "androidx.build",
        vendorName = "Android Open Source Project",
    )

    companion object {
        val Issues get(): List<Issue> {
            return listOf(
                AndroidManifestServiceExportedDetector.ISSUE,
                BanParcelableUsage.ISSUE,
                BanConcurrentHashMap.ISSUE,
                BanInappropriateExperimentalUsage.ISSUE,
                BanInappropriateExperimentalUsage.NULL_ANNOTATION_GROUP_ISSUE,
                BanInlineOptIn.ISSUE,
                BanKeepAnnotation.ISSUE,
                TargetApiAnnotationUsageDetector.ISSUE,
                // If you add more SampledAnnotationDetector issues here, you
                // MUST also update `buildSrc/lint_samples.xml` to ensure they
                // run against samples projects.
                SampledAnnotationDetector.OBSOLETE_SAMPLED_ANNOTATION,
                SampledAnnotationDetector.UNRESOLVED_SAMPLE_LINK,
                SampledAnnotationDetector.MULTIPLE_FUNCTIONS_FOUND,
                SampledAnnotationDetector.INVALID_SAMPLES_LOCATION,
                TestSizeAnnotationEnforcer.MISSING_TEST_SIZE_ANNOTATION,
                TestSizeAnnotationEnforcer.UNEXPECTED_TEST_SIZE_ANNOTATION,
                TestSizeAnnotationEnforcer.UNSUPPORTED_TEST_RUNNER,
                BanUncheckedReflection.ISSUE,
                ObsoleteBuildCompatUsageDetector.ISSUE,
                BanSynchronizedMethods.ISSUE,
                MetadataTagInsideApplicationTagDetector.ISSUE,
                PrivateConstructorForUtilityClassDetector.ISSUE,
                ClassVerificationFailureDetector.ISSUE,
                IdeaSuppressionDetector.ISSUE,
                CameraXQuirksClassDetector.ISSUE,
                NullabilityAnnotationsDetector.ISSUE,
                IgnoreClassLevelDetector.ISSUE,
                ExperimentalPropertyAnnotationDetector.ISSUE,
                // MissingJvmDefaultWithCompatibilityDetector is intentionally left out of the
                // registry, see comments on the class for more details.
            )
        }
    }
}
