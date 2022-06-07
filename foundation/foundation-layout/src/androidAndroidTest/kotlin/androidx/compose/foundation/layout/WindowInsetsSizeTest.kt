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

import android.graphics.Rect as AndroidRect
import android.view.WindowInsets as AndroidWindowInsets
import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.Insets as AndroidXInsets
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class WindowInsetsSizeTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var insetsView: InsetsView

    @Before
    fun setup() {
        WindowInsetsHolder.setUseTestInsets(true)
    }

    @After
    fun teardown() {
        WindowInsetsHolder.setUseTestInsets(false)
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun insetsSideWidthConsumption() {
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            AndroidView(factory = { context ->
                val view = InsetsView(context)
                insetsView = view
                val composeView = ComposeView(rule.activity)
                view.addView(
                    composeView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                composeView.setContent {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Box(Modifier
                            .wrapContentSize()
                            .onGloballyPositioned { coordinates = it }
                            .consumedWindowInsets(WindowInsets(left = 10))
                        ) {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .windowInsetsStartWidth(WindowInsets.navigationBars)
                            )
                        }
                    }
                }
                view
            }, modifier = Modifier.fillMaxSize())
        }

        // wait for layout
        rule.waitForIdle()

        sendInsets(
            WindowInsetsCompat.Type.navigationBars(),
            androidx.core.graphics.Insets.of(25, 0, 0, 0)
        )

        rule.runOnIdle {
            val view = findComposeView()
            val height = view.height
            val expectedSize = IntSize(15, height)
            assertThat(coordinates.size).isEqualTo(expectedSize)
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Test
    fun insetsSideHeightConsumption() {
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            AndroidView(factory = { context ->
                val view = InsetsView(context)
                insetsView = view
                val composeView = ComposeView(rule.activity)
                view.addView(
                    composeView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                composeView.setContent {
                    Box(Modifier
                        .wrapContentSize()
                        .onGloballyPositioned { coordinates = it }
                        .consumedWindowInsets(WindowInsets(bottom = 10))
                    ) {
                        Box(Modifier
                            .fillMaxWidth()
                            .windowInsetsBottomHeight(WindowInsets.navigationBars)
                        )
                    }
                }
                view
            }, modifier = Modifier.fillMaxSize())
        }

        // wait for layout
        rule.waitForIdle()

        sendInsets(
            WindowInsetsCompat.Type.navigationBars(),
            androidx.core.graphics.Insets.of(0, 0, 0, 25)
        )

        rule.runOnIdle {
            val view = findComposeView()
            val width = view.width
            val expectedSize = IntSize(width, 15)
            assertThat(coordinates.size).isEqualTo(expectedSize)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsStartWidthIme() {
        testInsetsSize(
            WindowInsetsCompat.Type.ime(),
            { Modifier.windowInsetsStartWidth(WindowInsets.ime).fillMaxHeight() },
            AndroidXInsets.of(10, 0, 0, 0),
            LayoutDirection.Ltr
        ) { size -> IntSize(10, size.height) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsStartWidthImeRtl() {
        testInsetsSize(
            WindowInsetsCompat.Type.ime(),
            { Modifier.windowInsetsStartWidth(WindowInsets.ime).fillMaxHeight() },
            AndroidXInsets.of(0, 0, 10, 0),
            LayoutDirection.Rtl
        ) { size -> IntSize(10, size.height) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsEndWidthIme() {
        testInsetsSize(
            WindowInsetsCompat.Type.ime(),
            { Modifier.windowInsetsEndWidth(WindowInsets.ime).fillMaxHeight() },
            AndroidXInsets.of(0, 0, 10, 0),
            LayoutDirection.Ltr
        ) { size -> IntSize(10, size.height) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsTopHeightIme() {
        testInsetsSize(
            WindowInsetsCompat.Type.ime(),
            { Modifier.windowInsetsTopHeight(WindowInsets.ime).fillMaxWidth() },
            AndroidXInsets.of(0, 10, 0, 0),
            LayoutDirection.Ltr
        ) { size -> IntSize(size.width, 10) }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
    @Test
    fun insetsBottomHeightIme() {
        testInsetsSize(
            WindowInsetsCompat.Type.ime(),
            { Modifier.windowInsetsBottomHeight(WindowInsets.ime).fillMaxWidth() },
            AndroidXInsets.of(0, 0, 0, 10),
            LayoutDirection.Ltr
        ) { size -> IntSize(size.width, 10) }
    }

    @Test
    fun insetsStartWidthNavigationBars() {
        testInsetsSize(
            WindowInsetsCompat.Type.navigationBars(),
            { Modifier.windowInsetsStartWidth(WindowInsets.navigationBars).fillMaxHeight() },
            AndroidXInsets.of(10, 0, 0, 0),
            LayoutDirection.Ltr
        ) { size -> IntSize(10, size.height) }
    }

    @Test
    fun insetsStartWidthNavigationBarsRtl() {
        testInsetsSize(
            WindowInsetsCompat.Type.navigationBars(),
            { Modifier.windowInsetsStartWidth(WindowInsets.navigationBars).fillMaxHeight() },
            AndroidXInsets.of(0, 0, 10, 0),
            LayoutDirection.Rtl
        ) { size -> IntSize(10, size.height) }
    }

    @Test
    fun insetsEndWidthNavigationBars() {
        testInsetsSize(
            WindowInsetsCompat.Type.navigationBars(),
            { Modifier.windowInsetsEndWidth(WindowInsets.navigationBars).fillMaxHeight() },
            AndroidXInsets.of(0, 0, 10, 0),
            LayoutDirection.Ltr
        ) { size -> IntSize(10, size.height) }
    }

    @Test
    fun insetsTopHeightStatusBars() {
        testInsetsSize(
            WindowInsetsCompat.Type.statusBars(),
            { Modifier.windowInsetsTopHeight(WindowInsets.statusBars).fillMaxWidth() },
            AndroidXInsets.of(0, 10, 0, 0),
            LayoutDirection.Ltr
        ) { size -> IntSize(size.width, 10) }
    }

    @Test
    fun insetsTopHeightMixed() {
        val coordinates = setInsetContent(
            {
                val insets = WindowInsets
                Modifier.windowInsetsTopHeight(insets.navigationBars.union(insets.systemBars))
                    .fillMaxWidth()
            },
            LayoutDirection.Ltr
        )
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.navigationBars(), AndroidXInsets.of(0, 3, 0, 0))
            .setInsets(WindowInsetsCompat.Type.systemBars(), AndroidXInsets.of(0, 10, 0, 0))
            .build()

        val view = findComposeView()
        rule.runOnIdle {
            insetsView.myInsets = insets.toWindowInsets()
            view.dispatchApplyWindowInsets(insets.toWindowInsets())
        }

        rule.runOnIdle {
            assertThat(coordinates.size).isEqualTo(IntSize(view.width, 10))
        }
    }

    @Test
    fun topHeightModifiersAreEqual() {
        rule.setContent {
            val modifier1 = Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
            val modifier2 = Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
            assertThat(modifier1).isEqualTo(modifier2)
        }
    }

    @Test
    fun bottomHeightModifiersAreEqual() {
        rule.setContent {
            val modifier1 = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
            val modifier2 = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
            assertThat(modifier1).isEqualTo(modifier2)
        }
    }

    @Test
    fun startWidthModifiersAreEqual() {
        rule.setContent {
            val modifier1 = Modifier.windowInsetsStartWidth(WindowInsets.navigationBars)
            val modifier2 = Modifier.windowInsetsStartWidth(WindowInsets.navigationBars)
            assertThat(modifier1).isEqualTo(modifier2)
        }
    }

    @Test
    fun endWidthModifiersAreEqual() {
        rule.setContent {
            val modifier1 = Modifier.windowInsetsEndWidth(WindowInsets.navigationBars)
            val modifier2 = Modifier.windowInsetsEndWidth(WindowInsets.navigationBars)
            assertThat(modifier1).isEqualTo(modifier2)
        }
    }

    private fun testInsetsSize(
        type: Int,
        modifier: @Composable () -> Modifier,
        sentInsets: AndroidXInsets,
        layoutDirection: LayoutDirection,
        expected: (IntSize) -> IntSize
    ) {
        val coordinates = setInsetContent(modifier, layoutDirection)

        val insets = sendInsets(type, sentInsets)
        assertThat(insets.isConsumed)

        rule.runOnIdle {
            val view = findComposeView()
            val width = view.width
            val height = view.height
            val expectedSize = expected(IntSize(width, height))
            assertThat(coordinates.size).isEqualTo(expectedSize)
        }
    }

    private fun sendInsets(
        type: Int,
        sentInsets: AndroidXInsets = AndroidXInsets.of(10, 11, 12, 13)
    ): AndroidWindowInsets {
        val builder = WindowInsetsCompat.Builder()
            .setInsets(type, sentInsets)
        if (type == WindowInsetsCompat.Type.displayCutout()) {
            val view = findComposeView()
            val width = view.width
            val height = view.height
            val safeRect = AndroidRect(0, 0, width, height)
            val cutoutRect =
                AndroidRect(width / 2 - 5, height / 2 - 5, width / 2 + 5, height / 2 + 5)
            when {
                sentInsets.left > 0 -> {
                    safeRect.left = sentInsets.left
                    cutoutRect.left = 0
                    cutoutRect.right = safeRect.left
                }
                sentInsets.top > 0 -> {
                    safeRect.top = sentInsets.top
                    cutoutRect.top = 0
                    cutoutRect.bottom = safeRect.top
                }
                sentInsets.right > 0 -> {
                    safeRect.right = width - sentInsets.right
                    cutoutRect.right = width
                    cutoutRect.left = width - safeRect.right
                }
                sentInsets.bottom > 0 -> {
                    safeRect.bottom = sentInsets.bottom
                    cutoutRect.bottom = height
                    cutoutRect.top = height - safeRect.bottom
                }
            }
            builder.setDisplayCutout(DisplayCutoutCompat(safeRect, listOf(cutoutRect)))
        }
        val insets = WindowInsetsCompat.Builder()
            .setInsets(type, sentInsets)
            .build()
        insetsView.myInsets = insets.toWindowInsets()
        return rule.runOnIdle {
            AndroidWindowInsets(
                findComposeView().dispatchApplyWindowInsets(insets.toWindowInsets())
            )
        }
    }

    private fun setInsetContent(
        sizeModifier: @Composable () -> Modifier,
        layoutDirection: LayoutDirection
    ): LayoutCoordinates {
        lateinit var coordinates: LayoutCoordinates

        rule.setContent {
            AndroidView(factory = { context ->
                val view = InsetsView(context)
                insetsView = view
                val composeView = ComposeView(rule.activity)
                view.addView(
                    composeView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                composeView.setContent {
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                        Box(Modifier.wrapContentSize().onGloballyPositioned { coordinates = it }) {
                            Box(sizeModifier())
                        }
                    }
                }
                view
            }, modifier = Modifier.fillMaxSize())
        }

        // wait for layout
        rule.waitForIdle()
        return coordinates
    }

    private fun findComposeView(): View = insetsView.findComposeView()
}
