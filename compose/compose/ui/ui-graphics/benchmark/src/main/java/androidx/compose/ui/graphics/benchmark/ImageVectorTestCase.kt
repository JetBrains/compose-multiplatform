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

package androidx.compose.ui.graphics.benchmark

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.paint
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.PathData
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.size
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

/**
 * Generic test case for drawing a [ImageVector].
 *
 * Subclasses are responsible for providing the vector asset, so we can test and benchmark different
 * methods of loading / creating this asset.
 */
sealed class ImageVectorTestCase : LayeredComposeTestCase() {

    @Composable
    override fun MeasuredContent() {
        Box(
            Modifier.testTag(testTag)
                .size(24.dp)
                .paint(getPainter())
        )
    }

    @Composable
    override fun ContentWrappers(content: @Composable () -> Unit) {
        Box {
            content()
        }
    }

    @Composable
    abstract fun getPainter(): Painter

    abstract val testTag: String
}

/**
 * Test case that loads and parses a vector asset from an XML file.
 */
class XmlVectorTestCase : ImageVectorTestCase() {
    // TODO: should switch to async loading here, and force that to be run synchronously
    @Composable
    override fun getPainter() = painterResource(
        androidx.compose.ui.graphics.benchmark.R.drawable.ic_baseline_menu_24
    )

    override val testTag = "Xml"
}

/**
 * Test case that creates a vector asset purely from code.
 */
class ProgrammaticVectorTestCase : ImageVectorTestCase() {

    /**
     * Returns a clone of ic_baseline_menu_24 built purely in code
     */
    @Composable
    override fun getPainter() = rememberVectorPainter(
        ImageVector.Builder(
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            addPath(
                PathData {
                    moveTo(3f, 18f)
                    horizontalLineToRelative(18f)
                    verticalLineToRelative(-2f)
                    lineTo(3f, 16f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(3f, 13f)
                    horizontalLineToRelative(18f)
                    verticalLineToRelative(-2f)
                    lineTo(3f, 11f)
                    verticalLineToRelative(2f)
                    close()
                    moveTo(3f, 6f)
                    verticalLineToRelative(2f)
                    horizontalLineToRelative(18f)
                    lineTo(21f, 6f)
                    lineTo(3f, 6f)
                    close()
                },
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f
            )
        }.build()
    )

    override val testTag = "Vector"
}
