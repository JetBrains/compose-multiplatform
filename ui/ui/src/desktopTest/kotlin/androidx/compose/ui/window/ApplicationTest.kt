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

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
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
}