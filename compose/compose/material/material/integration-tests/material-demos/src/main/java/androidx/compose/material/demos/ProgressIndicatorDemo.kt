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

package androidx.compose.material.demos

import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.samples.CircularProgressIndicatorSample
import androidx.compose.material.samples.LinearProgressIndicatorSample
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProgressIndicatorDemo() {
    val state = remember { ProgressState() }

    DisposableEffect(Unit) {
        state.start()
        onDispose {
            state.stop()
        }
    }

    Column {
        val modifier = Modifier.weight(1f, true)
            .align(Alignment.CenterHorizontally)
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colors.primary)
        // Determinate indicators
        Box(modifier, contentAlignment = Alignment.Center) {
            LinearProgressIndicatorSample()
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LinearProgressIndicator(progress = state.progress)
                Spacer(Modifier.requiredHeight(30.dp))
                OutlinedButton(
                    onClick = {
                        state.progress = 0f
                    }
                ) {
                    Text("Reset")
                }
            }
        }
        Box(modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicatorSample()
        }
        Row(
            modifier,
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indeterminate indicators
            LinearProgressIndicator()
            CircularProgressIndicator()
        }
    }
}

private class ProgressState {
    var progress by mutableStateOf(0f)

    fun start() {
        handler.postDelayed(updateProgress, 500)
    }

    fun stop() {
        handler.removeCallbacks(updateProgress)
    }

    val handler = Handler(Looper.getMainLooper())
    val updateProgress: Runnable = object : Runnable {
        override fun run() {
            if (progress < 1f) {
                progress += 0.05f
            }
            handler.postDelayed(this, 500)
        }
    }
}
