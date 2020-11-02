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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.animation.animatedFloat
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.onCommit
import androidx.compose.ui.DrawLayerModifier
import androidx.compose.ui.Modifier
import androidx.compose.ui.drawLayer

@Sampled
@Composable
fun ChangeOpacity() {
    Text("Hello World", Modifier.drawLayer(alpha = 0.5f, clip = true))
}

@Sampled
@Composable
fun AnimateFadeIn() {
    val alpha = animatedFloat(initVal = 0f)
    val layerModifier = object : DrawLayerModifier {
        override val alpha: Float get() = alpha.value
        override val clip: Boolean
            get() = true
    }
    Text("Hello World", layerModifier)
    onCommit {
        alpha.animateTo(1f)
    }
}