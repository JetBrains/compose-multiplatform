/*
 * Copyright 2020 The Android Open Source Project
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

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@MediumTest
@OptIn(ExperimentalTestApi::class)
class FirstDrawTest {

    /**
     * Tests that the compose tree has been drawn at least once when
     * [ComposeUiTest.setContent] finishes.
     */
    @LargeTest
    @Test
    fun waitsForFirstDraw_withoutOnIdle() = runComposeUiTest {
        var drawn = false
        setContent {
            Canvas(Modifier.fillMaxSize()) {
                drawn = true
            }
        }
        // waitForIdle() shouldn't be necessary
        assertThat(drawn).isTrue()
    }

    /**
     * Tests that [ComposeUiTest.waitForIdle] doesn't timeout when the compose tree is
     * completely off-screen and will hence not be drawn.
     */
    @Test
    fun waitsForOutOfBoundsComposeView() = runAndroidComposeUiTest<ComponentActivity> {
        var drawn = false

        runOnUiThread {
            // Set the compose content in a FrameLayout that is completely placed out of the
            // screen, and set clipToPadding to make sure the content won't be drawn.

            val root = object : FrameLayout(activity!!) {
                override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
                    // Place our child out of bounds
                    getChildAt(0).layout(-200, 0, -100, 100)
                }
            }.apply {
                // Enforce clipping:
                setPadding(1, 1, 1, 1)
                clipToPadding = true
            }

            val outOfBoundsView = ComposeView(activity!!).apply {
                layoutParams = ViewGroup.MarginLayoutParams(100, 100)
            }

            root.addView(outOfBoundsView)
            activity!!.setContentView(root)
            outOfBoundsView.setContent {
                // If you see this box when running the test, the test is setup incorrectly
                Canvas(Modifier.fillMaxSize()) {
                    drawRect(Color.Yellow)
                    drawn = true
                }
            }
        }

        // onIdle shouldn't timeout
        waitForIdle()
        // The compose view was off-screen, so it hasn't drawn yet
        assertThat(drawn).isFalse()
    }
}