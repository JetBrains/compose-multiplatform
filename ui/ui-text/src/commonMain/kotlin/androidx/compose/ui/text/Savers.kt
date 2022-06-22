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

package androidx.compose.ui.text

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.util.fastMap

/**
 * Utility function to be able to save nullable values. It also enables not to use with() scope
 * for readability/syntactic purposes.
 */
internal fun <T : Saver<Original, Saveable>, Original, Saveable> save(
    value: Original?,
    saver: T,
    scope: SaverScope
): Any {
    return value?.let { with(saver) { scope.save(value) } } ?: false
}

/**
 * Utility function to restore nullable values. It also enables not to use with() scope
 * for readability/syntactic purposes.
 */
internal inline fun <T : Saver<Original, Saveable>, Original, Saveable, reified Result> restore(
    value: Saveable?,
    saver: T
): Result? {
    if (value == false) return null
    return value?.let { with(saver) { restore(value) } as Result }
}

/**
 * Utility function to save nullable values that does not require a Saver.
 */
internal fun <T> save(value: T?): T? {
    return value
}

/**
 * Utility function to restore nullable values that does not require a Saver.
 */
internal inline fun <reified Result> restore(value: Any?): Result? {
    return value?.let { it as Result }
}

internal val AnnotatedStringSaver = Saver<AnnotatedString, Any>(
    save = {
        arrayListOf(
            save(it.text),
            save(it.spanStyles, AnnotationRangeListSaver, this),
            save(it.paragraphStyles, AnnotationRangeListSaver, this),
            save(it.annotations, AnnotationRangeListSaver, this),
        )
    },
    restore = {
        val list = it as List<Any?>
        AnnotatedString(
            text = restore(list[0])!!,
            spanStyles = restore(list[1], AnnotationRangeListSaver)!!,
            paragraphStyles = restore(list[2], AnnotationRangeListSaver)!!,
            annotations = restore(list[3], AnnotationRangeListSaver)!!,
        )
    }
)

private val AnnotationRangeListSaver = Saver<List<AnnotatedString.Range<out Any>>, Any>(
    save = {
        it.fastMap { range ->
            save(range, AnnotationRangeSaver, this)
        }
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        list.fastMap { item ->
            val range: AnnotatedString.Range<out Any> = restore(item, AnnotationRangeSaver)!!
            range
        }
    }
)

private enum class AnnotationType {
    Paragraph,
    Span,
    VerbatimTts,
    Url,
    String
}

@OptIn(ExperimentalTextApi::class)
private val AnnotationRangeSaver = Saver<AnnotatedString.Range<out Any>, Any>(
    save = {
        val marker = when (it.item) {
            is ParagraphStyle -> AnnotationType.Paragraph
            is SpanStyle -> AnnotationType.Span
            is VerbatimTtsAnnotation -> AnnotationType.VerbatimTts
            is UrlAnnotation -> AnnotationType.Url
            else -> AnnotationType.String
        }

        val item = when (marker) {
            AnnotationType.Paragraph -> save(it.item as ParagraphStyle, ParagraphStyleSaver, this)
            AnnotationType.Span -> save(it.item as SpanStyle, SpanStyleSaver, this)
            AnnotationType.VerbatimTts -> save(
                it.item as VerbatimTtsAnnotation,
                VerbatimTtsAnnotationSaver,
                this
            )
            AnnotationType.Url -> save(
                it.item as UrlAnnotation,
                UrlAnnotationSaver,
                this
            )
            AnnotationType.String -> save(it.item)
        }

        arrayListOf(
            save(marker),
            item,
            save(it.start),
            save(it.end),
            save(it.tag)
        )
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        val marker: AnnotationType = restore(list[0])!!
        val start: Int = restore(list[2])!!
        val end: Int = restore(list[3])!!
        val tag: String = restore(list[4])!!

        when (marker) {
            AnnotationType.Paragraph -> {
                val item: ParagraphStyle = restore(list[1], ParagraphStyleSaver)!!
                AnnotatedString.Range(item = item, start = start, end = end, tag = tag)
            }
            AnnotationType.Span -> {
                val item: SpanStyle = restore(list[1], SpanStyleSaver)!!
                AnnotatedString.Range(item = item, start = start, end = end, tag = tag)
            }
            AnnotationType.VerbatimTts -> {
                val item: VerbatimTtsAnnotation = restore(list[1], VerbatimTtsAnnotationSaver)!!
                AnnotatedString.Range(item = item, start = start, end = end, tag = tag)
            }
            AnnotationType.Url -> {
                val item: UrlAnnotation = restore(list[1], UrlAnnotationSaver)!!
                AnnotatedString.Range(item = item, start = start, end = end, tag = tag)
            }
            AnnotationType.String -> {
                val item: String = restore(list[1])!!
                AnnotatedString.Range(item = item, start = start, end = end, tag = tag)
            }
        }
    }
)

