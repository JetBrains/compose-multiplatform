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

package androidx.compose.ui.graphics.vector

import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VectorAssetBuilderTest {

    @Test
    fun dslAndBuilderAreEqual() {
        val builderFunctionVector = builder().apply {
            pushGroup(name = "Vector", pivotX = 0.2f, pivotY = 0.4f)
            addPath(
                listOf(PathNode.LineTo(10f, 10f), PathNode.Close)
            )
            addPath(
                listOf(
                    PathNode.HorizontalTo(20f),
                    PathNode.RelativeReflectiveCurveTo(40f, 40f, 10f, 10f),
                    PathNode.Close
                )
            )
            popGroup()
        }.build()

        val dslFunctionVector = builder().apply {
            group(name = "Vector", pivotX = 0.2f, pivotY = 0.4f) {
                path {
                    lineTo(10f, 10f)
                    close()
                }
                path {
                    horizontalLineTo(20f)
                    reflectiveCurveToRelative(40f, 40f, 10f, 10f)
                    close()
                }
            }
        }.build()

        Truth.assertThat(dslFunctionVector).isEqualTo(builderFunctionVector)
    }
}

private fun builder() = VectorAssetBuilder(
    defaultWidth = 10.dp,
    defaultHeight = 10.dp,
    viewportWidth = 10f,
    viewportHeight = 10f
)