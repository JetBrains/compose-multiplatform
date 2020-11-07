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

package androidx.compose.ui.tooling

import android.app.Activity
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.compose.animation.core.InternalAnimationApi
import androidx.compose.animation.core.TransitionAnimation
import androidx.compose.ui.tooling.preview.ComposeViewAdapter
import androidx.compose.ui.tooling.preview.ViewInfo
import androidx.compose.ui.tooling.preview.animation.PreviewAnimationClock
import androidx.compose.ui.tooling.test.R
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ComposeViewAdapterTest {
    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule<TestActivity>(
        TestActivity::class.java
    )

    private lateinit var composeViewAdapter: ComposeViewAdapter

    @Before
    fun setup() {
        composeViewAdapter =
            activityTestRule.activity.findViewById(R.id.compose_view_adapter)
    }

    /**
     * Asserts that the given Composable method executes correct and outputs some [ViewInfo]s.
     */
    private fun assertRendersCorrectly(className: String, methodName: String): List<ViewInfo> {
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(className, methodName, debugViewInfos = true)
        }

        activityTestRule.runOnUiThread {
            assertTrue(composeViewAdapter.viewInfos.isNotEmpty())
        }

        return composeViewAdapter.viewInfos
    }

    @Test
    fun instantiateComposeViewAdapter() {
        val viewInfos = assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "SimpleComposablePreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.fileName == "SimpleComposablePreview.kt" }
            .toList()

        activityTestRule.runOnUiThread {
            assertTrue(viewInfos.isNotEmpty())
            // Verify that valid line numbers are being recorded
            assertTrue(viewInfos.map { it.lineNumber }.all { it > 0 })
            // Verify that this composable has no animations
            assertFalse(composeViewAdapter.hasAnimations())
        }
    }

    @Test
    fun transitionAnimationsAreSubscribedToTheClock() {
        checkComposableAnimationIsSubscribed("CheckBoxPreview")
    }

    @Test
    fun transitionAnimationsWithSubcomposition() {
        checkComposableAnimationIsSubscribed("CheckBoxScaffoldPreview")
    }

    @OptIn(InternalAnimationApi::class)
    private fun checkComposableAnimationIsSubscribed(composableName: String) {
        val clock = PreviewAnimationClock()
        val laidOutLatch = CountDownLatch(1)

        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestAnimationPreviewKt",
                composableName
            )

            composeViewAdapter.viewTreeObserver.addOnPreDrawListener(
                object : ViewTreeObserver.OnPreDrawListener {
                    override fun onPreDraw(): Boolean {
                        laidOutLatch.countDown()
                        composeViewAdapter.viewTreeObserver.removeOnPreDrawListener(this)
                        return true
                    }
                }
            )
        }

        assertTrue(laidOutLatch.await(1, TimeUnit.SECONDS))

        activityTestRule.runOnUiThread {
            composeViewAdapter.clock = clock
            assertTrue(clock.observersToAnimations.isEmpty())

            composeViewAdapter.findAndSubscribeTransitions()
            assertTrue(composeViewAdapter.hasAnimations())

            val observer = clock.observersToAnimations.keys.single()
            val transitionAnimation =
                (observer as TransitionAnimation<*>.TransitionAnimationClockObserver).animation
            assertEquals("checkBoxAnim", transitionAnimation.label)
        }
    }

    @Test
    fun lineNumberMapping() {
        val viewInfos = assertRendersCorrectly(
            "androidx.compose.ui.tooling.LineNumberPreviewKt",
            "LineNumberPreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.fileName == "LineNumberPreview.kt" }
            .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            assertArrayEquals(
                arrayOf(36, 37, 38, 40, 43, 44, 45),
                viewInfos
                    .map { it.lineNumber }
                    .sorted()
                    .distinct()
                    .toTypedArray()
            )
        }
    }

    @Test
    fun lineNumberLocationMapping() {
        val viewInfos = assertRendersCorrectly(
            "androidx.compose.ui.tooling.LineNumberPreviewKt",
            "LineNumberPreview"
        ).flatMap { it.allChildren() + it }
            .filter { it.location?.let { it.sourceFile == "LineNumberPreview.kt" } == true }
            .toList()

        activityTestRule.runOnUiThread {
            // Verify all calls, generate the correct line number information
            val lines = viewInfos
                .map { it.location?.lineNumber ?: -1 }
                .sorted()
                .toTypedArray()
            assertArrayEquals(arrayOf(36, 37, 38, 40, 40, 40, 43, 44, 44, 45, 45), lines)

            // Verify that all calls generate the correct offset information
            val offsets = viewInfos
                .map { it.location?.offset ?: -1 }
                .sorted()
                .toTypedArray()
            assertArrayEquals(
                arrayOf(1235, 1272, 1293, 1421, 1421, 1421, 1469, 1491, 1508, 1531, 1548),
                offsets
            )
        }
    }

    @Test
    fun instantiatePrivateComposeViewAdapter() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "PrivateSimpleComposablePreview"
        )
    }

    @Test
    fun defaultParametersComposableTest1() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview1"
        )
    }

    @Test
    fun defaultParametersComposableTest2() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview2"
        )
    }

    @Test
    fun defaultParametersComposableTest3() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "DefaultParametersPreview3"
        )
    }

    @Test
    fun previewInClass() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.TestGroup",
            "InClassPreview"
        )
    }

    @Test
    fun lifecycleUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "LifecyclePreview"
        )
    }

    @Test
    fun uiSavedStateRegistryUsedInsidePreview() {
        assertRendersCorrectly(
            "androidx.compose.ui.tooling.SimpleComposablePreviewKt",
            "UiSavedStateRegistryPreview"
        )
    }

    /**
     * Check that no re-composition happens without forcing it.
     */
    @Test
    fun testNoInvalidation() {
        compositionCount.set(0)
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestInvalidationPreviewKt",
                "CounterPreview",
                forceCompositionInvalidation = false
            )
            assertEquals(1, compositionCount.get())
        }
        activityTestRule.runOnUiThread {
            assertEquals(1, compositionCount.get())
        }
        activityTestRule.runOnUiThread {
            assertEquals(1, compositionCount.get())
        }
    }

    /**
     * Check re-composition happens when forced.
     */
    @Test
    fun testInvalidation() {
        compositionCount.set(0)
        activityTestRule.runOnUiThread {
            composeViewAdapter.init(
                "androidx.compose.ui.tooling.TestInvalidationPreviewKt",
                "CounterPreview",
                forceCompositionInvalidation = true
            )
            assertEquals(1, compositionCount.get())
        }
        activityTestRule.runOnUiThread {
            assertEquals(2, compositionCount.get())
        }
        activityTestRule.runOnUiThread {
            assertEquals(3, compositionCount.get())
        }
    }

    companion object {
        class TestActivity : Activity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContentView(R.layout.compose_adapter_test)
            }
        }
    }
}
