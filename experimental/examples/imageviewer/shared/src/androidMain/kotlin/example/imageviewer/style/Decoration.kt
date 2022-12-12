package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import example.imageviewer.common.R

@Composable
fun icEmpty() = painterResource(R.drawable.empty)

@Composable
fun icBack() = painterResource(R.drawable.back)

@Composable
fun icRefresh() = painterResource(R.drawable.refresh)

@Composable
fun icDots() = painterResource(R.drawable.dots)

@Composable
fun icFilterGrayscaleOn() = painterResource(R.drawable.grayscale_on)

@Composable
fun icFilterGrayscaleOff() = painterResource(R.drawable.grayscale_off)

@Composable
fun icFilterPixelOn() = painterResource(R.drawable.pixel_on)

@Composable
fun icFilterPixelOff() = painterResource(R.drawable.pixel_off)

@Composable
fun icFilterBlurOn() = painterResource(R.drawable.blur_on)

@Composable
fun icFilterBlurOff() = painterResource(R.drawable.blur_off)

@Composable
fun icFilterUnknown() = painterResource(R.drawable.filter_unknown)
