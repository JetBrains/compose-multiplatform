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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.canScroll
import androidx.compose.ui.input.consumeScrollContainerInfo
import androidx.compose.ui.input.pointer.pointerInput
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.withTimeout

@Sampled
@Composable
fun ScrollableContainerSample() {
    var isParentScrollable by remember { mutableStateOf({ false }) }

    Column(Modifier.verticalScroll(rememberScrollState())) {

        Box(modifier = Modifier.consumeScrollContainerInfo {
            isParentScrollable = { it?.canScroll() == true }
        }) {
            Box(Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        // If there is an ancestor that handles drag events, this press might
                        // become a drag so delay any work
                        val doWork = !isParentScrollable() || try {
                            withTimeout(100) { tryAwaitRelease() }
                        } catch (e: TimeoutException) {
                            true
                        }
                        if (doWork) println("Do work")
                    })
            })
        }
    }
}
