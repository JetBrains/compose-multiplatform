
package org.jetbrains.compose.resources

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.junit4.DesktopComposeTestRule
import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(InternalTestApi::class, ExperimentalResourceApi::class)
class DesktopImageBitmapTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun testSameResource() {
        runBlocking (Dispatchers.Main) {
            rule.setContent {
//                assertEquals(resource("img.png"), resource("img1"))
            }
        }
    }

}
