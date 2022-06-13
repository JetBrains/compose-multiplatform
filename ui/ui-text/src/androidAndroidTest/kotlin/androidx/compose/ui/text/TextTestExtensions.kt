/*
 * Copyright 2019 The Android Open Source Project
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
package androidx.compose.ui.text

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.text.font.AndroidFontLoader
import androidx.compose.ui.text.font.AsyncTypefaceCache
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontFamilyResolverImpl
import androidx.compose.ui.text.font.FontListFontFamilyTypefaceAdapter
import androidx.compose.ui.text.font.PlatformFontLoader
import androidx.compose.ui.text.font.PlatformFontFamilyTypefaceAdapter
import androidx.compose.ui.text.font.TypefaceRequestCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.AndroidFontResolveInterceptor
import androidx.compose.ui.text.font.PlatformResolveInterceptor
import kotlin.math.ceil
import kotlin.math.roundToInt

fun Paragraph.bitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(
        width.toIntPx(),
        height.toIntPx(),
        Bitmap.Config.ARGB_8888
    )
    this.paint(androidx.compose.ui.graphics.Canvas(Canvas(bitmap)))
    return bitmap
}

@OptIn(ExperimentalTextApi::class)
fun Paragraph.bitmap(
    brush: Brush,
    alpha: Float
): Bitmap {
    val bitmap = Bitmap.createBitmap(
        width.toIntPx(),
        height.toIntPx(),
        Bitmap.Config.ARGB_8888
    )
    this.paint(androidx.compose.ui.graphics.Canvas(Canvas(bitmap)), brush, alpha)
    return bitmap
}

/**
 * MultiParagraph creates Paragraphs to vertically layout. However, a Paragraph is an immutable
 * object that cannot be changed after its creation. Thus, Brush evaluation according to the total
 * size of MultiParagraph cannot be delegated to Paragraph instances during initialization.
 *
 * We have to re-specify the brush during paint(draw) to apply it according to the total size of
 * MultiParagraph.
 */
@OptIn(ExperimentalTextApi::class)
fun MultiParagraph.bitmap(
    brush: Brush? = null,
    alpha: Float = Float.NaN
): Bitmap {
    val width = paragraphInfoList.maxByOrNull { it.paragraph.width }?.paragraph?.width ?: 0f
    val bitmap = Bitmap.createBitmap(
        width.toIntPx(),
        height.toIntPx(),
        Bitmap.Config.ARGB_8888
    )
    if (brush != null) {
        this.paint(androidx.compose.ui.graphics.Canvas(Canvas(bitmap)), brush, alpha)
    } else {
        this.paint(androidx.compose.ui.graphics.Canvas(Canvas(bitmap)))
    }
    return bitmap
}

@OptIn(ExperimentalTextApi::class)
internal fun UncachedFontFamilyResolver(
    context: Context
): FontFamily.Resolver = UncachedFontFamilyResolver(
    AndroidFontLoader(context),
    AndroidFontResolveInterceptor(context)
)

@OptIn(ExperimentalTextApi::class)
internal fun UncachedFontFamilyResolver(
    platformFontLoader: PlatformFontLoader,
    platformResolveInterceptor: PlatformResolveInterceptor
): FontFamily.Resolver = FontFamilyResolverImpl(
    platformFontLoader,
    platformResolveInterceptor,
    TypefaceRequestCache(),
    FontListFontFamilyTypefaceAdapter(AsyncTypefaceCache()),
    PlatformFontFamilyTypefaceAdapter()
)

fun Float.toIntPx(): Int = ceil(this).roundToInt()

internal fun FloatArray.asRectArray(): Array<Rect> {
    return Array((size) / 4) { index ->
        Rect(
            this[4 * index],
            this[4 * index + 1],
            this[4 * index + 2],
            this[4 * index + 3]
        )
    }
}

internal fun getLtrCharacterBoundariesForTestFont(
    text: String,
    fontSize: Float,
    // assumes that the test font is used and fontSize is equal to default line height
    lineHeight: Float = fontSize,
    initialTop: Float = 0f
): Array<Rect> {
    var top = initialTop
    var left = 0f
    return text.indices.map { index ->
        // if \n, no position update, same as before
        val right = if (text[index] == '\n') left else left + fontSize
        val bottom = top + lineHeight
        Rect(
            left = left,
            top = top,
            right = right,
            bottom = bottom
        ).also {
            if (text[index] == '\n') {
                // left resets to line start
                left = 0f
                // top will go to next line
                top = bottom
            } else {
                // else move to right with one char
                left = right
            }
        }
    }.toTypedArray()
}

internal fun getRtlCharacterBoundariesForTestFont(
    text: String,
    width: Float,
    fontSize: Float,
    lineHeight: Float = fontSize
): Array<Rect> {
    var top = 0f
    var right = width
    return text.indices.map { index ->
        // if \n, position doesn't update, same as before (right)
        // else left is 1 char left
        val left = if (text[index] == '\n') right else right - fontSize
        val bottom = top + lineHeight
        Rect(
            left = left,
            top = top,
            right = right,
            bottom = bottom
        ).also {
            if (text[index] == '\n') {
                // right resets to line start
                right = width
                // top will go to next line
                top = bottom
            } else {
                // else move to left with one char
                right = left
            }
        }
    }.toTypedArray()
}