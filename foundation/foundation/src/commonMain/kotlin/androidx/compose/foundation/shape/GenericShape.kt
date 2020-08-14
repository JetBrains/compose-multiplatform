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

package androidx.compose.foundation.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density

/**
 * Creates [Shape] defined by applying the provided [builder] on a [Path].
 *
 * @param builder the builder lambda to apply on a [Path]
 */
data class GenericShape(
    private val builder: Path.(size: Size) -> Unit
) : Shape {
    override fun createOutline(size: Size, density: Density): Outline {
        val path = Path().apply {
            builder(size)
            close()
        }
        return Outline.Generic(path)
    }
}
