package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.res.imageResource
import example.imageviewer.common.R

@Composable
fun icEmpty(): ImageAsset = imageResource(R.raw.empty)

@Composable
fun icBack(): ImageAsset = imageResource(R.raw.back)

@Composable
fun icRefresh(): ImageAsset = imageResource(R.raw.refresh)

@Composable
fun icDots(): ImageAsset = imageResource(R.raw.dots)

@Composable
fun icFilterGrayscaleOn(): ImageAsset = imageResource(R.raw.grayscale_on)

@Composable
fun icFilterGrayscaleOff(): ImageAsset = imageResource(R.raw.grayscale_off)

@Composable
fun icFilterPixelOn(): ImageAsset = imageResource(R.raw.pixel_on)

@Composable
fun icFilterPixelOff(): ImageAsset = imageResource(R.raw.pixel_off)

@Composable
fun icFilterBlurOn(): ImageAsset = imageResource(R.raw.blur_on)

@Composable
fun icFilterBlurOff(): ImageAsset = imageResource(R.raw.blur_off)

@Composable
fun icFilterUnknown(): ImageAsset = imageResource(R.raw.filter_unknown)
