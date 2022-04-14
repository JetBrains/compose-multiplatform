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

package androidx.compose.ui.focus

import android.graphics.Rect as AndroidRect
import android.view.View
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class FocusViewInteropTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun getFocusedRect_reportsFocusBounds_whenFocused() {
        val focusRequester = FocusRequester()
        var hasFocus = false
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier
                        .size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .onFocusChanged {
                            if (it.isFocused) {
                                hasFocus = true
                            }
                        }
                        .focusRequester(focusRequester)
                        .focusable()
                )
            }
        }
        rule.runOnIdle {
            focusRequester.requestFocus()
        }

        rule.waitUntil { hasFocus }

        assertThat(view.getFocusedRect()).isEqualTo(IntRect(30, 40, 40, 60))
    }

    @Test
    fun getFocusedRect_reportsEntireView_whenNoFocus() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            CompositionLocalProvider(LocalDensity provides Density(density = 1f)) {
                Box(
                    Modifier
                        .size(90.dp, 100.dp)
                        .wrapContentSize(align = Alignment.TopStart)
                        .size(10.dp, 20.dp)
                        .offset(30.dp, 40.dp)
                        .focusable()
                )
            }
        }

        assertThat(view.getFocusedRect()).isEqualTo(
            IntRect(0, 0, 90, 100)
        )
    }

    private fun View.getFocusedRect() = AndroidRect().run {
        rule.runOnIdle {
            getFocusedRect(this)
        }
        IntRect(left, top, right, bottom)
    }
}