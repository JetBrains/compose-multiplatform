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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.ui.unit.dp

class ComponentWithRedrawTestCase : SimpleComponentImplenentationTestCase() {

    @Composable
    override fun Content() {
        val innerSize = getInnerSize()
        val stroke = Stroke()
        Canvas(Modifier.preferredSize(48.dp)) {
            drawCircle(Color.Black, size.minDimension, style = stroke)
            drawCircle(Color.Black, innerSize.value.value / 2f, center)
        }
    }
}
