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

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.globalBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewTreeLifecycleOwner
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class ComposeViewTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun composeViewComposedContent() {
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello, World!", Modifier.testTag("text"))
            }
        }
        Espresso.onView(instanceOf(ComposeView::class.java))
            .check(matches(isDisplayed()))
            .check { view, _ ->
                view as ViewGroup
                assertTrue("has children", view.childCount > 0)
            }

        rule.onNodeWithTag("text").assertTextEquals("Hello, World!")
    }

    @Test
    fun composeDifferentViewContent() {
        val id = View.generateViewId()
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also { it.id = id }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.onActivity { activity ->
            val composeView: ComposeView = activity.findViewById(id)
            composeView.setContent {
                BasicText("World", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("World")

        rule.activityRule.scenario.onActivity { activity ->
            val composeView: ComposeView = activity.findViewById(id)
            composeView.disposeComposition()
        }

        rule.onNodeWithTag("text").assertDoesNotExist()
    }

    @Test
    fun disposeOnLifecycleDestroyed() {
        val lco = rule.runOnUiThread {
            TestLifecycleOwner().apply {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        }
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also {
                ViewTreeLifecycleOwner.set(it, lco)
                composeViewCapture = it
            }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.onActivity {
            lco.registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        assertNotNull("composeViewCapture", composeViewCapture)
        assertTrue("ComposeView.isDisposed", composeViewCapture?.isDisposed == true)
    }

    @Test
    fun paddingsAreNotIgnored() {
        var globalBounds = Rect.Zero
        val latch = CountDownLatch(1)
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            composeView.setPadding(10, 20, 30, 40)
            activity.setContentView(composeView, ViewGroup.LayoutParams(100, 100))
            composeView.setContent {
                Box(
                    Modifier.testTag("box").fillMaxSize().onGloballyPositioned {
                        val position = IntArray(2)
                        composeView.getLocationOnScreen(position)
                        globalBounds = it.globalBounds.translate(
                            -position[0].toFloat(), -position[1].toFloat()
                        )
                        latch.countDown()
                    }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(Rect(10f, 20f, 70f, 60f), globalBounds)
    }

    @Test
    fun viewSizeIsChildSizePlusPaddings() {
        var size = IntSize.Zero
        val latch = CountDownLatch(1)
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            composeView.setPadding(10, 20, 30, 40)
            activity.setContentView(composeView, ViewGroup.LayoutParams(100, 100))
            composeView.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        composeView.viewTreeObserver.removeOnPreDrawListener(this)
                        size = IntSize(composeView.measuredWidth, composeView.measuredHeight)
                        latch.countDown()
                        return true
                    }
                }
            )
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(IntSize(100, 100), size)
    }

    @Test
    @SmallTest
    fun throwsOnAddView() {
        rule.activityRule.scenario.onActivity { activity ->
            with(TestComposeView(activity)) {
                assertUnsupported("addView(View)") {
                    addView(View(activity))
                }

                assertUnsupported("addView(View, int)") {
                    addView(View(activity), 0)
                }

                assertUnsupported("addView(View, int, int)") {
                    addView(View(activity), 0, 0)
                }

                assertUnsupported("addView(View, LayoutParams)") {
                    addView(View(activity), ViewGroup.LayoutParams(0, 0))
                }

                assertUnsupported("addView(View, int, LayoutParams)") {
                    addView(View(activity), 0, ViewGroup.LayoutParams(0, 0))
                }

                assertUnsupported("addViewInLayout(View, int, LayoutParams)") {
                    addViewInLayout(View(activity), 0, ViewGroup.LayoutParams(0, 0))
                }

                assertUnsupported("addViewInLayout(View, int, LayoutParams, boolean)") {
                    addViewInLayout(View(activity), 0, ViewGroup.LayoutParams(0, 0), false)
                }
            }
        }
    }
}

private inline fun ViewGroup.assertUnsupported(
    testName: String,
    test: ViewGroup.() -> Unit
) {
    var exception: Throwable? = null
    try {
        test()
    } catch (t: Throwable) {
        exception = t
    }
    assertTrue(
        "$testName throws UnsupportedOperationException",
        exception is UnsupportedOperationException
    )
}

private class TestLifecycleOwner : LifecycleOwner {
    val registry = LifecycleRegistry(this)

    override fun getLifecycle(): Lifecycle = registry
}

private class TestComposeView(
    context: Context
) : AbstractComposeView(context) {

    @Composable
    override fun Content() {
        // No content
    }

    public override fun addViewInLayout(child: View?, index: Int, params: LayoutParams?): Boolean {
        return super.addViewInLayout(child, index, params)
    }

    public override fun addViewInLayout(
        child: View?,
        index: Int,
        params: LayoutParams?,
        preventRequestLayout: Boolean
    ): Boolean {
        return super.addViewInLayout(child, index, params, preventRequestLayout)
    }
}