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

package androidx.compose.ui.tooling

import android.content.Intent
import android.view.ViewGroup
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PreviewActivityTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(PreviewActivity::class.java)

    private lateinit var intent: Intent

    @Before
    fun setup() {
        intent = Intent(activityTestRule.activity, PreviewActivity::class.java)
    }

    @Test
    fun createActivityWithNoComposablePreview() {
        activityTestRule.launchActivity(intent)
        val mainContent = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
        assertThat(mainContent.childCount).isEqualTo(0)
    }

    @Test
    fun createActivityWithSimpleComposablePreview() {
        intent.putExtra(
            "composable",
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt.SimpleComposablePreview"
        )
        activityTestRule.launchActivity(intent)
        val mainContent = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
        assertThat(mainContent.childCount).isGreaterThan(0)
    }

    @Test
    fun createActivityWithPrivateComposablePreview() {
        intent.putExtra(
            "composable",
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt.PrivateSimpleComposablePreview"
        )
        activityTestRule.launchActivity(intent)
        val mainContent = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
        assertThat(mainContent.childCount).isGreaterThan(0)
    }

    @Test
    fun createActivityWithComposablePreviewWithinAClass() {
        intent.putExtra("composable", "androidx.compose.ui.tooling.TestGroup.InClassPreview")
        activityTestRule.launchActivity(intent)
        val mainContent = activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
        assertThat(mainContent.childCount).isGreaterThan(0)
    }
}
