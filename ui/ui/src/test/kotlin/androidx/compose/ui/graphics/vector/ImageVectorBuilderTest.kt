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
class ImageVectorBuilderTest {

    @Test
    fun dslAndBuilderAreEqual() {
        val builderFunctionVector = builder().apply {
            addGroup(name = "Vector", pivotX = 0.2f, pivotY = 0.4f)
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
            clearGroup()
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

    @Test
    fun testAddGroup() {
        val imageVector = builder().apply {
            addGroup("group1")
            addPath(name = "path1", pathData = emptyList())
            addGroup("group2")
            addPath(name = "path2", pathData = emptyList())
            clearGroup()
            addGroup("group3")
            addPath(name = "path3", pathData = emptyList())
            addPath(name = "path4", pathData = emptyList())
            clearGroup()
            clearGroup()
            addGroup(name = "group4")
            addPath(name = "path5", pathData = emptyList())
            // intentionally avoid popping group as build will pop all groups to the root
        }.build()

        val root = imageVector.root
        Truth.assertThat(root.size).isEqualTo(2)

        val group1 = root[0] as VectorGroup
        Truth.assertThat(group1.name).isEqualTo("group1")

        Truth.assertThat(group1.size).isEqualTo(3)

        val path1 = group1[0] as VectorPath
        Truth.assertThat(path1.name).isEqualTo("path1")

        val group2 = group1[1] as VectorGroup
        Truth.assertThat(group2.name).isEqualTo("group2")
        Truth.assertThat(group2.size).isEqualTo(1)

        val path2 = group2[0] as VectorPath
        Truth.assertThat(path2.name).isEqualTo("path2")

        val group3 = group1[2] as VectorGroup
        Truth.assertThat(group3.name).isEqualTo("group3")
        Truth.assertThat(group3.size).isEqualTo(2)

        val path3 = group3[0] as VectorPath
        Truth.assertThat(path3.name).isEqualTo("path3")

        val path4 = group3[1] as VectorPath
        Truth.assertThat(path4.name).isEqualTo("path4")

        val group4 = root[1] as VectorGroup
        Truth.assertThat(group4.name).isEqualTo("group4")
        Truth.assertThat(group4.size).isEqualTo(1)

        val path5 = group4[0] as VectorPath
        Truth.assertThat(path5.name).isEqualTo("path5")
    }
}

private fun builder() = ImageVector.Builder(
    defaultWidth = 10.dp,
    defaultHeight = 10.dp,
    viewportWidth = 10f,
    viewportHeight = 10f
)