private val VerbatimTtsAnnotationSaver = Saver<VerbatimTtsAnnotation, Any>(
    save = { save(it.verbatim) },
    restore = { VerbatimTtsAnnotation(restore(it)!!) }
)

@OptIn(ExperimentalTextApi::class)
private val UrlAnnotationSaver = Saver<UrlAnnotation, Any>(
    save = { save(it.url) },
    restore = { UrlAnnotation(restore(it)!!) }
)

internal val ParagraphStyleSaver = Saver<ParagraphStyle, Any>(
    save = {
        arrayListOf(
            save(it.textAlign),
            save(it.textDirection),
            save(it.lineHeight, TextUnit.Saver, this),
            save(it.textIndent, TextIndent.Saver, this)
        )
    },
    restore = {
        val list = it as List<Any?>
        ParagraphStyle(
            textAlign = restore(list[0]),
            textDirection = restore(list[1]),
            lineHeight = restore(list[2], TextUnit.Saver)!!,
            textIndent = restore(list[3], TextIndent.Saver)
        )
    }
)

internal val SpanStyleSaver = Saver<SpanStyle, Any>(
    save = {
        arrayListOf(
            save(it.color, Color.Saver, this),
            save(it.fontSize, TextUnit.Saver, this),
            save(it.fontWeight, FontWeight.Saver, this),
            save(it.fontStyle),
            save(it.fontSynthesis),
            save(-1), // TODO save fontFamily
            save(it.fontFeatureSettings),
            save(it.letterSpacing, TextUnit.Saver, this),
            save(it.baselineShift, BaselineShift.Saver, this),
            save(it.textGeometricTransform, TextGeometricTransform.Saver, this),
            save(it.localeList, LocaleList.Saver, this),
            save(it.background, Color.Saver, this),
            save(it.textDecoration, TextDecoration.Saver, this),
            save(it.shadow, Shadow.Saver, this)
        )
    },
    restore = {
        val list = it as List<Any?>
        SpanStyle(
            color = restore(list[0], Color.Saver)!!,
            fontSize = restore(list[1], TextUnit.Saver)!!,
            fontWeight = restore(list[2], FontWeight.Saver),
            fontStyle = restore(list[3]),
            fontSynthesis = restore(list[4]),
            // val fontFamily = list[5] // TODO restore fontFamily
            fontFeatureSettings = restore(list[6]),
            letterSpacing = restore(list[7], TextUnit.Saver)!!,
            baselineShift = restore(list[8], BaselineShift.Saver),
            textGeometricTransform = restore(list[9], TextGeometricTransform.Saver),
            localeList = restore(list[10], LocaleList.Saver),
            background = restore(list[11], Color.Saver)!!,
            textDecoration = restore(list[12], TextDecoration.Saver),
            shadow = restore(list[13], Shadow.Saver)
        )
    }
)

internal val TextDecoration.Companion.Saver: Saver<TextDecoration, Any>
    get() = TextDecorationSaver

private val TextDecorationSaver = Saver<TextDecoration, Any>(
    save = { it.mask },
    restore = { TextDecoration(it as Int) }
)

