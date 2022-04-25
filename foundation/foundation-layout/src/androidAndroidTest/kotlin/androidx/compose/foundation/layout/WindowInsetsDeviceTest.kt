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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.R)
class WindowInsetsDeviceTest {
    @get:Rule
    val rule = createAndroidComposeRule<WindowInsetsActivity>()

    @OptIn(ExperimentalLayoutApi::class)
    @Test
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

        // We don't have any way to know when the animation controller is applied, so just
        // loop until the value changes.

        val endTime = SystemClock.uptimeMillis() + 1000L
        while (imeInset1 == 0 && SystemClock.uptimeMillis() < endTime) {
            rule.runOnIdle {
                dispatcher.dispatchPostScroll(
                    Offset.Zero,
                    Offset(0f, -10f),
                    NestedScrollSource.Drag
                )
                Snapshot.sendApplyNotifications()
            }
            Thread.sleep(50)
        }

        rule.runOnIdle {
            assertThat(imeInset1).isGreaterThan(0)
            assertThat(imeInset2).isEqualTo(imeInset1)
        }
    }
}
