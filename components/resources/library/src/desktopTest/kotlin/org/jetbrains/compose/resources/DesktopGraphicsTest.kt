//
//package org.jetbrains.compose.resources
//
//import androidx.compose.ui.graphics.*
//import androidx.compose.ui.test.InternalTestApi
//import androidx.compose.ui.test.junit4.DesktopScreenshotTestRule
//import org.jetbrains.skia.Surface
//import org.junit.After
//import org.junit.Rule
//
//@OptIn(InternalTestApi::class)
//abstract class DesktopGraphicsTest {
//    @get:Rule
//    val screenshotRule = DesktopScreenshotTestRule("compose/ui/ui-desktop/graphics")
//
//    private var _surface: Surface? = null
//    protected val surface get() = _surface!!
//
//    protected val redPaint = Paint().apply { color = Color.Red }
//    protected val bluePaint = Paint().apply { color = Color.Blue }
//    protected val greenPaint = Paint().apply { color = Color.Green }
//    protected val cyanPaint = Paint().apply { color = Color.Cyan }
//    protected val magentaPaint = Paint().apply { color = Color.Magenta }
//    protected val yellowPaint = Paint().apply { color = Color.Yellow }
//
//    @Suppress("SameParameterValue")
//    protected fun initCanvas(widthPx: Int, heightPx: Int): Canvas {
//        require(_surface == null)
//        _surface = Surface.makeRasterN32Premul(widthPx, heightPx)
//        return SkiaBackedCanvas(_surface!!.canvas)
//    }
//
//    @After
//    fun teardown() {
//        _surface?.close()
//    }
//}