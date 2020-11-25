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

package androidx.ui.integration.test.core

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.unit.dp

class ComponentWithTwoLayoutNodesTestCase : SimpleComponentImplenentationTestCase() {
    @Composable
    override fun Content() {
        Box(
            modifier = Modifier
                .preferredSize(48.dp)
                .border(BorderStroke(1.dp, Color.Cyan), CircleShape)
                .padding(1.dp),
            contentAlignment = Alignment.Center
        ) {
            val innerSize = getInnerSize().value
            Canvas(Modifier.preferredSize(innerSize)) {
                drawOutline(
                    CircleShape.createOutline(size, this),
                    Color.Cyan
                )
            }
        }
    }
}
