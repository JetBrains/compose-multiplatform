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

package androidx.compose.ui.test.junit4

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ViewVisibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun setupComposeView(visibility: Int) {
        rule.activityRule.scenario.onActivity {
            val composeView = ComposeView(it)
            composeView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            composeView.visibility = visibility
            composeView.setContent {
                Text("Hello")
            }
            it.setContentView(composeView)
        }
    }

    private fun setupHostView(visibility: Int) {
        rule.activityRule.scenario.onActivity {
            it.setContent {
                val hostView = LocalView.current
                SideEffect {
                    hostView.visibility = visibility
                }
                Text("Hello")
            }
        }
    }

    @Test
    fun composeView_gone() {
        setupComposeView(View.GONE)
        rule.onNodeWithText("Hello")
            .assertExists()
            .assertIsNotDisplayed()
    }

    @Test
    fun composeView_invisible() {
        setupComposeView(View.INVISIBLE)
        rule.onNodeWithText("Hello")
            .assertExists()
            .assertIsNotDisplayed()
    }

    @Test
    fun composeView_visible() {
        setupComposeView(View.VISIBLE)
        rule.onNodeWithText("Hello")
            .assertExists()
            .assertIsDisplayed()
    }

    @Test
    fun hostView_gone() {
        setupHostView(View.GONE)
        rule.onNodeWithText("Hello")
            .assertExists()
            .assertIsNotDisplayed()
    }

    @Test
    fun hostView_invisible() {
        setupHostView(View.INVISIBLE)
        rule.onNodeWithText("Hello")
            .assertExists()
            .assertIsNotDisplayed()
    }

    @Test
    fun hostView_visible() {
        setupHostView(View.VISIBLE)
        rule.onNodeWithText("Hello")
            .assertExists()
            .assertIsDisplayed()
    }
}
