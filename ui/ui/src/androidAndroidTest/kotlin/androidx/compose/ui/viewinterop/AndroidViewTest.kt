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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Providers
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.R
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.test.assertPixels
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createAndroidComposeRule
import androidx.ui.test.onNodeWithTag
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.endsWith
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class AndroidViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun androidViewWithConstructor() {
        rule.setContent {
            AndroidView({ TextView(it).apply { text = "Test" } })
        }
        Espresso
            .onView(instanceOf(TextView::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest() {
        rule.setContent {
            AndroidView({ LayoutInflater.from(it).inflate(R.layout.test_layout, null) })
        }
        Espresso
            .onView(instanceOf(RelativeLayout::class.java))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithViewTest() {
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        rule.setContent {
            AndroidView({ frameLayout })
        }
        Espresso
            .onView(equalTo(frameLayout))
            .check(matches(isDisplayed()))
    }

    @Test
    fun androidViewWithResourceTest_preservesLayoutParams() {
        rule.setContent {
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
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(300, 300)
            }
        }
        var emit by mutableStateOf(true)
        rule.setContent {
            if (emit) {
                AndroidView({ frameLayout })
            }
        }

        rule.runOnUiThread {
            assertThat(frameLayout.parent).isNotNull()
            emit = false
        }

        rule.runOnIdle {
            assertThat(frameLayout.parent).isNull()
        }
    }

    @Test
    @LargeTest
    fun androidView_attachedAfterDetached_addsViewBack() {
        lateinit var root: FrameLayout
        lateinit var composeView: ComposeView
        lateinit var viewInsideCompose: View
        rule.activityRule.scenario.onActivity { activity ->
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
        rule.runOnUiThread {
            assertThat(viewInsideCompose.parent).isNotNull()
            viewInsideComposeHolder = viewInsideCompose.parent as ViewGroup
            root.removeView(composeView)
        }

        rule.runOnIdle {
            // Views don't detach from the parent when the parent is detached
            assertThat(viewInsideCompose.parent).isNotNull()
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
            root.addView(composeView)
        }

        rule.runOnIdle {
            assertThat(viewInsideCompose.parent).isEqualTo(viewInsideComposeHolder)
            assertThat(viewInsideComposeHolder?.childCount).isEqualTo(1)
        }
    }

    @Test
    fun androidViewWithResource_modifierIsApplied() {
        val size = 20.dp
        rule.setContent {
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
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity)
        }
        rule.setContent {
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
    @LargeTest
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    fun androidViewWithView_drawModifierIsApplied() {
        val size = 300
        lateinit var frameLayout: FrameLayout
        rule.activityRule.scenario.onActivity { activity ->
            frameLayout = FrameLayout(activity).apply {
                layoutParams = ViewGroup.LayoutParams(size, size)
            }
        }
        rule.setContent {
            AndroidView({ frameLayout }, Modifier.testTag("view").background(color = Color.Blue))
        }

        rule.onNodeWithTag("view").captureToBitmap().assertPixels(IntSize(size, size)) {
            Color.Blue
        }
    }

    @Test
    fun androidViewWithResource_modifierIsCorrectlyChanged() {
        val size = mutableStateOf(20.dp)
        rule.setContent {
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
        rule.runOnIdle { size.value = 30.dp }
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
        rule.setContent {
            Box {
                AndroidView(::FrameLayout) {
                    it.setContent(Recomposer.current()) {
                        Box(Modifier)
                    }
                }
            }
        }
    }

    @Test
    fun androidView_updateObservesStateChanges() {
        var size by mutableStateOf(20)
        var obtainedSize: IntSize = IntSize.Zero
        rule.setContent {
            Box {
                AndroidView(
                    ::View,
                    Modifier.onGloballyPositioned { obtainedSize = it.size }
                ) { view ->
                    view.layoutParams = ViewGroup.LayoutParams(size, size)
                }
            }
        }
        rule.runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
            size = 40
        }
        rule.runOnIdle {
            assertThat(obtainedSize).isEqualTo(IntSize(size, size))
        }
    }

    @Test
    fun androidView_propagatesDensity() {
        rule.setContent {
            val size = 50.dp
            val density = Density(3f)
            val sizeIpx = with(density) { size.toIntPx() }
            Providers(DensityAmbient provides density) {
                AndroidView(
                    { FrameLayout(it) },
                    Modifier.size(size).onGloballyPositioned {
                        assertThat(it.size).isEqualTo(IntSize(sizeIpx, sizeIpx))
                    }
                )
            }
        }
        rule.waitForIdle()
    }

    private fun Dp.toPx(displayMetrics: DisplayMetrics) =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            displayMetrics
        ).roundToInt()
}
