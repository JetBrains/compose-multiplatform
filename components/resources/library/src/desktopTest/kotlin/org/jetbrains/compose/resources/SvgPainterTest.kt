package org.jetbrains.compose.resources

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SvgPainterTest {

    @Test
    fun svgPainterCachesByDrawSizeWithSizeAndScaleModifiers() = clearResourceCachesAndRunUiTest {
        val imageSize = mutableStateOf(24.dp)
        val imageScale = mutableStateOf(1f)
        lateinit var painter: SvgPainter

        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                painter = remember { testSvgPainter() }
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(imageSize.value)
                        .scale(imageScale.value)
                )
            }
        }

        waitForIdle()
        assertEquals(
            expected = Size(24f, 24f),
            actual = painter.currentDrawSize
        )

        imageScale.value = 2f
        waitForIdle()
        assertEquals(
            expected = Size(24f, 24f),
            actual = painter.currentDrawSize
        )

        imageScale.value = 1f
        imageSize.value = 48.dp
        waitForIdle()
        assertEquals(
            expected = Size(48f, 48f),
            actual = painter.currentDrawSize
        )

        imageScale.value = 0.5f
        waitForIdle()
        assertEquals(
            expected = Size(48f, 48f),
            actual = painter.currentDrawSize
        )
    }

    private fun testSvgPainter(): SvgPainter =
        TestSvg.encodeToByteArray().decodeToSvgPainter(Density(1f)) as SvgPainter

    private companion object {
        private val TestSvg = """
            <svg width="24" height="24" xmlns="http://www.w3.org/2000/svg">
                <rect width="24" height="24" fill="red"/>
            </svg>
        """.trimIndent()
    }
}
