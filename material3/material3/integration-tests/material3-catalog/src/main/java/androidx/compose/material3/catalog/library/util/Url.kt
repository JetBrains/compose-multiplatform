/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}

const val GuidelinesUrl = "https://m3.material.io"
const val ComponentGuidelinesUrl = "https://m3.material.io/components"
const val StyleGuidelinesUrl = "https://m3.material.io/styles"
const val ReleasesUrl = "https://developer.android.com/jetpack/androidx/releases/compose-material3"
const val DocsUrl = "https://developer.android.com/reference/kotlin/androidx/compose/material3"
const val PackageSummaryUrl = "https://developer.android.com/reference/kotlin/androidx/compose/" +
    "material3/package-summary"
const val SourceUrl = "https://cs.android.com/androidx/platform/frameworks/support/+/" +
    "androidx-main:compose/material3"
const val Material3SourceUrl = "https://cs.android.com/androidx/platform/frameworks/support/+/" +
    "androidx-main:compose/material3/" +
    "material3/src/commonMain/kotlin/androidx/compose/material3"
const val SampleSourceUrl = "https://cs.android.com/androidx/platform/frameworks/support/+/" +
    "androidx-main:compose/material3/" +
    "material3/samples/src/main/java/androidx/compose/material3/samples"
const val IssueUrl = "https://issuetracker.google.com/issues/new?component=742043"
const val TermsUrl = "https://policies.google.com/terms"
const val PrivacyUrl = "https://policies.google.com/privacy"
const val LicensesUrl = "https://cs.android.com/androidx/platform/frameworks/support/+/" +
    "androidx-main:LICENSE.txt"
