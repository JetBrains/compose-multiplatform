package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.res.imageResource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@Composable
fun icEmpty(): ImageAsset = imageResource("images/empty.png")

@Composable
fun icBack(): ImageAsset = imageResource("images/back.png")

@Composable
fun icRefresh(): ImageAsset = imageResource("images/refresh.png")

@Composable
fun icDots(): ImageAsset = imageResource("images/dots.png")

@Composable
fun icFilterGrayscaleOn(): ImageAsset = imageResource("images/grayscale_on.png")

@Composable
fun icFilterGrayscaleOff(): ImageAsset = imageResource("images/grayscale_off.png")

@Composable
fun icFilterPixelOn(): ImageAsset = imageResource("images/pixel_on.png")

@Composable
fun icFilterPixelOff(): ImageAsset = imageResource("images/pixel_off.png")

@Composable
fun icFilterBlurOn(): ImageAsset = imageResource("images/blur_on.png")

@Composable
fun icFilterBlurOff(): ImageAsset = imageResource("images/blur_off.png")

@Composable
fun icFilterUnknown(): ImageAsset = imageResource("images/filter_unknown.png")

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
