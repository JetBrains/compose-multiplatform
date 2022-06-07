/*
 * Copyright 2022 The Android Open Source Project
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

import android.graphics.Insets
import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalLayoutApi::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class WindowInsetsControllerTest {
    @get:Rule
    val rule = createAndroidComposeRule<WindowInsetsActivity>()

    private val testTag = "TestTag"

    /**
     * The size of the inset when shown.
     */
    private var shownSize = 0

    /**
     * This is the fling velocity that will move enough so that a spring will show at least
     * 1 pixel of movement. This should be considered a small fling.
     */
    private val FlingToSpring1Pixel = 300f

    // ========================================================
    // The specific insets are extracted here so that different
    // insets can be tested locally. IME works for R+, but
    // status bars only work on S+. The following allows
    // extracting out the insets particulars so that tests
    // work with different insets types.
    // ========================================================

    /**
     * The android WindowInsets type.
     */
    private val insetType = android.view.WindowInsets.Type.statusBars()
    private val insetSide = WindowInsetsSides.Top

    private val windowInsets: AndroidWindowInsets
        @Composable
        get() = WindowInsetsHolder.current().ime

    private val WindowInsets.value: Int
        get() = getBottom(Density(1f))

    private val Insets.value: Int
        get() = bottom

    private val reverseLazyColumn = true

    private fun TouchInjectionScope.swipeAwayFromInset() {
        swipeUp()
    }

    private fun TouchInjectionScope.swipeTowardInset() {
        swipeDown()
    }

    /**
     * A motion in this direction moves away from the insets
     */
    private val directionMultiplier: Float = -1f

    private var shownAtStart = false

    @Before
    fun setup() {
        rule.activity.createdLatch.await(1, TimeUnit.SECONDS)
        rule.activity.attachedToWindowLatch.await(1, TimeUnit.SECONDS)
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            shownAtStart = view.rootWindowInsets.isVisible(insetType)
        }
    }
    @After
    fun teardown() {
        rule.runOnUiThread {
            val view = rule.activity.window.decorView
            if (shownAtStart) {
                view.windowInsetsController?.show(insetType)
            } else {
                view.windowInsetsController?.hide(insetType)
            }
        }
    }

    /**
     * Scrolling away from the inset with the inset hidden should show it.
     */
    @Test
    fun canScrollToShow() {
        if (!initializeDeviceWithInsetsHidden()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
                .onPlaced { coordinates = it }
            )
        }

        val sizeBefore = coordinates.size

        rule.runOnUiThread {
            // The first scroll triggers the animation controller to be requested
            val consumed = connection.onPostScroll(
                consumed = Offset.Zero,
                available = Offset(3f, directionMultiplier),
                source = NestedScrollSource.Drag
            )
            assertThat(consumed).isEqualTo(Offset(0f, directionMultiplier))
        }
        // We don't know when the animation controller request will be fulfilled, so loop
        // until we're sure
        val startTime = SystemClock.uptimeMillis()
        do {
            assertThat(SystemClock.uptimeMillis()).isLessThan(startTime + 1000)
            val size = rule.runOnUiThread {
                connection.onPostScroll(
                    consumed = Offset.Zero,
                    available = Offset(3f, directionMultiplier * 5f),
                    source = NestedScrollSource.Drag
                )
                coordinates.size
            }
        } while (size == sizeBefore)

        rule.runOnIdle {
            val sizeAfter = coordinates.size
            assertThat(sizeBefore.height).isGreaterThan(sizeAfter.height)
        }
    }

    /**
     * Scrolling toward the inset with the inset shown should hide it.
     */
    @Test
    fun canScrollToHide() {
        if (!initializeDeviceWithInsetsShown()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
                .onPlaced { coordinates = it }
            )
        }

        val sizeBefore = coordinates.size

        rule.runOnUiThread {
            // The first scroll triggers the animation controller to be requested
            val consumed = connection.onPreScroll(
                available = Offset(3f, -directionMultiplier),
                source = NestedScrollSource.Drag
            )
            assertThat(consumed).isEqualTo(Offset(0f, -directionMultiplier))
        }
        // We don't know when the animation controller request will be fulfilled, so loop
        // until we're sure
        val startTime = SystemClock.uptimeMillis()
        do {
            assertThat(SystemClock.uptimeMillis()).isLessThan(startTime + 1000)
            val size = rule.runOnUiThread {
                connection.onPreScroll(
                    available = Offset(3f, directionMultiplier * -5f),
                    source = NestedScrollSource.Drag
                )
                coordinates.size
            }
        } while (size == sizeBefore)

        rule.runOnIdle {
            val sizeAfter = coordinates.size
            assertThat(sizeBefore.height).isLessThan(sizeAfter.height)
        }
    }

    /**
     * Flinging away from an inset should show it.
     */
    @Test
    fun canFlingToShow() {
        if (!initializeDeviceWithInsetsHidden()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
                .onPlaced { coordinates = it }
            )
        }

        val sizeBefore = coordinates.size

        runBlockingOnUiThread {
            val consumed = connection.onPostFling(
                consumed = Velocity.Zero,
                available = Velocity(3f, directionMultiplier * 5000f)
            )
            assertThat(consumed.x).isEqualTo(0f)
            assertThat(abs(consumed.y)).isLessThan(5000f)
        }

        rule.runOnIdle {
            val sizeAfter = coordinates.size
            assertThat(sizeBefore.height).isGreaterThan(sizeAfter.height)
        }
    }

    /**
     * Flinging toward an inset should hide it.
     */
    @Test
    fun canFlingToHide() {
        if (!initializeDeviceWithInsetsShown()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
                .onPlaced { coordinates = it }
            )
        }

        val sizeBefore = coordinates.size

        runBlockingOnUiThread {
            val consumed = connection.onPreFling(
                available = Velocity(3f, -directionMultiplier * 5000f)
            )
            assertThat(consumed.x).isEqualTo(0f)
            assertThat(abs(consumed.y)).isLessThan(5000f)
        }

        rule.runOnIdle {
            val sizeAfter = coordinates.size
            assertThat(sizeBefore.height).isLessThan(sizeAfter.height)
        }
    }

    /**
     * A small fling should use an animation to bounce back to hiding the inset
     */
    @Test
    fun smallFlingSpringsBackToHide() {
        if (!initializeDeviceWithInsetsHidden()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection

        var maxVisible = 0
        var isVisible = false

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            maxVisible = maxOf(maxVisible, windowInsets.value)
            isVisible = windowInsets.isVisible
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
            )
        }

        runBlockingOnUiThread {
            connection.onPostFling(
                consumed = Velocity.Zero,
                available = Velocity(0f, directionMultiplier * FlingToSpring1Pixel)
            )
            assertThat(maxVisible).isGreaterThan(0)
        }

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    /**
     * A small fling should use an animation to bounce back to showing the inset
     */
    @Test
    fun smallFlingSpringsBackToShow() {
        if (!initializeDeviceWithInsetsShown()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection

        var minVisible = 0
        var isVisible = false

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            minVisible = minOf(minVisible, windowInsets.value)
            isVisible = windowInsets.isVisible
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
            )
        }

        runBlockingOnUiThread {
            connection.onPostFling(
                consumed = Velocity.Zero,
                available = Velocity(0f, directionMultiplier * FlingToSpring1Pixel)
            )
            assertThat(minVisible).isLessThan(shownSize)
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }
    }

    /**
     * A fling past the middle should animate to fully showing the inset
     */
    @Test
    fun flingPastMiddleSpringsToShow() {
        if (!initializeDeviceWithInsetsHidden()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection

        var isVisible = false
        var insetsSize = 0

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            isVisible = windowInsets.isVisible
            insetsSize = windowInsets.value
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
            )
        }

        // We don't know when the animation controller request will be fulfilled, so loop
        // we scroll
        val startTime = SystemClock.uptimeMillis()
        do {
            assertThat(SystemClock.uptimeMillis()).isLessThan(startTime + 1000)
            rule.runOnIdle {
                connection.onPostScroll(
                    consumed = Offset.Zero,
                    available = Offset(0f, directionMultiplier),
                    source = NestedScrollSource.Drag
                )
            }
        } while (!isVisible)

        // now scroll to just short of half way
        rule.runOnIdle {
            val sizeDifference = shownSize / 2f - 1f - insetsSize
            connection.onPostScroll(
                consumed = Offset.Zero,
                available = Offset(0f, directionMultiplier * sizeDifference),
                source = NestedScrollSource.Drag
            )
        }

        rule.waitForIdle()

        runBlockingOnUiThread {
            connection.onPostFling(
                consumed = Velocity.Zero,
                available = Velocity(0f, directionMultiplier * FlingToSpring1Pixel)
            )
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }
    }

    /**
     * A fling that moves more than half way toward hiding should animate to fully hiding the inset
     */
    @Test
    fun flingPastMiddleSpringsToHide() {
        if (!initializeDeviceWithInsetsShown()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection

        var isVisible = false
        var insetsSize = 0

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            isVisible = windowInsets.isVisible
            insetsSize = windowInsets.value
            Box(Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
            )
        }

        // We don't know when the animation controller request will be fulfilled, so loop
        // we scroll
        val startTime = SystemClock.uptimeMillis()
        do {
            assertThat(SystemClock.uptimeMillis()).isLessThan(startTime + 1000)
            rule.runOnIdle {
                connection.onPreScroll(
                    available = Offset(0f, directionMultiplier * -1f),
                    source = NestedScrollSource.Drag
                )
            }
        } while (insetsSize != shownSize)

        // now scroll to just short of half way
        rule.runOnIdle {
            val sizeDifference = shownSize / 2f + 1f - insetsSize
            connection.onPreScroll(
                available = Offset(0f, directionMultiplier * sizeDifference),
                source = NestedScrollSource.Drag
            )
        }

        runBlockingOnUiThread {
            // should fling at least one pixel past the middle
            connection.onPreFling(
                available = Velocity(0f, directionMultiplier * -FlingToSpring1Pixel)
            )
        }

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    /**
     * The insets shouldn't get in the way of normal scrolling on the normal content.
     */
    @Test
    fun allowsContentScroll() {
        if (!initializeDeviceWithInsetsHidden()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection
        lateinit var coordinates: LayoutCoordinates
        val lazyListState = LazyListState()

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            val boxSize = with(LocalDensity.current) { 100.toDp() }
            LazyColumn(
                reverseLayout = reverseLazyColumn,
                state = lazyListState,
                modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
                .testTag(testTag)
                .onPlaced { coordinates = it }
            ) {
                items(1000) {
                    Box(Modifier.size(boxSize))
                }
            }
        }

        val sizeBefore = coordinates.size

        rule.onNodeWithTag(testTag)
            .performTouchInput {
                swipeTowardInset()
            }

        rule.runOnIdle {
            assertThat(coordinates.size.height).isEqualTo(sizeBefore.height)
            // The drag should result in 1 item scrolled, but the fling should give more than 1
            assertThat(lazyListState.firstVisibleItemIndex).isGreaterThan(2)
        }

        val firstVisibleIndex = lazyListState.firstVisibleItemIndex

        rule.onNodeWithTag(testTag)
            .performTouchInput {
                swipeAwayFromInset()
            }

        rule.runOnIdle {
            assertThat(coordinates.size.height).isEqualTo(sizeBefore.height)
            // The drag should result in 1 item scrolled, but the fling should give more than 1
            assertThat(lazyListState.firstVisibleItemIndex).isLessThan(firstVisibleIndex - 2)
        }
    }

    /**
     * When flinging more than the inset, it should animate the insets closed and then fling
     * the content.
     */
    @Test
    fun flingRemainderMovesContent() {
        if (!initializeDeviceWithInsetsShown()) {
            return // The insets don't exist on this device
        }
        lateinit var connection: NestedScrollConnection
        lateinit var coordinates: LayoutCoordinates
        val lazyListState = LazyListState()

        rule.setContent {
            connection =
                rememberWindowInsetsConnection(windowInsets, insetSide)
            val boxSize = with(LocalDensity.current) { 100.toDp() }
            LazyColumn(
                reverseLayout = reverseLazyColumn,
                state = lazyListState,
                modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(windowInsets)
                .nestedScroll(connection)
                .testTag(testTag)
                .onPlaced { coordinates = it }
            ) {
                items(1000) {
                    Box(Modifier.size(boxSize))
                }
            }
        }

        val sizeBefore = coordinates.size

        rule.onNodeWithTag(testTag)
            .performTouchInput {
                swipeTowardInset()
            }

        rule.runOnIdle {
            assertThat(coordinates.size.height).isGreaterThan(sizeBefore.height)
            // The fling should get at least one item moved
            assertThat(lazyListState.firstVisibleItemIndex).isGreaterThan(0)
        }
    }

    /**
     * On some devices, the animation can begin and then end immediately without the value being
     * set to the final state in onProgress().
     */
    @Test
    fun quickAnimation() {
        val view = rule.activity.window.decorView
        val imeType = android.view.WindowInsets.Type.ime()

        rule.runOnUiThread {
            view.windowInsetsController?.show(imeType)
        }

        val imeAvailable = rule.runOnIdle {
            val windowInsets = view.rootWindowInsets
            val insets = windowInsets.getInsets(imeType)
            shownSize = insets.value
            windowInsets.isVisible(imeType) && insets.value != 0
        }
        if (!imeAvailable) {
            return // IME isn't available on this device
        }
        var imeBottom by mutableStateOf(0)
        var showDialog by mutableStateOf(false)
        val focusRequester = FocusRequester()
        rule.setContent {
            imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
            Column(Modifier.background(Color.White).wrapContentSize().imePadding()) {
                BasicTextField(
                    "Hello World",
                    { },
                    modifier = Modifier.focusRequester(focusRequester).testTag("textField")
                )
                if (showDialog) {
                    Dialog(onDismissRequest = { showDialog = false }) {
                        Box(Modifier.size(20.dp).background(Color.Red))
                    }
                }
            }
        }

        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.onNodeWithTag("textField").assertIsFocused()

        rule.runOnIdle {
            assertThat(imeBottom).isNotEqualTo(0)
        }

        showDialog = true

        rule.waitForIdle() // wait for showDialog

        // We don't know when the IME will go away, so we should keep checking for it.
        rule.waitUntil { imeBottom == 0 }
    }

    private fun initializeDeviceWithInsetsShown(): Boolean {
        val view = rule.activity.window.decorView

        rule.runOnUiThread {
            view.windowInsetsController?.show(insetType)
        }

        return rule.runOnIdle {
            val windowInsets = view.rootWindowInsets
            val insets = windowInsets.getInsets(insetType)
            shownSize = insets.value
            windowInsets.isVisible(insetType) && insets.value != 0
        }
    }

    private fun initializeDeviceWithInsetsHidden(): Boolean {
        if (!initializeDeviceWithInsetsShown()) {
            return false
        }
        val view = rule.activity.window.decorView
        rule.runOnUiThread {
            view.windowInsetsController?.hide(insetType)
        }
        return rule.runOnUiThread {
            val windowInsets = view.rootWindowInsets
            !windowInsets.isVisible(insetType)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun runBlockingOnUiThread(block: suspend CoroutineScope.() -> Unit) {
        val latch = CountDownLatch(1)
        val clock = MyTestFrameClock()
        GlobalScope.launch(Dispatchers.Main) {
            val context = coroutineContext + clock
            withContext(context, block)
            latch.countDown()
        }
        var frameTimeNanos = 0L
        while (latch.count > 0) {
            frameTimeNanos += 4_000_000L // 4ms
            clock.trySendFrame(frameTimeNanos)
            rule.waitForIdle()
        }
    }

    private class MyTestFrameClock : MonotonicFrameClock {
        private val frameCh = Channel<Long>(1)

        fun trySendFrame(frameTimeNanos: Long) {
            frameCh.trySend(frameTimeNanos)
        }

        override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
            return onFrame(frameCh.receive())
        }
    }
}
