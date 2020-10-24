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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Providers
import androidx.compose.runtime.ambientOf
import androidx.compose.runtime.emptyContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Owner
import androidx.compose.ui.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.filters.MediumTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class PopupTest {

    @get:Rule
    val rule = createComposeRule()

    private val testTag = "testedPopup"
    private val offset = IntOffset(10, 10)
    private val popupSize = IntSize(40, 20)

    @Test
    fun isShowing() {
        rule.setContent {
            SimpleContainer {
                PopupTestTag(testTag) {
                    Popup(alignment = Alignment.Center) {
                        SimpleContainer(Modifier.preferredSize(50.dp), children = emptyContent())
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
                            children = emptyContent()
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
        var isFocusable by mutableStateOf(false)
        rule.setContent {
            Box {
                PopupTestTag(testTag) {
                    Popup(
                        alignment = Alignment.TopStart,
                        offset = offset,
                        isFocusable = isFocusable
                    ) {
                        // This is called after the OnChildPosition method in Popup() which
                        // updates the popup to its final position
                        Box(
                            modifier = Modifier.width(200.dp).height(200.dp)
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
            isFocusable = true
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
    fun preservesAmbients() {
        val ambient = ambientOf<Float>()
        var value = 0f
        rule.setContent {
            Providers(ambient provides 1f) {
                Popup {
                    value = ambient.current
                }
            }
        }
        rule.runOnIdle {
            assertThat(value).isEqualTo(1f)
        }
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
