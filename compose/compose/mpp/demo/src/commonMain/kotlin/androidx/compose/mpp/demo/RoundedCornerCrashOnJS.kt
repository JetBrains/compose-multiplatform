/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun RoundedCornerCrashOnJS() {
    // Crash happens in ShadowUtils.drawShadow(
    // Related issue https://github.com/JetBrains/compose-multiplatform/issues/3013
    Box(
        modifier = Modifier
            .size(100.dp)
            .graphicsLayer {
                shadowElevation = 5.dp.toPx()
                shape = RoundedCornerShape(0.dp, 9.dp, 9.dp, 9.dp)
            }
    ) {
    }
}
