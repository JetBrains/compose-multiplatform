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

package androidx.compose.ui.platform

import android.view.ViewGroup
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.tests.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @Test
    fun composeViewIsTransitionGroup() {
        val view = ComposeView(rule.activity)
        assertTrue("ComposeView isTransitionGroup by default", view.isTransitionGroup)
    }

    @Test
    fun composeViewInflatesTransitionGroup() {
        val view = rule.activity.layoutInflater.inflate(
            R.layout.composeview_transition_group_false,
            null
        ) as ViewGroup
        assertFalse("XML overrides ComposeView.isTransitionGroup", view.isTransitionGroup)
    }
}