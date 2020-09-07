/*
 * Copyright 2020 The Android Open Source Project
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
package example.imageviewer.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.onDispose
import androidx.compose.ui.gesture.RawScaleObserver
import androidx.compose.ui.gesture.doubleTapGestureFilter
import androidx.compose.ui.gesture.rawScaleGestureFilter
import androidx.compose.ui.Modifier
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.Interaction
import androidx.compose.foundation.InteractionState
import androidx.compose.material.Surface
import example.imageviewer.style.Transparent
import androidx.compose.runtime.mutableStateOf

@Composable
fun Scalable(
    onScale: ScaleHandler,
    modifier: Modifier = Modifier,
    children: @Composable() () -> Unit
) {
    Surface(
        color = Transparent,
        modifier = modifier.rawScaleGestureFilter(
            scaleObserver = onScale
        ).doubleTapGestureFilter(onDoubleTap = { onScale.resetFactor() }),
    ) {
        children()
    }
}

class ScaleHandler(private val maxFactor: Float = 5f, private val minFactor: Float = 1f) :
    RawScaleObserver {
    val factor = mutableStateOf(1f)

    fun resetFactor() {
        if (factor.value > minFactor)
            factor.value = minFactor
    }

    override fun onScale(scaleFactor: Float): Float {
        factor.value += scaleFactor - 1f

        if (maxFactor < factor.value) factor.value = maxFactor
        if (minFactor > factor.value) factor.value = minFactor

        return scaleFactor
    }
}