/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.text.font

import android.content.res.AssetManager
import android.graphics.Typeface
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.android.InternalPlatformTextApi
import java.io.File

/**
 * Create a Font declaration from a file in the assets directory. The content of the [File] is
 * read during construction.
 *
 * @param assetManager Android AssetManager
 * @param path full path starting from the assets directory (i.e. dir/myfont.ttf for
 * assets/dir/myfont.ttf).
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 */
@ExperimentalTextApi
@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
@Stable
fun Font(
    assetManager: AssetManager,
    path: String,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = AndroidAssetFont(assetManager, path, weight, style)

/**
 * Create a Font declaration from a file. The content of the [File] is read during construction.
 *
 * @param file the font file.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 */
@ExperimentalTextApi
@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
@Stable
@Suppress("StreamFiles")
fun Font(
    file: File,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = AndroidFileFont(file, weight, style)

/**
 * Create a Font declaration from a [ParcelFileDescriptor]. The content of the
 * [ParcelFileDescriptor] is read during construction.
 *
 * @param fileDescriptor the file descriptor for the font file.
 * @param weight The weight of the font. The system uses this to match a font to a font request
 * that is given in a [androidx.compose.ui.text.SpanStyle].
 * @param style The style of the font, normal or italic. The system uses this to match a font to a
 * font request that is given in a [androidx.compose.ui.text.SpanStyle].
 */
@RequiresApi(26)
@ExperimentalTextApi
@OptIn(InternalPlatformTextApi::class, ExperimentalTextApi::class)
@Stable
fun Font(
    fileDescriptor: ParcelFileDescriptor,
    weight: FontWeight = FontWeight.Normal,
    style: FontStyle = FontStyle.Normal
): Font = AndroidFileDescriptorFont(fileDescriptor, weight, style)

internal interface AndroidFont : Font {
    val typeface: Typeface
}

internal class AndroidAssetFont constructor(
    val assetManager: AssetManager,
    val path: String,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : AndroidFont {
    private val typefaceInternal = Typeface.createFromAsset(assetManager, path)

    override val typeface: Typeface get() = typefaceInternal
}

internal class AndroidFileFont constructor(
    val file: File,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : AndroidFont {
    private val typefaceInternal = Typeface.createFromFile(file)

    override val typeface: Typeface get() = typefaceInternal
}

@RequiresApi(26)
internal class AndroidFileDescriptorFont constructor(
    val fileDescriptor: ParcelFileDescriptor,
    override val weight: FontWeight = FontWeight.Normal,
    override val style: FontStyle = FontStyle.Normal
) : AndroidFont {
    private val typefaceInternal = if (Build.VERSION.SDK_INT >= 26) {
        AndroidFileDescriptorHelper.create(fileDescriptor)
    } else {
        throw IllegalArgumentException("Cannot create font from file descriptor for SDK < 26")
    }

    override val typeface: Typeface get() = typefaceInternal
}

@RequiresApi(26)
private object AndroidFileDescriptorHelper {
    @RequiresApi(26)
    @DoNotInline
    fun create(fileDescriptor: ParcelFileDescriptor): Typeface {
        return Typeface.Builder(fileDescriptor.fileDescriptor).build()
    }
}
