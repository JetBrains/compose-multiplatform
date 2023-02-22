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
package androidx.compose.foundation.layout

import android.view.View
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.test.filters.FlakyTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@MediumTest
class WindowInsetsAnimationTest {
    @get:Rule
    val rule = createAndroidComposeRule<WindowInsetsActionBarActivity>()

    @Before
    fun setup() {
        rule.activity.createdLatch.await(1, TimeUnit.SECONDS)
        rule.activity.attachedToWindowLatch.await(1, TimeUnit.SECONDS)
    }

    @After
    fun teardown() {
        rule.runOnUiThread {
            val window = rule.activity.window
            val view = window.decorView
            WindowInsetsControllerCompat(window, view).hide(WindowInsetsCompat.Type.ime())
        }
    }

    @SdkSuppress(minSdkVersion = 22) // b/266742122
    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun imeAnimationWhenShowingIme() {
        val imeAnimationSourceValues = mutableListOf<Int>()
        val imeAnimationTargetValues = mutableListOf<Int>()
        val focusRequester = FocusRequester()
        rule.setContent {
            val density = LocalDensity.current
            val source = WindowInsets.imeAnimationSource
            val target = WindowInsets.imeAnimationTarget
            val sourceBottom = source.getBottom(density)
            imeAnimationSourceValues += sourceBottom
            val targetBottom = target.getBottom(density)
            imeAnimationTargetValues += targetBottom
            BasicTextField(
                value = "Hello World",
                onValueChange = {},
                Modifier.focusRequester(focusRequester)
            )
        }

        rule.waitForIdle()
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.waitForIdle()
        rule.waitUntil(timeoutMillis = 3000) {
            imeAnimationSourceValues.last() > imeAnimationTargetValues.first()
        }

        rule.waitUntil(timeoutMillis = 3000) {
            imeAnimationTargetValues.last() == imeAnimationSourceValues.last()
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @FlakyTest(bugId = 256020254)
    @Test
    fun imeAnimationWhenHidingIme() {
        val imeAnimationSourceValues = mutableListOf<Int>()
        val imeAnimationTargetValues = mutableListOf<Int>()
        val focusRequester = FocusRequester()
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            val density = LocalDensity.current
            val source = WindowInsets.imeAnimationSource
            val target = WindowInsets.imeAnimationTarget
            val sourceBottom = source.getBottom(density)
            imeAnimationSourceValues += sourceBottom
            val targetBottom = target.getBottom(density)
            imeAnimationTargetValues += targetBottom
            BasicTextField(
                value = "Hello World",
                onValueChange = {},
                Modifier.focusRequester(focusRequester)
            )
        }

        rule.waitForIdle()
        rule.runOnUiThread {
            focusRequester.requestFocus()
        }

        rule.waitUntil(timeoutMillis = 3000) {
            val target = imeAnimationTargetValues.last()
            val source = imeAnimationSourceValues.last()
            target > imeAnimationSourceValues.first() && target == source
        }

        rule.runOnUiThread {
            val window = rule.activity.window
            WindowInsetsControllerCompat(window, view).hide(WindowInsetsCompat.Type.ime())
        }

        rule.waitForIdle()

        rule.waitUntil(timeoutMillis = 3000) {
            imeAnimationTargetValues.last() == imeAnimationSourceValues.first()
        }
    }
}
