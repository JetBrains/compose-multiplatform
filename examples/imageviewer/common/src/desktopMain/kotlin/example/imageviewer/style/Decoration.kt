package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

@Composable
fun icEmpty() = painterResource("images/empty.png")

@Composable
fun icBack() = painterResource("images/back.png")

@Composable
fun icRefresh() = painterResource("images/refresh.png")

@Composable
fun icDots() = painterResource("images/dots.png")

@Composable
fun icFilterGrayscaleOn() = painterResource("images/grayscale_on.png")

@Composable
fun icFilterGrayscaleOff() = painterResource("images/grayscale_off.png")

@Composable
fun icFilterPixelOn() = painterResource("images/pixel_on.png")

@Composable
fun icFilterPixelOff() = painterResource("images/pixel_off.png")

@Composable
fun icFilterBlurOn() = painterResource("images/blur_on.png")

@Composable
fun icFilterBlurOff() = painterResource("images/blur_off.png")

@Composable
fun icFilterUnknown() = painterResource("images/filter_unknown.png")

@Composable
fun icAppRounded() = painterResource("images/ic_imageviewer_round.png")
