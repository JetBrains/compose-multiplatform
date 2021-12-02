/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.monotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CompletableDeferred
import org.junit.Test

@OptIn(ExperimentalComposeUiApi::class)
class ApplicationTest {
    @Test
    fun `run application`() = runApplicationTest {
        var isInit = false
        var isDisposed = false

        val appJob = launchApplication {
            DisposableEffect(Unit) {
                isInit = true
                onDispose {
                    isDisposed = true
                }
            }
        }

        appJob.join()

        assertThat(isInit).isTrue()
        assertThat(isDisposed).isTrue()
    }

    @Test
    fun `run application with launched effect`() = runApplicationTest {
        val onEffectLaunch = CompletableDeferred<Unit>()
        val shouldEnd = CompletableDeferred<Unit>()

        launchApplication {
            LaunchedEffect(Unit) {
                onEffectLaunch.complete(Unit)
                shouldEnd.await()
            }
        }

        onEffectLaunch.await()
        shouldEnd.complete(Unit)
    }

    @Test
    fun `run two applications`() = runApplicationTest {
        var window1: ComposeWindow? = null
        var window2: ComposeWindow? = null

        var isOpen1 by mutableStateOf(true)
        var isOpen2 by mutableStateOf(true)

        launchApplication {
            if (isOpen1) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(600.dp, 600.dp),
                    )
                ) {
                    window1 = this.window
                    Box(Modifier.size(32.dp).background(Color.Red))
                }
            }
        }

        launchApplication {
            if (isOpen2) {
                Window(
                    onCloseRequest = {},
                    state = rememberWindowState(
                        size = DpSize(300.dp, 300.dp),
                    )
                ) {
                    window2 = this.window
                    Box(Modifier.size(32.dp).background(Color.Blue))
                }
            }
        }

        awaitIdle()
        assertThat(window1?.isShowing).isTrue()
        assertThat(window2?.isShowing).isTrue()

        isOpen1 = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isTrue()

        isOpen2 = false
        awaitIdle()
        assertThat(window1?.isShowing).isFalse()
        assertThat(window2?.isShowing).isFalse()
    }

    @OptIn(ExperimentalComposeApi::class)
    @Test
    fun `window shouldn't use MonotonicFrameClock from application context`() = runApplicationTest {
        lateinit var appClock: MonotonicFrameClock
        lateinit var windowClock: MonotonicFrameClock

        launchApplication {
            LaunchedEffect(Unit) {
                appClock = coroutineContext.monotonicFrameClock
            }

            Window(
                onCloseRequest = {}
            ) {
                LaunchedEffect(Unit) {
                    windowClock = coroutineContext.monotonicFrameClock
                }
            }
        }

        awaitIdle()
        assertThat(windowClock).isNotEqualTo(appClock)

        exitApplication()
    }
}