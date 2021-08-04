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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun BlurSample() {
    Box(
        Modifier.size(300.dp)
            // Disabling clipping here to ensure the blurred edges are not
            // omitted.
            // Using TileMode.Decal to ensure the region outside of the blur radius is
            // blurred with transparent black instead of replicating the edge pixels
            .blur(
                30.dp,
                clip = false,
                edgeTreatment = TileMode.Decal
            )
            .background(Color.Red, CircleShape)
    )
}

@Sampled
@Composable
fun ImageBlurSample() {
    Image(
        painter = painterResource(R.drawable.circus),
        contentDescription = "sample blurred image",
        // Leaving default clipping behavior to ensure the blurred result only renders
        // within the original bounds of the composable
        modifier = Modifier.blur(30.dp, shape = RoundedCornerShape(5.dp))
    )
}
