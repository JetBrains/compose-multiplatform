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

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit

/**
 * Utility function to be able to save nullable values. It also enables not to use with() scope
 * for readability/syntactic purposes.
 */
private fun <T : Saver<Original, Saveable>, Original, Saveable> save(
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
private inline fun <T : Saver<Original, Saveable>, Original, Saveable, reified Result> restore(
    value: Saveable?,
    saver: T
): Result? {
    if (value == false) return null
    return value?.let { with(saver) { restore(value) } as Result }
}

/**
 * Utility function to save nullable values that does not require a Saver.
 */
private fun <T> save(value: T?): T? {
    return value
}

/**
 * Utility function to restore nullable values that does not require a Saver.
 */
private inline fun <reified Result> restore(value: Any?): Result? {
    return value?.let { it as Result }
}

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
            save(-1), // TODO save localeList
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
            // val localeList = list[10]  // TODO restore localeList
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

@OptIn(ExperimentalUnsignedTypes::class)
private val ColorSaver = Saver<Color, Any>(
    save = { it.value },
    restore = { Color(it as ULong) }
)

internal val TextUnit.Companion.Saver: Saver<TextUnit, Any>
    get() = TextUnitSaver

@OptIn(ExperimentalComposeApi::class)
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