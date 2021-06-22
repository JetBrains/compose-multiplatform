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

package androidx.compose.ui.platform

import android.content.ClipData
import android.content.ClipDescription.MIMETYPE_TEXT_PLAIN
import android.content.Context
import android.os.Parcel
import android.text.Annotation
import android.text.SpannableString
import android.text.Spanned
import android.util.Base64
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.util.fastForEach

private const val PLAIN_TEXT_LABEL = "plain text"

/**
 * Android implementation for [ClipboardManager].
 */
internal class AndroidClipboardManager(context: Context) : ClipboardManager {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as
        android.content.ClipboardManager

    override fun setText(annotatedString: AnnotatedString) {
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                PLAIN_TEXT_LABEL,
                annotatedString.convertToCharSequence()
            )
        )
    }

    override fun getText(): AnnotatedString? {
        return if (clipboardManager.hasPrimaryClip()) {
            clipboardManager.primaryClip!!.getItemAt(0).text.convertToAnnotatedString()
        } else {
            null
        }
    }

    fun hasText() =
        clipboardManager.primaryClipDescription?.hasMimeType(MIMETYPE_TEXT_PLAIN) ?: false
}

internal fun CharSequence?.convertToAnnotatedString(): AnnotatedString? {
    if (this == null) return null
    if (this !is Spanned) {
        return AnnotatedString(text = toString())
    }
    val annotations = getSpans(0, length, Annotation::class.java)
    val spanStyleRanges = mutableListOf<AnnotatedString.Range<SpanStyle>>()
    for (i in 0..annotations.lastIndex) {
        val span = annotations[i]
        if (span.key != "androidx.compose.text.SpanStyle") {
            continue
        }
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        val decodeHelper = DecodeHelper(span.value)
        val spanStyle = decodeHelper.decodeSpanStyle()
        spanStyleRanges.add(AnnotatedString.Range(spanStyle, start, end))
    }
    return AnnotatedString(text = toString(), spanStyles = spanStyleRanges)
}

