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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Sampled
@Composable
fun BlurSample() {
    Box(
        Modifier.size(300.dp)
            // Blur content allowing the result to extend beyond the bounds of the original content
            .blur(
                30.dp,
                edgeTreatment = BlurredEdgeTreatment.Unbounded
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
        // Blur content within the original bounds, clipping the result to a rounded rectangle
        modifier = Modifier.blur(30.dp, BlurredEdgeTreatment(RoundedCornerShape(5.dp)))
    )
}
