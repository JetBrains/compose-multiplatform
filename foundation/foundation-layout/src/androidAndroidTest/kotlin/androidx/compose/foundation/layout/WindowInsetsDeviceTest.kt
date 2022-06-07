/*
 * Copyright (C) 2022 The Android Open Source Project
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

import android.os.Build
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollDispatcher
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.children
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInsetsDeviceTest {
    @get:Rule
    val rule = createAndroidComposeRule<WindowInsetsActivity>()

    @Before
    fun setup() {
        rule.activity.createdLatch.await(1, TimeUnit.SECONDS)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    fun disableConsumeDisablesAnimationConsumption() {
        var imeInset1 = 0
        var imeInset2 = 0

        val connection = object : NestedScrollConnection { }
        val dispatcher = NestedScrollDispatcher()

        // broken out for line length
        val innerComposable: @Composable () -> Unit = {
            imeInset2 = WindowInsets.ime.getBottom(LocalDensity.current)
            Box(
                Modifier.fillMaxSize().imePadding().imeNestedScroll()
                    .nestedScroll(connection, dispatcher).background(
                        Color.Cyan
                    )
            )
        }

        rule.setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { outerContext ->
                    ComposeView(outerContext).apply {
                        consumeWindowInsets = false
                        setContent {
                            imeInset1 = WindowInsets.ime.getBottom(LocalDensity.current)
                            Box(Modifier.fillMaxSize()) {
                                AndroidView(
                                    modifier = Modifier.fillMaxSize(),
                                    factory = { context ->
                                        ComposeView(context).apply {
                                            consumeWindowInsets = false
                                            setContent(innerComposable)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            )
        }

        rule.waitForIdle()

        // We don't have any way to know when the animation controller is applied, so just
        // loop until the value changes.

        val endTime = SystemClock.uptimeMillis() + 1000L
        var iteration = 0
        while (imeInset1 == 0 && SystemClock.uptimeMillis() < endTime) {
            rule.runOnIdle {
                if (iteration % 5 == 0) {
                    // Cuttlefish doesn't consistently show the IME when requested, so
                    // we must poke it. This will poke it every 5 iterations to ensure that
                    // if we miss it once then it will get it on another pass.
                    pokeIME()
                }
                dispatcher.dispatchPostScroll(
                    Offset.Zero,
                    Offset(0f, -10f),
                    NestedScrollSource.Drag
                )
                Snapshot.sendApplyNotifications()
            }
            Thread.sleep(50)
            iteration++
        }

        rule.runOnIdle {
            assertThat(imeInset1).isGreaterThan(0)
            assertThat(imeInset2).isEqualTo(imeInset1)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun pokeIME() {
        // This appears to be necessary for cuttlefish devices to show the keyboard
        val controller = rule.activity.window.insetsController
        controller?.show(android.view.WindowInsets.Type.ime())
        controller?.hide(android.view.WindowInsets.Type.ime())
    }

    @Test
    fun insetsUsedAfterInitialComposition() {
        var useInsets by mutableStateOf(false)
        var systemBarsInsets by mutableStateOf(Insets.NONE)

        rule.setContent {
            val view = LocalView.current
            DisposableEffect(Unit) {
                // Ensure that the system bars are shown
                val window = rule.activity.window

                @Suppress("RedundantNullableReturnType") // nullable on some versions
                val controller: WindowInsetsControllerCompat? =
                    WindowCompat.getInsetsController(window, view)
                controller?.show(WindowInsetsCompat.Type.systemBars())
                onDispose { }
            }
            Box(Modifier.fillMaxSize()) {
                if (useInsets) {
                    val systemBars = WindowInsets.systemBars
                    val density = LocalDensity.current
                    val left = systemBars.getLeft(density, LayoutDirection.Ltr)
                    val top = systemBars.getTop(density)
                    val right = systemBars.getRight(density, LayoutDirection.Ltr)
                    val bottom = systemBars.getBottom(density)
                    systemBarsInsets = Insets.of(left, top, right, bottom)
                }
            }
        }

        rule.runOnIdle {
            useInsets = true
        }

        rule.runOnIdle {
            assertThat(systemBarsInsets).isNotEqualTo(Insets.NONE)
        }
    }

    @Test
    fun insetsAfterStopWatching() {
        var useInsets by mutableStateOf(true)
        var hasStatusBarInsets = false

        rule.setContent {
            val view = LocalView.current
            DisposableEffect(Unit) {
                // Ensure that the status bars are shown
                val window = rule.activity.window

                @Suppress("RedundantNullableReturnType") // nullable on some versions
                val controller: WindowInsetsControllerCompat? =
                    WindowCompat.getInsetsController(window, view)
                controller?.hide(WindowInsetsCompat.Type.statusBars())
                onDispose { }
            }
            Box(Modifier.fillMaxSize()) {
                if (useInsets) {
                    val statusBars = WindowInsets.statusBars
                    val density = LocalDensity.current
                    val left = statusBars.getLeft(density, LayoutDirection.Ltr)
                    val top = statusBars.getTop(density)
                    val right = statusBars.getRight(density, LayoutDirection.Ltr)
                    val bottom = statusBars.getBottom(density)
                    hasStatusBarInsets = left != 0 || top != 0 || right != 0 || bottom != 0
                }
            }
        }

        rule.waitForIdle()

        rule.waitUntil(1000) { !hasStatusBarInsets }

        // disable watching the insets
        rule.runOnIdle {
            useInsets = false
        }

        val statusBarsWatcher = StatusBarsShowListener()

        // show the insets while we're not watching
        rule.runOnIdle {
            ViewCompat.setOnApplyWindowInsetsListener(
                rule.activity.window.decorView,
                statusBarsWatcher
            )
            @Suppress("RedundantNullableReturnType")
            val controller: WindowInsetsControllerCompat? = WindowCompat.getInsetsController(
                rule.activity.window,
                rule.activity.window.decorView
            )
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }

        assertThat(statusBarsWatcher.latch.await(1, TimeUnit.SECONDS)).isTrue()

        // Now look at the insets
        rule.runOnIdle {
            useInsets = true
        }

        rule.runOnIdle {
            assertThat(hasStatusBarInsets).isTrue()
        }
    }

    @Test
    fun insetsAfterReattachingView() {
        var hasStatusBarInsets = false

        // hide the insets
        rule.runOnUiThread {
            @Suppress("RedundantNullableReturnType")
            val controller: WindowInsetsControllerCompat? = WindowCompat.getInsetsController(
                rule.activity.window,
                rule.activity.window.decorView
            )
            controller?.hide(WindowInsetsCompat.Type.statusBars())
        }

        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                val statusBars = WindowInsets.statusBars
                val density = LocalDensity.current
                val left = statusBars.getLeft(density, LayoutDirection.Ltr)
                val top = statusBars.getTop(density)
                val right = statusBars.getRight(density, LayoutDirection.Ltr)
                val bottom = statusBars.getBottom(density)
                hasStatusBarInsets = left != 0 || top != 0 || right != 0 || bottom != 0
            }
        }

        rule.waitForIdle()

        rule.waitUntil(1000) { !hasStatusBarInsets }

        val contentView = rule.activity.findViewById<ViewGroup>(android.R.id.content)
        val composeView = contentView.children.first()

        // remove the view
        rule.runOnUiThread {
            contentView.removeView(composeView)
        }

        val statusBarsWatcher = StatusBarsShowListener()

        // show the insets while we're not watching
        rule.runOnUiThread {
            ViewCompat.setOnApplyWindowInsetsListener(
                rule.activity.window.decorView,
                statusBarsWatcher
            )
            @Suppress("RedundantNullableReturnType")
            val controller: WindowInsetsControllerCompat? = WindowCompat.getInsetsController(
                rule.activity.window,
                rule.activity.window.decorView
            )
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }

        assertThat(statusBarsWatcher.latch.await(1, TimeUnit.SECONDS)).isTrue()

        // Now add the view back again
        rule.runOnUiThread {
            contentView.addView(composeView)
        }

        rule.waitUntil(1000) { hasStatusBarInsets }
    }

    /**
     * If we have setDecorFitsSystemWindows(false), there should be insets.
     */
    @Test
    fun insetsSetAtStart() {
        var leftInset = 0
        var topInset = 0
        var rightInset = 0
        var bottomInset = 0

        rule.setContent {
            val insets = WindowInsets.safeContent
            leftInset = insets.getLeft(LocalDensity.current, LocalLayoutDirection.current)
            topInset = insets.getTop(LocalDensity.current)
            rightInset = insets.getRight(LocalDensity.current, LocalLayoutDirection.current)
            bottomInset = insets.getBottom(LocalDensity.current)
        }

        rule.waitForIdle()
        assertTrue(
            leftInset != 0 || topInset != 0 || rightInset != 0 || bottomInset != 0
        )
    }

    class StatusBarsShowListener : OnApplyWindowInsetsListener {
        val latch = CountDownLatch(1)

        override fun onApplyWindowInsets(v: View, insets: WindowInsetsCompat): WindowInsetsCompat {
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            if (statusBars != Insets.NONE) {
                latch.countDown()
                ViewCompat.setOnApplyWindowInsetsListener(v, null)
            }
            return insets
        }
    }
}