internal fun AnnotatedString.convertToCharSequence(): CharSequence {
    if (spanStyles.isEmpty()) {
        return text
    }
    val spannableString = SpannableString(text)
    // Normally a SpanStyle will take less than 100 bytes. However, fontFeatureSettings is a string
    // and doesn't have a maximum length defined. Here we set tentatively set maxSize to be 1024.
    val encodeHelper = EncodeHelper()
    spanStyles.fastForEach { (spanStyle, start, end) ->
        encodeHelper.apply {
            reset()
            encode(spanStyle)
        }
        spannableString.setSpan(
            Annotation("androidx.compose.text.SpanStyle", encodeHelper.encodedString()),
            start,
            end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }
    return spannableString
}

/**
 * A helper class used to encode SpanStyles into bytes.
 * Each field of SpanStyle is assigned with an ID. And if a field is not null or Unspecified, it
 * will be encoded. Otherwise, it will simply be omitted to save space.
 */
internal class EncodeHelper {
    private var parcel = Parcel.obtain()

    fun reset() {
        parcel.recycle()
        parcel = Parcel.obtain()
    }

    fun encodedString(): String {
        val bytes = parcel.marshall()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    fun encode(spanStyle: SpanStyle) {
        if (spanStyle.color != Color.Unspecified) {
            encode(COLOR_ID)
            encode(spanStyle.color)
        }
        if (spanStyle.fontSize != TextUnit.Unspecified) {
            encode(FONT_SIZE_ID)
            encode(spanStyle.fontSize)
        }
        spanStyle.fontWeight?.let {
            encode(FONT_WEIGHT_ID)
            encode(it)
        }

        spanStyle.fontStyle?.let {
            encode(FONT_STYLE_ID)
            encode(it)
        }

        spanStyle.fontSynthesis?.let {
            encode(FONT_SYNTHESIS_ID)
            encode(it)
        }

        spanStyle.fontFeatureSettings?.let {
            encode(FONT_FEATURE_SETTINGS_ID)
            encode(it)
        }

        if (spanStyle.letterSpacing != TextUnit.Unspecified) {
            encode(LETTER_SPACING_ID)
            encode(spanStyle.letterSpacing)
        }

        spanStyle.baselineShift?.let {
            encode(BASELINE_SHIFT_ID)
            encode(it)
        }

        spanStyle.textGeometricTransform?.let {
            encode(TEXT_GEOMETRIC_TRANSFORM_ID)
            encode(it)
        }

        if (spanStyle.background != Color.Unspecified) {
            encode(BACKGROUND_ID)
            encode(spanStyle.background)
        }

        spanStyle.textDecoration?.let {
            encode(TEXT_DECORATION_ID)
            encode(it)
        }

        spanStyle.shadow?.let {
            encode(SHADOW_ID)
            encode(it)
        }
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun encode(color: Color) {
        encode(color.value)
    }

    fun encode(textUnit: TextUnit) {
        val typeCode = when (textUnit.type) {
            TextUnitType.Unspecified -> UNIT_TYPE_UNSPECIFIED
            TextUnitType.Sp -> UNIT_TYPE_SP
            TextUnitType.Em -> UNIT_TYPE_EM
            else -> UNIT_TYPE_UNSPECIFIED
        }
        encode(typeCode)
        if (textUnit.type != TextUnitType.Unspecified) {
            encode(textUnit.value)
        }
    }

    fun encode(fontWeight: FontWeight) {
        encode(fontWeight.weight)
    }

    fun encode(fontStyle: FontStyle) {
        encode(
            when (fontStyle) {
                FontStyle.Normal -> FONT_STYLE_NORMAL
                FontStyle.Italic -> FONT_STYLE_ITALIC
                else -> FONT_STYLE_NORMAL
            }
        )
    }

    fun encode(fontSynthesis: FontSynthesis) {
        val value = when (fontSynthesis) {
            FontSynthesis.None -> FONT_SYNTHESIS_NONE
            FontSynthesis.All -> FONT_SYNTHESIS_ALL
            FontSynthesis.Weight -> FONT_SYNTHESIS_WEIGHT
            FontSynthesis.Style -> FONT_SYNTHESIS_STYLE
            else -> FONT_SYNTHESIS_NONE
        }
        encode(value)
    }

    fun encode(baselineShift: BaselineShift) {
        encode(baselineShift.multiplier)
    }

    fun encode(textGeometricTransform: TextGeometricTransform) {
        encode(textGeometricTransform.scaleX)
        encode(textGeometricTransform.skewX)
    }

    fun encode(textDecoration: TextDecoration) {
        encode(textDecoration.mask)
    }

    fun encode(shadow: Shadow) {
        encode(shadow.color)
        encode(shadow.offset.x)
        encode(shadow.offset.y)
        encode(shadow.blurRadius)
    }

    fun encode(byte: Byte) {
        parcel.writeByte(byte)
    }

    fun encode(int: Int) {
        parcel.writeInt(int)
    }

    fun encode(float: Float) {
        parcel.writeFloat(float)
    }

    fun encode(uLong: ULong) {
        parcel.writeLong(uLong.toLong())
    }

    fun encode(string: String) {
        parcel.writeString(string)
    }
}

/**
 * The helper class to decode SpanStyle from a string encoded by [EncodeHelper].
 */
internal class DecodeHelper(string: String) {
    private val parcel = Parcel.obtain()

    init {
        val bytes = Base64.decode(string, Base64.DEFAULT)
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
    }

    /** Decode a SpanStyle from a string. */
    fun decodeSpanStyle(): SpanStyle {
        val mutableSpanStyle = MutableSpanStyle()
        while (parcel.dataAvail() > BYTE_SIZE) {
            when (decodeByte()) {
                COLOR_ID ->
                    if (dataAvailable() >= COLOR_SIZE) {
                        mutableSpanStyle.color = decodeColor()
                    } else {
                        break
                    }
                FONT_SIZE_ID ->
                    if (dataAvailable() >= TEXT_UNIT_SIZE) {
                        mutableSpanStyle.fontSize = decodeTextUnit()
                    } else {
                        break
                    }
                FONT_WEIGHT_ID ->
                    if (dataAvailable() >= FONT_WEIGHT_SIZE) {
                        mutableSpanStyle.fontWeight = decodeFontWeight()
                    } else {
                        break
                    }
                FONT_STYLE_ID ->
                    if (dataAvailable() >= FONT_STYLE_SIZE) {
                        mutableSpanStyle.fontStyle = decodeFontStyle()
                    } else {
                        break
                    }
                FONT_SYNTHESIS_ID ->
                    if (dataAvailable() >= FONT_SYNTHESIS_SIZE) {
                        mutableSpanStyle.fontSynthesis = decodeFontSynthesis()
                    } else {
                        break
                    }
                FONT_FEATURE_SETTINGS_ID ->
                    mutableSpanStyle.fontFeatureSettings = decodeString()
                LETTER_SPACING_ID ->
                    if (dataAvailable() >= TEXT_UNIT_SIZE) {
                        mutableSpanStyle.letterSpacing = decodeTextUnit()
                    } else {
                        break
                    }
                BASELINE_SHIFT_ID ->
                    if (dataAvailable() >= BASELINE_SHIFT_SIZE) {
                        mutableSpanStyle.baselineShift = decodeBaselineShift()
                    } else {
                        break
                    }
                TEXT_GEOMETRIC_TRANSFORM_ID ->
                    if (dataAvailable() >= TEXT_GEOMETRIC_TRANSFORM_SIZE) {
                        mutableSpanStyle.textGeometricTransform = decodeTextGeometricTransform()
                    } else {
                        break
                    }
                BACKGROUND_ID ->
                    if (dataAvailable() >= COLOR_SIZE) {
                        mutableSpanStyle.background = decodeColor()
                    } else {
                        break
                    }
                TEXT_DECORATION_ID ->
                    if (dataAvailable() >= TEXT_DECORATION_SIZE) {
                        mutableSpanStyle.textDecoration = decodeTextDecoration()
                    } else {
                        break
                    }
                SHADOW_ID ->
                    if (dataAvailable() >= SHADOW_SIZE) {
                        mutableSpanStyle.shadow = decodeShadow()
                    } else {
                        break
                    }
            }
        }

        return mutableSpanStyle.toSpanStyle()
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun decodeColor(): Color {
        return Color(decodeULong())
    }

    @OptIn(ExperimentalUnitApi::class)
    fun decodeTextUnit(): TextUnit {
        val type = when (decodeByte()) {
            UNIT_TYPE_SP -> TextUnitType.Sp
            UNIT_TYPE_EM -> TextUnitType.Em
            else -> TextUnitType.Unspecified
        }
        if (type == TextUnitType.Unspecified) {
            return TextUnit.Unspecified
        }
        val value = decodeFloat()
        return TextUnit(value, type)
    }

    @OptIn(ExperimentalUnitApi::class)
    fun decodeFontWeight(): FontWeight {
        return FontWeight(decodeInt())
    }

    fun decodeFontStyle(): FontStyle {
        return when (decodeByte()) {
            FONT_STYLE_NORMAL -> FontStyle.Normal
            FONT_STYLE_ITALIC -> FontStyle.Italic
            else -> FontStyle.Normal
        }
    }

    fun decodeFontSynthesis(): FontSynthesis {
        return when (decodeByte()) {
            FONT_SYNTHESIS_NONE -> FontSynthesis.None
            FONT_SYNTHESIS_ALL -> FontSynthesis.All
            FONT_SYNTHESIS_STYLE -> FontSynthesis.Style
            FONT_SYNTHESIS_WEIGHT -> FontSynthesis.Weight
            else -> FontSynthesis.None
        }
    }

    private fun decodeBaselineShift(): BaselineShift {
        return BaselineShift(decodeFloat())
    }

    private fun decodeTextGeometricTransform(): TextGeometricTransform {
        return TextGeometricTransform(
            scaleX = decodeFloat(),
            skewX = decodeFloat()
        )
    }

    private fun decodeTextDecoration(): TextDecoration {
        val mask = decodeInt()
        val hasLineThrough = mask and TextDecoration.LineThrough.mask != 0
        val hasUnderline = mask and TextDecoration.Underline.mask != 0
        return if (hasLineThrough && hasUnderline) {
            TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
        } else if (hasLineThrough) {
            TextDecoration.LineThrough
        } else if (hasUnderline) {
            TextDecoration.Underline
        } else {
            TextDecoration.None
        }
    }

    private fun decodeShadow(): Shadow {
        return Shadow(
            color = decodeColor(),
            offset = Offset(decodeFloat(), decodeFloat()),
            blurRadius = decodeFloat()
        )
    }

    private fun decodeByte(): Byte {
        return parcel.readByte()
    }

    private fun decodeInt(): Int {
        return parcel.readInt()
    }

    private fun decodeULong(): ULong {
        return parcel.readLong().toULong()
    }

    private fun decodeFloat(): Float {
        return parcel.readFloat()
    }

    private fun decodeString(): String? {
        return parcel.readString()
    }

    private fun dataAvailable(): Int {
        return parcel.dataAvail()
    }
}

private class MutableSpanStyle(
    var color: Color = Color.Unspecified,
    var fontSize: TextUnit = TextUnit.Unspecified,
    var fontWeight: FontWeight? = null,
    var fontStyle: FontStyle? = null,
    var fontSynthesis: FontSynthesis? = null,
    var fontFamily: FontFamily? = null,
    var fontFeatureSettings: String? = null,
    var letterSpacing: TextUnit = TextUnit.Unspecified,
    var baselineShift: BaselineShift? = null,
    var textGeometricTransform: TextGeometricTransform? = null,
    var localeList: LocaleList? = null,
    var background: Color = Color.Unspecified,
    var textDecoration: TextDecoration? = null,
    var shadow: Shadow? = null
) {
    fun toSpanStyle(): SpanStyle {
        return SpanStyle(
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            fontSynthesis = fontSynthesis,
            fontFamily = fontFamily,
            fontFeatureSettings = fontFeatureSettings,
            letterSpacing = letterSpacing,
            baselineShift = baselineShift,
            textGeometricTransform = textGeometricTransform,
            localeList = localeList,
            background = background,
            textDecoration = textDecoration,
            shadow = shadow
        )
    }
}

private const val UNIT_TYPE_UNSPECIFIED: Byte = 0
private const val UNIT_TYPE_SP: Byte = 1
private const val UNIT_TYPE_EM: Byte = 2

private const val FONT_STYLE_NORMAL: Byte = 0
private const val FONT_STYLE_ITALIC: Byte = 1

private const val FONT_SYNTHESIS_NONE: Byte = 0
private const val FONT_SYNTHESIS_ALL: Byte = 1
private const val FONT_SYNTHESIS_WEIGHT: Byte = 2
private const val FONT_SYNTHESIS_STYLE: Byte = 3

private const val COLOR_ID: Byte = 1
private const val FONT_SIZE_ID: Byte = 2
private const val FONT_WEIGHT_ID: Byte = 3
private const val FONT_STYLE_ID: Byte = 4
private const val FONT_SYNTHESIS_ID: Byte = 5
private const val FONT_FEATURE_SETTINGS_ID: Byte = 6
private const val LETTER_SPACING_ID: Byte = 7
private const val BASELINE_SHIFT_ID: Byte = 8
private const val TEXT_GEOMETRIC_TRANSFORM_ID: Byte = 9
private const val BACKGROUND_ID: Byte = 10
private const val TEXT_DECORATION_ID: Byte = 11
private const val SHADOW_ID: Byte = 12

private const val BYTE_SIZE = 1
private const val INT_SIZE = 4
private const val FLOAT_SIZE = 4
private const val LONG_SIZE = 8
private const val COLOR_SIZE = LONG_SIZE
private const val TEXT_UNIT_SIZE = BYTE_SIZE + FLOAT_SIZE
private const val FONT_WEIGHT_SIZE = INT_SIZE
private const val FONT_STYLE_SIZE = BYTE_SIZE
private const val FONT_SYNTHESIS_SIZE = BYTE_SIZE
private const val BASELINE_SHIFT_SIZE = FLOAT_SIZE
private const val TEXT_GEOMETRIC_TRANSFORM_SIZE = FLOAT_SIZE * 2
private const val TEXT_DECORATION_SIZE = INT_SIZE
private const val SHADOW_SIZE = COLOR_SIZE + FLOAT_SIZE * 3