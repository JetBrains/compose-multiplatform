package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.imageResource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@Composable
fun icEmpty() = imageResource("images/empty.png")

@Composable
fun icBack() = imageResource("images/back.png")

@Composable
fun icRefresh() = imageResource("images/refresh.png")

@Composable
fun icDots() = imageResource("images/dots.png")

@Composable
fun icFilterGrayscaleOn() = imageResource("images/grayscale_on.png")

@Composable
fun icFilterGrayscaleOff() = imageResource("images/grayscale_off.png")

@Composable
fun icFilterPixelOn() = imageResource("images/pixel_on.png")

@Composable
fun icFilterPixelOff() = imageResource("images/pixel_off.png")

@Composable
fun icFilterBlurOn() = imageResource("images/blur_on.png")

@Composable
fun icFilterBlurOff() = imageResource("images/blur_off.png")

@Composable
fun icFilterUnknown() = imageResource("images/filter_unknown.png")

private var icon: BufferedImage? = null
fun icAppRounded(): BufferedImage {
    if (icon != null) {
        return icon!!
    }
    try {
        val imageRes = "images/ic_imageviewer_round.png"
        val img = Thread.currentThread().contextClassLoader.getResource(imageRes)
        val bitmap: BufferedImage? = ImageIO.read(img)
        if (bitmap != null) {
            icon = bitmap
            return bitmap
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
}
