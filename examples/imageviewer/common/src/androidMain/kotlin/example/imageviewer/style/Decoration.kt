/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.imageviewer.style

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageAsset
import androidx.compose.ui.res.imageResource
import example.imageviewer.R

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
