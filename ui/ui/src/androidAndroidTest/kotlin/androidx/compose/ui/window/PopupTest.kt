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
package androidx.compose.ui.window

import android.view.View
import android.view.View.MEASURED_STATE_TOO_SMALL
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
class PopupTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    private val testTag = "testedPopup"
    private val offset = IntOffset(10, 10)
    private val popupSize = IntSize(40, 20)

    @Test
    fun isShowing() {
        rule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(Modifier.size(50.dp), content = {})
                    }
                }
            }
        }

        rule.popupMatches(testTag, isDisplayed())
    }

    @Test
    fun hasActualSize() {
        val popupWidthDp = with(rule.density) {
            popupSize.width.toDp()
        }
        val popupHeightDp = with(rule.density) {
            popupSize.height.toDp()
        }

        rule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(
                            width = popupWidthDp,
                            height = popupHeightDp,
                            content = {}
                        )
                    }
                }
            }
        }

        rule.popupMatches(testTag, matchesSize(popupSize.width, popupSize.height))
    }

    @Test
    fun changeParams_assertNoLeaks() {
        class PopupsCounterMatcher : TypeSafeMatcher<Root>() {
            var popupsFound = 0

            override fun describeTo(description: Description?) {
                description?.appendText("PopupLayoutMatcher")
            }

            // TODO(b/141101446): Find a way to match the window used by the popup
            override fun matchesSafely(item: Root?): Boolean {
                val isPopup = item != null && isPopupLayout(
                    item.decorView,
                    testTag
                )
                if (isPopup) {
                    popupsFound++
                }
                return isPopup
            }
        }

        val measureLatch = CountDownLatch(1)
        var focusable by mutableStateOf(false)
        rule.setContent {
            Box {
                PopupTestTag(testTag) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = offset,
                        properties = PopupProperties(focusable = focusable)
                    ) {
                        // This is called after the OnChildPosition method in Popup() which
                        // updates the popup to its final position
                        Box(
                            modifier = Modifier.requiredWidth(200.dp).requiredHeight(200.dp)
                                .onGloballyPositioned {
                                    measureLatch.countDown()
                                }
                        ) {}
                    }
                }
            }
        }
        measureLatch.await(1, TimeUnit.SECONDS)

        fun assertSinglePopupExists() {
            rule.runOnIdle { }
            val counterMatcher = PopupsCounterMatcher()
            Espresso.onView(instanceOf(Owner::class.java))
                .inRoot(counterMatcher)
                .check(matches(isDisplayed()))

            assertThat(counterMatcher.popupsFound).isEqualTo(1)
        }

        assertSinglePopupExists()

        rule.runOnUiThread {
            focusable = true
        }

        // If we have a leak, this will crash on multiple popups found
        assertSinglePopupExists()
    }

    @Test
    fun hasViewTreeLifecycleOwner() {
        rule.setContent {
            PopupTestTag(testTag) {
                Popup {}
            }
        }

        Espresso.onView(instanceOf(Owner::class.java))
            .inRoot(PopupLayoutMatcher(testTag))
            .check(
                matches(object : TypeSafeMatcher<View>() {
                    override fun describeTo(description: Description?) {
                        description?.appendText("ViewTreeLifecycleOwner.get(view) != null")
                    }

                    override fun matchesSafely(item: View): Boolean {
                        return ViewTreeLifecycleOwner.get(item) != null
                    }
                })
            )
    }

    @Test
    fun preservesCompositionLocals() {
        val compositionLocal = compositionLocalOf<Float> { error("unset") }
        var value = 0f
        rule.setContent {
            CompositionLocalProvider(compositionLocal provides 1f) {
                Popup {
                    value = compositionLocal.current
                }
            }
        }
        rule.runOnIdle {
            assertThat(value).isEqualTo(1f)
        }
    }

    @Test
    fun preservesLayoutDirection() {
        var value = LayoutDirection.Ltr
        rule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Popup {
                    value = LocalLayoutDirection.current
                }
            }
        }
        rule.runOnIdle {
            assertThat(value).isEqualTo(LayoutDirection.Rtl)
        }
    }

    @Test
    fun isDismissedOnTapOutside() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(alignment = Alignment.Center, onDismissRequest = { showPopup = false }) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        // Click outside the popup
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // Popup should not exist
        rule.onNodeWithTag(testTag).assertDoesNotExist()
    }

    @Test
    fun isDismissedOnBackPress() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        properties = PopupProperties(
                            // Needs to be focusable to intercept back press
                            focusable = true
                        ),
                        alignment = Alignment.Center,
                        onDismissRequest = { showPopup = false }
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        Espresso.pressBack()

        // Popup should not exist
        rule.onNodeWithTag(testTag).assertDoesNotExist()
    }

    @Test
    fun isNotDismissedOnTapOutside_dismissOnClickOutsideFalse() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        alignment = Alignment.Center,
                        properties = PopupProperties(dismissOnClickOutside = false),
                        onDismissRequest = { showPopup = false }
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        // Click outside the popup
        val outsideX = 0
        val outsideY = with(rule.density) {
            rule.onAllNodes(isRoot()).onFirst().getUnclippedBoundsInRoot().height.roundToPx() / 2
        }
        UiDevice.getInstance(getInstrumentation()).click(outsideX, outsideY)

        // Popup should still be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @Test
    fun isNotDismissedOnBackPress_dismissOnBackPressFalse() {
        var showPopup by mutableStateOf(true)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                if (showPopup) {
                    Popup(
                        properties = PopupProperties(
                            // Needs to be focusable to intercept back press
                            focusable = true,
                            dismissOnBackPress = false
                        ),
                        alignment = Alignment.Center,
                        onDismissRequest = { showPopup = false }
                    ) {
                        Box(Modifier.size(50.dp).testTag(testTag))
                    }
                }
            }
        }

        // Popup should be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()

        Espresso.pressBack()

        // Popup should still be visible
        rule.onNodeWithTag(testTag).assertIsDisplayed()
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    fun canFillScreenWidth_dependingOnProperty() {
        var box1Width = 0
        var box2Width = 0
        rule.setContent {
            Popup {
                Box(Modifier.fillMaxSize().onSizeChanged { box1Width = it.width })
            }
            Popup(properties = PopupProperties(useDefaultMaxWidth = true)) {
                Box(Modifier.fillMaxSize().onSizeChanged { box2Width = it.width })
            }
        }
        rule.runOnIdle {
            assertThat(box1Width).isEqualTo(
                (rule.activity.resources.configuration.screenWidthDp * rule.density.density)
                    .roundToInt()
            )
            assertThat(box2Width).isLessThan(box1Width)
        }
    }

    @Test
    fun didNotMeasureTooSmallLast() {
        rule.setContent {
            PopupTestTag(testTag) {
                Popup {
                    Box(Modifier.fillMaxWidth())
                }
            }
        }

        rule.popupMatches(
            testTag,
            object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description?) {
                    description?.appendText("Did not end up in MEASURE_STATE_TOO_SMALL")
                }

                override fun matchesSafely(item: View): Boolean {
                    val popupLayout = item.parent as ViewGroup
                    return popupLayout.measuredState != MEASURED_STATE_TOO_SMALL
                }
            }
        )
    }

    @Test
    fun doesNotMeasureContentMultipleTimes() {
        var measurements = 0
        rule.setContent {
            Popup {
                Box {
                    Layout({}) { _, constraints ->
                        ++measurements
                        // We size to maxWidth to make ViewRootImpl measure multiple times.
                        layout(constraints.maxWidth, 0) {}
                    }
                }
            }
        }
        rule.runOnIdle {
            assertThat(measurements).isEqualTo(1)
        }
    }

    @Test
    fun resizesWhenContentResizes() {
        val size1 = 20
        val size2 = 30
        var size by mutableStateOf(size1)
        rule.setContent {
            PopupTestTag(testTag) {
                Popup {
                    Box(Modifier.size(with(rule.density) { size.toDp() }))
                }
            }
        }
        rule.popupMatches(testTag, matchesSize(20, 20))
        rule.runOnIdle { size = size2 }
        rule.popupMatches(testTag, matchesSize(30, 30))
    }

    private fun matchesSize(width: Int, height: Int): BoundedMatcher<View, View> {
        return object : BoundedMatcher<View, View>(View::class.java) {
            override fun matchesSafely(item: View?): Boolean {
                return item?.width == width && item.height == height
            }

            override fun describeTo(description: Description?) {
                description?.appendText("with width = $width height = $height")
            }
        }
    }
}