internal val TextGeometricTransform.Companion.Saver: Saver<TextGeometricTransform, Any>
    get() = TextGeometricTransformSaver

private val TextGeometricTransformSaver = Saver<TextGeometricTransform, Any>(
    save = { arrayListOf(it.scaleX, it.skewX) },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Float>
        TextGeometricTransform(scaleX = list[0], skewX = list[1])
    }
)

internal val TextIndent.Companion.Saver: Saver<TextIndent, Any>
    get() = TextIndentSaver

private val TextIndentSaver = Saver<TextIndent, Any>(
    save = {
        arrayListOf(
            save(it.firstLine, TextUnit.Saver, this),
            save(it.restLine, TextUnit.Saver, this)
        )
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        TextIndent(
            firstLine = restore(list[0], TextUnit.Saver)!!,
            restLine = restore(list[1], TextUnit.Saver)!!
        )
    }
)

internal val FontWeight.Companion.Saver: Saver<FontWeight, Any>
    get() = FontWeightSaver

private val FontWeightSaver = Saver<FontWeight, Any>(
    save = { it.weight },
    restore = { FontWeight(it as Int) }
)

internal val BaselineShift.Companion.Saver: Saver<BaselineShift, Any>
    get() = BaselineShiftSaver

private val BaselineShiftSaver = Saver<BaselineShift, Any>(
    save = { it.multiplier },
    restore = {
        BaselineShift(it as Float)
    }
)

internal val TextRange.Companion.Saver: Saver<TextRange, Any>
    get() = TextRangeSaver

private val TextRangeSaver = Saver<TextRange, Any>(
    save = {
        arrayListOf(save(it.start), save(it.end))
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        TextRange(restore(list[0])!!, restore(list[1])!!)
    }
)

internal val Shadow.Companion.Saver: Saver<Shadow, Any>
    get() = ShadowSaver

private val ShadowSaver = Saver<Shadow, Any>(
    save = {
        arrayListOf(
            save(it.color, Color.Saver, this),
            save(it.offset, Offset.Saver, this),
            save(it.blurRadius)
        )
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        Shadow(
            color = restore(list[0], Color.Saver)!!,
            offset = restore(list[1], Offset.Saver)!!,
            blurRadius = restore(list[2])!!
        )
    }
)

internal val Color.Companion.Saver: Saver<Color, Any>
    get() = ColorSaver

private val ColorSaver = Saver<Color, Any>(
    save = { it.value },
    restore = { Color(it as ULong) }
)

internal val TextUnit.Companion.Saver: Saver<TextUnit, Any>
    get() = TextUnitSaver

@OptIn(ExperimentalUnitApi::class)
private val TextUnitSaver = Saver<TextUnit, Any>(
    save = {
        arrayListOf(save(it.value), save(it.type))
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        TextUnit(restore(list[0])!!, restore(list[1])!!)
    }
)

internal val Offset.Companion.Saver: Saver<Offset, Any>
    get() = OffsetSaver

private val OffsetSaver = Saver<Offset, Any>(
    save = {
        if (it == Offset.Unspecified) {
            false
        } else {
            arrayListOf(save(it.x), save(it.y))
        }
    },
    restore = {
        if (it == false) {
            Offset.Unspecified
        } else {
            val list = it as List<Any?>
            Offset(restore(list[0])!!, restore(list[1])!!)
        }
    }
)

internal val LocaleList.Companion.Saver: Saver<LocaleList, Any>
    get() = LocaleListSaver

private val LocaleListSaver = Saver<LocaleList, Any>(
    save = {
        it.localeList.fastMap { locale ->
            save(locale, Locale.Saver, this)
        }
    },
    restore = {
        @Suppress("UNCHECKED_CAST")
        val list = it as List<Any>
        LocaleList(list.fastMap { item -> restore(item, Locale.Saver)!! })
    }
)

internal val Locale.Companion.Saver: Saver<Locale, Any>
    get() = LocaleSaver

private val LocaleSaver = Saver<Locale, Any>(
    save = { it.toLanguageTag() },
    restore = { Locale(languageTag = it as String) }
)