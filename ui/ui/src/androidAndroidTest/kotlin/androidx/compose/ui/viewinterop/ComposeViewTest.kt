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
import android.os.Build
import android.view.ContextThemeWrapper
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.R
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Ignore
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
                if (Build.VERSION.SDK_INT >= 23) {
                    assertEquals(
                        "androidx.compose.ui.platform.ComposeView",
                        view.getAccessibilityClassName()
                    )
                }
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
    }

    @Test
    fun compositionStrategyDisposed() {
        rule.activityRule.scenario.onActivity { activity ->
            var installed = false
            var disposed = false
            val testView = TestComposeView(activity)
            val strategy = object : ViewCompositionStrategy {
                override fun installFor(view: AbstractComposeView): () -> Unit {
                    installed = true
                    assertSame("correct view provided", testView, view)
                    return { disposed = true }
                }
            }
            testView.setViewCompositionStrategy(strategy)
            assertTrue("strategy should be installed", installed)
            assertFalse("strategy should not be disposed", disposed)
            testView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            assertTrue("strategy should be disposed", disposed)
        }
    }

    @Test
    fun disposeOnDetachedDefaultStrategy() {
        rule.activityRule.scenario.onActivity { activity ->
            val testView = TestComposeView(activity)
            assertFalse("should not have composition yet", testView.hasComposition)
            activity.setContentView(testView)
            assertTrue("composition should be created", testView.hasComposition)
            activity.setContentView(View(activity))
            assertFalse("composition should have been disposed on detach", testView.hasComposition)
        }
    }

    @Test
    fun disposeOnLifecycleDestroyedStrategy() {
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also {
                it.setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnLifecycleDestroyed(activity)
                )
                composeViewCapture = it
            }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        assertNotNull("composeViewCapture should not be null", composeViewCapture)
        assertTrue(
            "ComposeView should not have a composition",
            composeViewCapture?.hasComposition == false
        )
    }

    @Test
    fun disposeOnViewTreeLifecycleDestroyedStrategy_setBeforeAttached() {
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity).also {
                it.setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                composeViewCapture = it
            }
            activity.setContentView(composeView)
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        assertNotNull("composeViewCapture should not be null", composeViewCapture)
        assertTrue(
            "ComposeView should not have a composition",
            composeViewCapture?.hasComposition == false
        )
    }

    @Test
    fun disposeOnViewTreeLifecycleDestroyedStrategy_setAfterAttached() {
        var composeViewCapture: ComposeView? = null
        rule.activityRule.scenario.onActivity { activity ->
            val composeView = ComposeView(activity)
            composeViewCapture = composeView

            activity.setContentView(composeView)
            composeView.setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            composeView.setContent {
                BasicText("Hello", Modifier.testTag("text"))
            }
        }

        rule.onNodeWithTag("text").assertTextEquals("Hello")

        rule.activityRule.scenario.moveToState(Lifecycle.State.DESTROYED)
        assertNotNull("composeViewCapture should not be null", composeViewCapture)
        assertTrue(
            "ComposeView should not have a composition",
            composeViewCapture?.hasComposition == false
        )
    }

    @Ignore("Disable Broken test: b/187962859")
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
                        globalBounds = it.boundsInWindow().translate(
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

    /**
     * Regression test for https://issuetracker.google.com/issues/181463117
     * Ensures that [ComposeView] can be constructed and attached a window even if View calls
     * [View.onRtlPropertiesChanged] in its constructor before subclass constructors run.
     * (AndroidComposeView is sensitive to this.)
     */
    @Test
    @SmallTest
    fun onRtlPropertiesChangedCalledByViewConstructor() {
        var result: Result<Unit>? = null
        rule.activityRule.scenario.onActivity { activity ->
            result = runCatching {
                activity.setContentView(
                    ComposeView(
                        ContextThemeWrapper(activity, R.style.Theme_WithScrollbarAttrSet)
                    ).apply {
                        setContent {}
                    }
                )
            }
        }
        assertNotNull("test did not run", result?.getOrThrow())
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