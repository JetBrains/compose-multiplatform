/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.benchmark.input.pointer

import android.content.Context
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

/**
 * Benchmark for simply tapping on an item in Android.
 *
 * The intent is to measure the speed of all parts necessary for a normal tap starting from
 * MotionEvents getting dispatched to a particular view. The test therefore includes hit
 * testing and dispatch.
 *
 * This is intended to be an equivalent counterpart to [ComposeTapIntegrationBenchmark].
 *
 * The hierarchy is set up to look like:
 * rootView
 *   -> LinearLayout
 *     -> CustomView (with click listener)
 *       -> TextView
 *       -> TextView
 *       -> TextView
 *       -> ...
 *
 * MotionEvents are dispatched to rootView as ACTION_DOWN followed by ACTION_UP.  The validity of
 * the test is verified in a custom click listener in CustomView with
 * com.google.common.truth.Truth.assertThat and by counting the clicks in the click listener and
 * later verifying that they count is sufficiently high.
 *
 * The reason a CustomView is used with a custom click listener is that View's normal click
 * listener is called via a posted Runnable, which is problematic for the benchmark library and
 * less equivalent to what Compose does anyway.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidTapIntegrationBenchmark {

    private lateinit var rootView: View
    private lateinit var expectedLabel: String

    private var actualClickCount = 0
    private var expectedClickCount = 0

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(TestActivity::class.java)

    @Before
    fun setup() {
        val activity = activityTestRule.activity
        Assert.assertTrue(
            "timed out waiting for activity focus",
            activity.hasFocusLatch.await(5, TimeUnit.SECONDS)
        )

        rootView = activity.findViewById<ViewGroup>(android.R.id.content)

        activityTestRule.runOnUiThread {

            val children = (0 until NumItems).map { i ->
                CustomView(activity).apply {
                    layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, ItemHeightPx.toInt())
                    label = "$i"
                    clickListener = {
                        assertThat(this.label).isEqualTo(expectedLabel)
                        actualClickCount++
                    }
                }
            }

            val linearLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                children.forEach {
                    addView(it)
                }
            }

            activity.setContentView(linearLayout)
        }
    }

    // This test requires more hit test processing so changes to hit testing will be tracked more
    // by this test.
    @UiThreadTest
    @Test
    @Ignore("We don't want this to show up in our benchmark CI tests.")
    fun clickOnLateItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at 0 will be hit tested late.
        clickOnItem(0, "0")
    }

    // This test requires less hit testing so changes to dispatch will be tracked more by this test.
    @UiThreadTest
    @Test
    @Ignore("We don't want this to show up in our benchmark CI tests.")
    fun clickOnEarlyItem() {
        // As items that are laid out last are hit tested first (so z order is respected), item
        // at NumItems - 1 will be hit tested early.
        val lastItem = NumItems - 1
        clickOnItem(lastItem, "$lastItem")
    }

    private fun clickOnItem(item: Int, expectedLabel: String) {

        this.expectedLabel = expectedLabel

        // half height of an item + top of the chosen item = middle of the chosen item
        val y = (ItemHeightPx / 2) + (item * ItemHeightPx)

        val down = MotionEvent(
            0,
            ACTION_DOWN,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(0f, y)),
            rootView
        )

        val up = MotionEvent(
            10,
            ACTION_UP,
            1,
            0,
            arrayOf(PointerProperties(0)),
            arrayOf(PointerCoords(0f, y)),
            rootView
        )

        benchmarkRule.measureRepeated {
            rootView.dispatchTouchEvent(down)
            rootView.dispatchTouchEvent(up)
            expectedClickCount++
        }

        assertThat(actualClickCount).isEqualTo(expectedClickCount)
    }
}

private class CustomView(context: Context) : FrameLayout(context) {
    var label: String
        get() = textView.text.toString()
        set(value) {
            textView.text = value
        }

    lateinit var clickListener: () -> Unit

    val textView: TextView = TextView(context).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
    }

    init {
        addView(textView)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event!!.actionMasked == ACTION_UP) {
            clickListener.invoke()
        }

        return true
    }
}