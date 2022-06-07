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

import android.os.Build
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.Insets
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

@OptIn(ExperimentalLayoutApi::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class WindowInsetsIgnoringVisibilityTest {
    @get:Rule
    val rule = createAndroidComposeRule<WindowInsetsActivity>()

    private lateinit var insetsView: InsetsView

    @Before
    fun setup() {
        WindowInsetsHolder.setUseTestInsets(true)
    }

    @After
    fun teardown() {
        WindowInsetsHolder.setUseTestInsets(false)
    }

    @Test
    fun isCaptionBarVisible() {
        val insets = Insets.of(0, 10, 0, 0)
        val type = WindowInsetsCompat.Type.captionBar()
        var isVisible = false

        setContent(createInsets(type, insets, true)) {
            isVisible = WindowInsets.isCaptionBarVisible
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    @Test
    fun isImeVisible() {
        val insets = Insets.of(0, 0, 0, 10)
        val type = WindowInsetsCompat.Type.ime()
        var isVisible = false

        setContent(createInsets(type, insets, true)) {
            isVisible = WindowInsets.isImeVisible
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    @Test
    fun areStatusBarsVisible() {
        val insets = Insets.of(0, 10, 0, 0)
        val type = WindowInsetsCompat.Type.statusBars()
        var isVisible = false

        setContent(createInsets(type, insets, true)) {
            isVisible = WindowInsets.areStatusBarsVisible
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    @Test
    fun areNavigationBarsVisible() {
        val insets = Insets.of(0, 0, 0, 10)
        val type = WindowInsetsCompat.Type.navigationBars()
        var isVisible = false

        setContent(createInsets(type, insets, true)) {
            isVisible = WindowInsets.areNavigationBarsVisible
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    @Test
    fun areSystemBarsVisible() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.systemBars()
        var isVisible = false

        setContent(createInsets(type, insets, true)) {
            isVisible = WindowInsets.areSystemBarsVisible
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    @Test
    fun isTappableElementVisible() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.tappableElement()
        var isVisible = false

        setContent(createInsets(type, insets, true)) {
            isVisible = WindowInsets.isTappableElementVisible
        }

        rule.runOnIdle {
            assertThat(isVisible).isTrue()
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            assertThat(isVisible).isFalse()
        }
    }

    private fun createInsets(type: Int, insets: Insets, isVisible: Boolean): WindowInsetsCompat {
        val builder = WindowInsetsCompat.Builder()
            .setInsets(type, if (isVisible) insets else Insets.of(0, 0, 0, 0))
            .setVisible(type, isVisible)
        if (type != WindowInsetsCompat.Type.ime()) {
            builder.setInsetsIgnoringVisibility(type, insets)
        }
        return builder.build()
    }

    @Test
    fun captionBarIgnoringVisibility() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.captionBar()
        var ignoringVisibility = WindowInsets(0, 0, 0, 0)

        setContent(createInsets(type, insets, true)) {
            ignoringVisibility = WindowInsets.captionBarIgnoringVisibility
        }

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }
    }

    @Test
    fun navigationBarsIgnoringVisibility() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.navigationBars()
        var ignoringVisibility = WindowInsets(0, 0, 0, 0)

        setContent(createInsets(type, insets, true)) {
            ignoringVisibility = WindowInsets.navigationBarsIgnoringVisibility
        }

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }
    }

    @Test
    fun statusBarsIgnoringVisibility() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.statusBars()
        var ignoringVisibility = WindowInsets(0, 0, 0, 0)

        setContent(createInsets(type, insets, true)) {
            ignoringVisibility = WindowInsets.statusBarsIgnoringVisibility
        }

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }
    }

    @Test
    fun systemBarsIgnoringVisibility() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.systemBars()
        var ignoringVisibility = WindowInsets(0, 0, 0, 0)

        setContent(createInsets(type, insets, true)) {
            ignoringVisibility = WindowInsets.systemBarsIgnoringVisibility
        }

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }
    }

    @Test
    fun tappableElementIgnoringVisibility() {
        val insets = Insets.of(10, 11, 12, 13)
        val type = WindowInsetsCompat.Type.tappableElement()
        var ignoringVisibility = WindowInsets(0, 0, 0, 0)

        setContent(createInsets(type, insets, true)) {
            ignoringVisibility = WindowInsets.tappableElementIgnoringVisibility
        }

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }

        dispatchApplyWindowInsets(createInsets(type, insets, false))

        rule.runOnIdle {
            ignoringVisibility.assertSameAs(10, 11, 12, 13)
        }
    }

    fun WindowInsets.assertSameAs(left: Int, top: Int, right: Int, bottom: Int) {
        // The density doesn't matter for this check as we work only in pixels
        val dummyDensity = Density(1f)
        assertThat(getLeft(dummyDensity, LayoutDirection.Ltr)).isEqualTo(left)
        assertThat(getTop(dummyDensity)).isEqualTo(top)
        assertThat(getRight(dummyDensity, LayoutDirection.Ltr)).isEqualTo(right)
        assertThat(getBottom(dummyDensity)).isEqualTo(bottom)
    }

    private fun dispatchApplyWindowInsets(insets: WindowInsetsCompat): WindowInsetsCompat {
        return rule.runOnIdle {
            val windowInsets = insets.toWindowInsets()!!
            val view = insetsView
            insetsView.myInsets = windowInsets
            val returnedInsets = view.findComposeView().dispatchApplyWindowInsets(windowInsets)
            WindowInsetsCompat.toWindowInsetsCompat(returnedInsets, view)
        }
    }

    private fun setContent(
        initialInsets: WindowInsetsCompat? = null,
        content: @Composable () -> Unit
    ) {
        rule.setContent {
            AndroidView(factory = { context ->
                val view = InsetsView(context).also {
                    it.myInsets = initialInsets?.toWindowInsets()
                }
                insetsView = view
                val composeView = ComposeView(rule.activity)
                view.addView(
                    composeView,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                composeView.setContent(content)
                view
            }, modifier = Modifier.fillMaxSize())
        }
    }
}
