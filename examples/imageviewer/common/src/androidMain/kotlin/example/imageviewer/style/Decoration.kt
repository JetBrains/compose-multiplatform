package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.imageResource
import example.imageviewer.common.R

@Composable
fun icEmpty() = imageResource(R.raw.empty)

@Composable
fun icBack() = imageResource(R.raw.back)

@Composable
fun icRefresh() = imageResource(R.raw.refresh)

@Composable
fun icDots() = imageResource(R.raw.dots)

@Composable
fun icFilterGrayscaleOn() = imageResource(R.raw.grayscale_on)

@Composable
fun icFilterGrayscaleOff() = imageResource(R.raw.grayscale_off)

@Composable
fun icFilterPixelOn() = imageResource(R.raw.pixel_on)

@Composable
fun icFilterPixelOff() = imageResource(R.raw.pixel_off)

@Composable
fun icFilterBlurOn() = imageResource(R.raw.blur_on)

@Composable
fun icFilterBlurOff() = imageResource(R.raw.blur_off)

@Composable
fun icFilterUnknown() = imageResource(R.raw.filter_unknown)
