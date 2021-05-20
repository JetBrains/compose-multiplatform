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

import androidx.compose.material.icons.generator.PackageNames.MaterialIconsPackage
import androidx.compose.material.icons.generator.vector.FillType
import androidx.compose.material.icons.generator.vector.PathNode
import androidx.compose.material.icons.generator.vector.Vector
import androidx.compose.material.icons.generator.vector.VectorNode
import com.google.common.truth.Truth
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Test for [ImageVectorGenerator].
 */
@RunWith(JUnit4::class)
class ImageVectorGeneratorTest {

    @Test
    fun generateFileSpec() {
        val iconName = "TestVector"
        val theme = IconTheme.Filled

        val generator = ImageVectorGenerator(
            iconName = iconName,
            iconTheme = theme,
            vector = TestVector
        )

        val fileSpec = generator.createFileSpec()

        Truth.assertThat(fileSpec.name).isEqualTo(iconName)

        val expectedPackageName = MaterialIconsPackage.packageName + "." + theme.themePackageName

        Truth.assertThat(fileSpec.packageName).isEqualTo(expectedPackageName)

        val fileContent = StringBuilder().run {
            fileSpec.writeTo(this)
            toString()
        }

        Truth.assertThat(fileContent).isEqualTo(ExpectedFile)
    }
}

@Language("kotlin")
private val ExpectedFile = """
    package androidx.compose.material.icons.filled

    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.materialIcon
    import androidx.compose.material.icons.materialPath
    import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.graphics.vector.group

    public val Icons.Filled.TestVector: ImageVector
        get() {
            if (_testVector != null) {
                return _testVector!!
            }
            _testVector = materialIcon(name = "Filled.TestVector") {
                materialPath(fillAlpha = 0.8f) {
                    moveTo(20.0f, 10.0f)
                    lineToRelative(0.0f, 10.0f)
                    lineToRelative(-10.0f, 0.0f)
                    close()
                }
                group {
                    materialPath(pathFillType = EvenOdd) {
                        moveTo(0.0f, 10.0f)
                        lineToRelative(-10.0f, 0.0f)
                        close()
                    }
                }
            }
            return _testVector!!
        }

    private var _testVector: ImageVector? = null

""".trimIndent()

private val path1 = VectorNode.Path(
    strokeAlpha = 1f,
    fillAlpha = 0.8f,
    fillType = FillType.NonZero,
    nodes = listOf(
        PathNode.MoveTo(20f, 10f),
        PathNode.RelativeLineTo(0f, 10f),
        PathNode.RelativeLineTo(-10f, 0f),
        PathNode.Close
    )
)

private val path2 = VectorNode.Path(
    strokeAlpha = 1f,
    fillAlpha = 1f,
    fillType = FillType.EvenOdd,
    nodes = listOf(
        PathNode.MoveTo(0f, 10f),
        PathNode.RelativeLineTo(-10f, 0f),
        PathNode.Close
    )
)

private val group = VectorNode.Group(mutableListOf(path2))

private val TestVector = Vector(listOf(path1, group))
