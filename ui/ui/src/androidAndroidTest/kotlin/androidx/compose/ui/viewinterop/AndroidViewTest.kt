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

package androidx.compose.ui.viewinterop

import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.compose.ui.Modifier
import androidx.compose.ui.onPositioned
import androidx.compose.ui.test.R
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.Box
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.setContent
import androidx.ui.test.android.createAndroidComposeRule
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.roundToInt

@SmallTest
@RunWith(JUnit4::class)
class AndroidViewTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun androidViewWithConstructor() {
        composeTestRule.setContent {
            AndroidView({ TextView(it).apply { text = "Test" } })
        }
        Espresso
            .onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest() {
        composeTestRule.setContent {
            AndroidView({ LayoutInflater.from(it).inflate(R.layout.test_layout, null) })
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithViewTest() {
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        composeTestRule.setContent {
            AndroidView({ frameLayout })
        }
        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest_preservesLayoutParams() {
        composeTestRule.setContent {
            AndroidView({
                LayoutInflater.from(it).inflate(R.layout.test_layout, FrameLayout(it), false)
            })
        }
        Espresso
            .onView(withClassName(endsWith("RelativeLayout")))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                if (view.layoutParams.width != 300.dp.toPx(view.context.resources.displayMetrics)) {
                    throw exception
                }
                if (view.layoutParams.height != WRAP_CONTENT) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewProperlyDetached() {
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        var emit by mutableStateOf(true)
        composeTestRule.setContent {
            if (emit) {
                AndroidView({ frameLayout })
            }
        }

        runOnUiThread {
            assertThat(frameLayout.parent).isNotNull()
            emit = false
        }

        runOnIdle {
            assertThat(frameLayout.parent).isNull()
        }
    }

    @Test
    fun androidView_attachedAfterDetached_addsViewBack() {
        lateinit var root: FrameLayout
        lateinit var composeView: ComposeView
        lateinit var viewInsideCompose: View
        composeTestRule.activityRule.scenario.onActivity { activity ->
            root = FrameLayout(activity)
            composeView = ComposeView(activity)
            viewInsideCompose = View(activity)

            activity.setContentView(root)
            root.addView(composeView)
            composeView.setContent {
                AndroidView({ viewInsideCompose })
            }
        }

        var viewInsideComposeHolder: ViewGroup? = null
        runOnUiThread {
            assertThat(viewInsideCompose.parent).isNotNull()
            viewInsideComposeHolder = viewInsideCompose.parent as ViewGroup
            root.removeView(composeView)
        }

        runOnIdle {
            assertThat(viewInsideCompose.parent).isNull()
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(0)
            root.addView(composeView)
        }

        runOnIdle {
            assertThat(viewInsideCompose.parent).isEqualTo(viewInsideComposeHolder)
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
        }
    }

    @Test
    fun androidViewWithResource_modifierIsApplied() {
        val size = 20.dp
        composeTestRule.setContent {
            AndroidView(
                { LayoutInflater.from(it).inflate(R.layout.test_layout, null) },
                Modifier.size(size)
            )
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidViewWithView_modifierIsApplied() {
        val size = 20.dp
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity)
        }
        composeTestRule.setContent {
            AndroidView({ frameLayout }, Modifier.size(size))
        }

        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidViewWithView_drawModifierIsApplied() {
        val size = 300
        lateinit var frameLayout: FrameLayout
        composeTestRule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
        }
        composeTestRule.setContent {
            AndroidView({ frameLayout }, Modifier.testTag("view").background(color = Color.Blue))
        }

        onNodeWithTag("view").captureToBitmap().assertPixels(IntSize(size, size)) {
            Color.Blue
        }
    }

    @Test
    fun androidViewWithResource_modifierIsCorrectlyChanged() {
        val size = mutableStateOf(20.dp)
        composeTestRule.setContent {
            AndroidView(
                { LayoutInflater.from(it).inflate(R.layout.test_layout, null) },
                Modifier.size(size.value)
            )
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
        runOnIdle { size.value = 30.dp }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
            .check { view, exception ->
                val expectedSize = size.value.toPx(view.context.resources.displayMetrics)
                if (view.width != expectedSize || view.height != expectedSize) {
                    throw exception
                }
            }
    }

    @Test
    fun androidView_notDetachedFromWindowTwice() {
        // Should not crash.
        composeTestRule.setContent {
            Box {
                AndroidView(::FrameLayout) {
                    it.setContent(Recomposer()) {
                        Box()
                    }
                }
            }
        }
    }

    @Test
    fun androidView_updateObservesStateChanges() {
        var size by mutableStateOf(20)
        var obtainedSize: IntSize = IntSize.Zero
        composeTestRule.setContent {
            Box {
                AndroidView(::View, Modifier.onPositioned { obtainedSize = it.size }) { view ->
                    view.layoutParams = ViewGroup.LayoutParams(size, size)
                }
            }
        }
        runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
            size = 40
        }
        runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
        }
    }

    private fun Dp.toPx(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            displayMetrics
        ).roundToInt()
}
