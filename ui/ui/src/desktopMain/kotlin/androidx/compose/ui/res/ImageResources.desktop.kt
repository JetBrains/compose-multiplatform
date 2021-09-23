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

package androidx.compose.ui.res

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.jetbrains.skia.Image
import java.io.InputStream

/**
 * Load and decode [ImageBitmap] from the given [inputStream]. [inputStream] should contain encoded
 * raster image in a format supported by Skia (BMP, GIF, HEIF, ICO, JPEG, PNG, WBMP, WebP)
 *
 * @param inputStream input stream to load an rater image. All bytes will be read from this
 * stream, but stream will not be closed after this method.
 * @return the decoded SVG image associated with the resource
 */
fun loadImageBitmap(inputStream: InputStream): ImageBitmap =
    Image.makeFromEncoded(inputStream.readAllBytes()).asImageBitmap()