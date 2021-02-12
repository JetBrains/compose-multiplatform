package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import example.imageviewer.common.R

@Composable
fun icEmpty() = painterResource(R.raw.empty)

@Composable
fun icBack() = painterResource(R.raw.back)

@Composable
fun icRefresh() = painterResource(R.raw.refresh)

@Composable
fun icDots() = painterResource(R.raw.dots)

@Composable
fun icFilterGrayscaleOn() = painterResource(R.raw.grayscale_on)

@Composable
fun icFilterGrayscaleOff() = painterResource(R.raw.grayscale_off)

@Composable
fun icFilterPixelOn() = painterResource(R.raw.pixel_on)

@Composable
fun icFilterPixelOff() = painterResource(R.raw.pixel_off)

@Composable
fun icFilterBlurOn() = painterResource(R.raw.blur_on)

@Composable
fun icFilterBlurOff() = painterResource(R.raw.blur_off)

@Composable
fun icFilterUnknown() = painterResource(R.raw.filter_unknown)
