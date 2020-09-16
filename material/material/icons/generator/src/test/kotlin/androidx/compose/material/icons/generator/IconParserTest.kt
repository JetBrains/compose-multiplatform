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

package androidx.compose.material.icons.generator

import androidx.compose.material.icons.generator.vector.FillType
import androidx.compose.material.icons.generator.vector.PathNode
import androidx.compose.material.icons.generator.vector.VectorNode
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Test for [IconParser].
 */
@RunWith(JUnit4::class)
class IconParserTest {

    @Test
    fun parseVector() {
        val icon = Icon("SimpleVector", "simple_vector", IconTheme.Filled, TestVector)
        val vector = IconParser(icon).parse()

        val nodes = vector.nodes
        Truth.assertThat(nodes.size).isEqualTo(2)

        val firstPath = nodes[0] as VectorNode.Path
        Truth.assertThat(firstPath.fillAlpha).isEqualTo(0.3f)
        Truth.assertThat(firstPath.strokeAlpha).isEqualTo(1f)
        Truth.assertThat(firstPath.fillType).isEqualTo(FillType.NonZero)

        val expectedFirstPathNodes = listOf(
            PathNode.MoveTo(20f, 10f),
            PathNode.RelativeLineTo(10f, 10f),
            PathNode.RelativeLineTo(0f, 10f),
            PathNode.RelativeLineTo(-10f, 0f),
            PathNode.Close
        )
        Truth.assertThat(firstPath.nodes).isEqualTo(expectedFirstPathNodes)

        val secondPath = nodes[1] as VectorNode.Path
        Truth.assertThat(secondPath.fillAlpha).isEqualTo(1f)
        Truth.assertThat(secondPath.strokeAlpha).isEqualTo(0.9f)
        Truth.assertThat(secondPath.fillType).isEqualTo(FillType.EvenOdd)

        val expectedSecondPathNodes = listOf(
            PathNode.MoveTo(16.5f, 9.0f),
            PathNode.RelativeHorizontalTo(3.5f),
            PathNode.RelativeVerticalTo(9f),
            PathNode.RelativeHorizontalTo(-3.5f),
            PathNode.Close
        )
        Truth.assertThat(secondPath.nodes).isEqualTo(expectedSecondPathNodes)
    }
}

private val TestVector = """
    <vector xmlns:android="http://schemas.android.com/apk/res/android"
        android:width="24dp"
        android:height="24dp">
        <path
            android:fillAlpha=".3"
            android:pathData="M20,10, l10,10 0,10 -10, 0z" />
        <path
            android:strokeAlpha=".9"
            android:pathData="M16.5,9h3.5v9h-3.5z"
            android:fillType="evenOdd" />
    </vector>
""".trimIndent()
