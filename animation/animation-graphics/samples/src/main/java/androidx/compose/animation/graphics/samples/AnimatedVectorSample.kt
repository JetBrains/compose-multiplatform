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

package androidx.compose.animation.graphics.samples

import androidx.annotation.DrawableRes
import androidx.annotation.Sampled
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun AnimatedVectorSample() {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    @Composable
    fun AnimatedVector(@DrawableRes drawableId: Int) {
        val image = animatedVectorResource(drawableId)
        var atEnd by remember { mutableStateOf(false) }
        Image(
            painter = image.painterFor(atEnd),
            contentDescription = "Your content description",
            modifier = Modifier.size(64.dp).clickable {
                atEnd = !atEnd
            }
        )
    }
}
