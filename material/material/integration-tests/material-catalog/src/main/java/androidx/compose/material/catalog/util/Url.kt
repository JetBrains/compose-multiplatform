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

package androidx.compose.material.catalog.util

import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openUrl(url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    startActivity(intent)
}

const val GUIDELINES_URL = "https://material.io/components"
const val DOCS_URL = "https://developer.android.com/jetpack/androidx/releases/compose-material"
const val SOURCE_URL = "https://cs.android.com/androidx/platform/frameworks/support/+/" +
    "androidx-main:compose/material/"
const val ISSUE_URL = "https://issuetracker.google.com/issues/new?component=742043"
