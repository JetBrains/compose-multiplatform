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
