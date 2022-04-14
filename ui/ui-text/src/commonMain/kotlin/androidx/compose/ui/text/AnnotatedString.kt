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

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * The basic data structure of text with multiple styles. To construct an [AnnotatedString] you
 * can use [Builder].
 */
@Immutable
class AnnotatedString internal constructor(
    val text: String,
    val spanStyles: List<Range<SpanStyle>> = emptyList(),
    val paragraphStyles: List<Range<ParagraphStyle>> = emptyList(),
    internal val annotations: List<Range<out Any>> = emptyList()
) : CharSequence {
    /**
     * The basic data structure of text with multiple styles. To construct an [AnnotatedString]
     * you can use [Builder].
     *
     * @param text the text to be displayed.
     * @param spanStyles a list of [Range]s that specifies [SpanStyle]s on certain portion of the
     * text. These styles will be applied in the order of the list. And the [SpanStyle]s applied
     * later can override the former styles. Notice that [SpanStyle] attributes which are null or
     * [androidx.compose.ui.unit.TextUnit.Unspecified] won't change the current ones.
     * @param paragraphStyles a list of [Range]s that specifies [ParagraphStyle]s on certain
     * portion of the text. Each [ParagraphStyle] with a [Range] defines a paragraph of text.
     * It's required that [Range]s of paragraphs don't overlap with each other. If there are gaps
     * between specified paragraph [Range]s, a default paragraph will be created in between.
     *
     * @throws IllegalArgumentException if [paragraphStyles] contains any two overlapping [Range]s.
     * @sample androidx.compose.ui.text.samples.AnnotatedStringConstructorSample
     *
     * @see SpanStyle
     * @see ParagraphStyle
     */
    constructor(
        text: String,
        spanStyles: List<Range<SpanStyle>> = listOf(),
        paragraphStyles: List<Range<ParagraphStyle>> = listOf()
    ) : this(text, spanStyles, paragraphStyles, listOf())

    init {
        var lastStyleEnd = -1
        paragraphStyles.fastForEach { paragraphStyle ->
            require(paragraphStyle.start >= lastStyleEnd) { "ParagraphStyle should not overlap" }
            require(paragraphStyle.end <= text.length) {
                "ParagraphStyle range [${paragraphStyle.start}, ${paragraphStyle.end})" +
                    " is out of boundary"
            }
            lastStyleEnd = paragraphStyle.end
        }
    }

    override val length: Int
        get() = text.length

    override operator fun get(index: Int): Char = text[index]

    /**
     * Return a substring for the AnnotatedString and include the styles in the range of [startIndex]
     * (inclusive) and [endIndex] (exclusive).
     *
     * @param startIndex the inclusive start offset of the range
     * @param endIndex the exclusive end offset of the range
     */
    override fun subSequence(startIndex: Int, endIndex: Int): AnnotatedString {
        require(startIndex <= endIndex) {
            "start ($startIndex) should be less or equal to end ($endIndex)"
        }
        if (startIndex == 0 && endIndex == text.length) return this
        val text = text.substring(startIndex, endIndex)
        return AnnotatedString(
            text = text,
            spanStyles = filterRanges(spanStyles, startIndex, endIndex),
            paragraphStyles = filterRanges(paragraphStyles, startIndex, endIndex),
            annotations = filterRanges(annotations, startIndex, endIndex)
        )
    }

    /**
     * Return a substring for the AnnotatedString and include the styles in the given [range].
     *
     * @param range the text range
     *
     * @see subSequence(start: Int, end: Int)
     */
    fun subSequence(range: TextRange): AnnotatedString {
        return subSequence(range.min, range.max)
    }

    @Stable
    operator fun plus(other: AnnotatedString): AnnotatedString {
        return with(Builder(this)) {
            append(other)
            toAnnotatedString()
        }
    }

    /**
     * Query the string annotations attached on this AnnotatedString.
     * Annotations are metadata attached on the AnnotatedString, for example, a URL is a string
     * metadata attached on the a certain range. Annotations are also store with [Range] like the
     * styles.
     *
     * @param tag the tag of the annotations that is being queried. It's used to distinguish
     * the annotations for different purposes.
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range].  Notice that All annotations that intersect
     * with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     * list will be returned.
     */
    @Suppress("UNCHECKED_CAST")
    fun getStringAnnotations(tag: String, start: Int, end: Int): List<Range<String>> =
        annotations.fastFilter {
            it.item is String && tag == it.tag && intersect(start, end, it.start, it.end)
        } as List<Range<String>>

    /**
     * Query all of the string annotations attached on this AnnotatedString.
     *
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range].  Notice that All annotations that intersect
     * with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     * list will be returned.
     */
    @Suppress("UNCHECKED_CAST")
    fun getStringAnnotations(start: Int, end: Int): List<Range<String>> =
        annotations.fastFilter {
            it.item is String && intersect(start, end, it.start, it.end)
        } as List<Range<String>>

    /**
     * Query all of the string annotations attached on this AnnotatedString.
     *
     * @param start the start of the query range, inclusive.
     * @param end the end of the query range, exclusive.
     * @return a list of annotations stored in [Range].  Notice that All annotations that intersect
     * with the range [start, end) will be returned. When [start] is bigger than [end], an empty
     * list will be returned.
     */
    @Suppress("UNCHECKED_CAST")
    fun getTtsAnnotations(start: Int, end: Int): List<Range<TtsAnnotation>> =
        annotations.fastFilter {
            it.item is TtsAnnotation && intersect(start, end, it.start, it.end)
        } as List<Range<TtsAnnotation>>

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AnnotatedString) return false
        if (text != other.text) return false
        if (spanStyles != other.spanStyles) return false
        if (paragraphStyles != other.paragraphStyles) return false
        if (annotations != other.annotations) return false
        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + spanStyles.hashCode()
        result = 31 * result + paragraphStyles.hashCode()
        result = 31 * result + annotations.hashCode()
        return result
    }

    override fun toString(): String {
        // AnnotatedString.toString has special value, it converts it into regular String
        // rather than debug string.
        return text
    }

    /**
     * The information attached on the text such as a [SpanStyle].
     *
     * @param item The object attached to [AnnotatedString]s.
     * @param start The start of the range where [item] takes effect. It's inclusive
     * @param end The end of the range where [item] takes effect. It's exclusive
     * @param tag The tag used to distinguish the different ranges. It is useful to store custom
     * data. And [Range]s with same tag can be queried with functions such as [getStringAnnotations].
     */
    @Immutable
    data class Range<T>(val item: T, val start: Int, val end: Int, val tag: String) {
        constructor(item: T, start: Int, end: Int) : this(item, start, end, "")
        init {
            require(start <= end) { "Reversed range is not supported" }
        }
    }

    /**
     * Builder class for AnnotatedString. Enables construction of an [AnnotatedString] using
     * methods such as [append] and [addStyle].
     *
     * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderSample
     *
     * @param capacity initial capacity for the internal char buffer
     */
    class Builder(capacity: Int = 16) {

        private data class MutableRange<T>(
            val item: T,
            val start: Int,
            var end: Int = Int.MIN_VALUE,
            val tag: String = ""
        ) {
            /**
             * Create an immutable [Range] object.
             *
             * @param defaultEnd if the end is not set yet, it will be set to this value.
             */
            fun toRange(defaultEnd: Int = Int.MIN_VALUE): Range<T> {
                val end = if (end == Int.MIN_VALUE) defaultEnd else end
                check(end != Int.MIN_VALUE) { "Item.end should be set first" }
                return Range(item = item, start = start, end = end, tag = tag)
            }
        }

        private val text: StringBuilder = StringBuilder(capacity)
        private val spanStyles: MutableList<MutableRange<SpanStyle>> = mutableListOf()
        private val paragraphStyles: MutableList<MutableRange<ParagraphStyle>> = mutableListOf()
        private val annotations: MutableList<MutableRange<out Any>> = mutableListOf()
        private val styleStack: MutableList<MutableRange<out Any>> = mutableListOf()

        /**
         * Create an [Builder] instance using the given [String].
         */
        constructor(text: String) : this() {
            append(text)
        }

        /**
         * Create an [Builder] instance using the given [AnnotatedString].
         */
        constructor(text: AnnotatedString) : this() {
            append(text)
        }

        /**
         * Returns the length of the [String].
         */
        val length: Int get() = text.length

        /**
         * Appends the given [String] to this [Builder].
         *
         * @param text the text to append
         */
        fun append(text: String) {
            this.text.append(text)
        }

        /**
         * Appends the given [Char] to this [Builder].
         *
         * @param char the Char to append
         */
        fun append(char: Char) {
            this.text.append(char)
        }

        /**
         * Appends the given [AnnotatedString] to this [Builder].
         *
         * @param text the text to append
         */
        fun append(text: AnnotatedString) {
            val start = this.text.length
            this.text.append(text.text)
            // offset every style with start and add to the builder
            text.spanStyles.fastForEach {
                addStyle(it.item, start + it.start, start + it.end)
            }
            text.paragraphStyles.fastForEach {
                addStyle(it.item, start + it.start, start + it.end)
            }

            text.annotations.fastForEach {
                annotations.add(
                    MutableRange(it.item, start + it.start, start + it.end, it.tag)
                )
            }
        }

        /**
         * Set a [SpanStyle] for the given [range].
         *
         * @param style [SpanStyle] to be applied
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         */
        fun addStyle(style: SpanStyle, start: Int, end: Int) {
            spanStyles.add(MutableRange(item = style, start = start, end = end))
        }

        /**
         * Set a [ParagraphStyle] for the given [range]. When a [ParagraphStyle] is applied to the
         * [AnnotatedString], it will be rendered as a separate paragraph.
         *
         * @param style [ParagraphStyle] to be applied
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         */
        fun addStyle(style: ParagraphStyle, start: Int, end: Int) {
            paragraphStyles.add(MutableRange(item = style, start = start, end = end))
        }

        /**
         * Set an Annotation for the given [range].
         *
         * @param tag the tag used to distinguish annotations
         * @param annotation the string annotation that is attached
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         * @see getStringAnnotations
         * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
         */
        fun addStringAnnotation(tag: String, annotation: String, start: Int, end: Int) {
            annotations.add(MutableRange(annotation, start, end, tag))
        }

        /**
         * Set a [TtsAnnotation] for the given [range].
         *
         * @param ttsAnnotation an object stores text to speech metadata that intended for the
         * TTS engine.
         * @param start the inclusive starting offset of the range
         * @param end the exclusive end offset of the range
         * @see getStringAnnotations
         * @sample androidx.compose.ui.text.samples.AnnotatedStringAddStringAnnotationSample
         */
        @ExperimentalTextApi
        @Suppress("SetterReturnsThis")
        fun addTtsAnnotation(ttsAnnotation: TtsAnnotation, start: Int, end: Int) {
            annotations.add(MutableRange(ttsAnnotation, start, end))
        }

        /**
         * Applies the given [SpanStyle] to any appended text until a corresponding [pop] is
         * called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushSample
         *
         * @param style SpanStyle to be applied
         */
        fun pushStyle(style: SpanStyle): Int {
            MutableRange(item = style, start = text.length).also {
                styleStack.add(it)
                spanStyles.add(it)
            }
            return styleStack.size - 1
        }

        /**
         * Applies the given [ParagraphStyle] to any appended text until a corresponding [pop]
         * is called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushParagraphStyleSample
         *
         * @param style ParagraphStyle to be applied
         */
        fun pushStyle(style: ParagraphStyle): Int {
            MutableRange(item = style, start = text.length).also {
                styleStack.add(it)
                paragraphStyles.add(it)
            }
            return styleStack.size - 1
        }

        /**
         * Attach the given [annotation] to any appended text until a corresponding [pop]
         * is called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
         *
         * @param tag the tag used to distinguish annotations
         * @param annotation the string annotation attached on this AnnotatedString
         * @see getStringAnnotations
         * @see Range
         */
        fun pushStringAnnotation(tag: String, annotation: String): Int {
            MutableRange(item = annotation, start = text.length, tag = tag).also {
                styleStack.add(it)
                annotations.add(it)
            }
            return styleStack.size - 1
        }

        /**
         * Attach the given [ttsAnnotation] to any appended text until a corresponding [pop]
         * is called.
         *
         * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderPushStringAnnotationSample
         *
         * @param ttsAnnotation an object stores text to speech metadata that intended for the
         * TTS engine.
         * @see getStringAnnotations
         * @see Range
         */
        fun pushTtsAnnotation(ttsAnnotation: TtsAnnotation): Int {
            MutableRange(item = ttsAnnotation, start = text.length).also {
                styleStack.add(it)
                annotations.add(it)
            }
            return styleStack.size - 1
        }

        /**
         * Ends the style or annotation that was added via a push operation before.
         *
         * @see pushStyle
         * @see pushStringAnnotation
         */
        fun pop() {
            check(styleStack.isNotEmpty()) { "Nothing to pop." }
            // pop the last element
            val item = styleStack.removeAt(styleStack.size - 1)
            item.end = text.length
        }

        /**
         * Ends the styles or annotation up to and `including` the [pushStyle] or
         * [pushStringAnnotation] that returned the given index.
         *
         * @param index the result of the a previous [pushStyle] or [pushStringAnnotation] in order
         * to pop to
         *
         * @see pop
         * @see pushStyle
         * @see pushStringAnnotation
         */
        fun pop(index: Int) {
            check(index < styleStack.size) { "$index should be less than ${styleStack.size}" }
            while ((styleStack.size - 1) >= index) {
                pop()
            }
        }

        /**
         * Constructs an [AnnotatedString] based on the configurations applied to the [Builder].
         */
        fun toAnnotatedString(): AnnotatedString {
            return AnnotatedString(
                text = text.toString(),
                spanStyles = spanStyles.fastMap { it.toRange(text.length) },
                paragraphStyles = paragraphStyles.fastMap { it.toRange(text.length) },
                annotations = annotations.fastMap { it.toRange(text.length) }
            )
        }
    }
}

/**
 * A helper function used to determine the paragraph boundaries in [MultiParagraph].
 *
 * It reads paragraph information from [AnnotatedString.paragraphStyles] where only some parts of
 * text has [ParagraphStyle] specified, and unspecified parts(gaps between specified paragraphs)
 * are considered as default paragraph with default [ParagraphStyle].
 * For example, the following string with a specified paragraph denoted by "[]"
 *      "Hello WorldHi!"
 *      [          ]
 * The result paragraphs are "Hello World" and "Hi!".
 *
 * @param defaultParagraphStyle The default [ParagraphStyle]. It's used for both unspecified
 *  default paragraphs and specified paragraph. When a specified paragraph's [ParagraphStyle] has
 *  a null attribute, the default one will be used instead.
 */
internal fun AnnotatedString.normalizedParagraphStyles(
    defaultParagraphStyle: ParagraphStyle
): List<Range<ParagraphStyle>> {
    val length = text.length
    val paragraphStyles = paragraphStyles

    var lastOffset = 0
    val result = mutableListOf<Range<ParagraphStyle>>()
    paragraphStyles.fastForEach { (style, start, end) ->
        if (start != lastOffset) {
            result.add(Range(defaultParagraphStyle, lastOffset, start))
        }
        result.add(Range(defaultParagraphStyle.merge(style), start, end))
        lastOffset = end
    }
    if (lastOffset != length) {
        result.add(Range(defaultParagraphStyle, lastOffset, length))
    }
    // This is a corner case where annotatedString is an empty string without any ParagraphStyle.
    // In this case, an empty ParagraphStyle is created.
    if (result.isEmpty()) {
        result.add(Range(defaultParagraphStyle, 0, 0))
    }
    return result
}

/**
 * Helper function used to find the [SpanStyle]s in the given paragraph range and also convert the
 * range of those [SpanStyle]s to paragraph local range.
 *
 * @param start The start index of the paragraph range, inclusive
 * @param end The end index of the paragraph range, exclusive
 * @return The list of converted [SpanStyle]s in the given paragraph range
 */
private fun AnnotatedString.getLocalStyles(
    start: Int,
    end: Int
): List<Range<SpanStyle>> {
    if (start == end) return listOf()
    // If the given range covers the whole AnnotatedString, return SpanStyles without conversion.
    if (start == 0 && end >= this.text.length) {
        return spanStyles
    }
    return spanStyles.fastFilter { intersect(start, end, it.start, it.end) }
        .fastMap {
            Range(
                it.item,
                it.start.coerceIn(start, end) - start,
                it.end.coerceIn(start, end) - start
            )
        }
}

/**
 * Helper function used to return another AnnotatedString that is a substring from [start] to
 * [end]. This will ignore the [ParagraphStyle]s and the resulting [AnnotatedString] will have no
 * [ParagraphStyle]s.
 *
 * @param start The start index of the paragraph range, inclusive
 * @param end The end index of the paragraph range, exclusive
 * @return The list of converted [SpanStyle]s in the given paragraph range
 */
private fun AnnotatedString.substringWithoutParagraphStyles(
    start: Int,
    end: Int
): AnnotatedString {
    return AnnotatedString(
        text = if (start != end) text.substring(start, end) else "",
        spanStyles = getLocalStyles(start, end)
    )
}

internal inline fun <T> AnnotatedString.mapEachParagraphStyle(
    defaultParagraphStyle: ParagraphStyle,
    crossinline block: (
        annotatedString: AnnotatedString,
        paragraphStyle: Range<ParagraphStyle>
    ) -> T
): List<T> {
    return normalizedParagraphStyles(defaultParagraphStyle).fastMap { paragraphStyleRange ->
        val annotatedString = substringWithoutParagraphStyles(
            paragraphStyleRange.start,
            paragraphStyleRange.end
        )
        block(annotatedString, paragraphStyleRange)
    }
}

/**
 * Create upper case transformed [AnnotatedString]
 *
 * The uppercase sometimes maps different number of characters. This function adjusts the text
 * style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the uppercase mapping context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for upper case mapping. Only the first locale is
 *                   effective. If empty locale list is passed, use the current locale instead.
 * @return A uppercase transformed string.
 */
fun AnnotatedString.toUpperCase(localeList: LocaleList = LocaleList.current): AnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toUpperCase(localeList) }
}

/**
 * Create lower case transformed [AnnotatedString]
 *
 * The lowercase sometimes maps different number of characters. This function adjusts the text
 * style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the lowercase mapping context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for lower case mapping. Only the first locale is
 *                   effective. If empty locale list is passed, use the current locale instead.
 * @return A lowercase transformed string.
 */
fun AnnotatedString.toLowerCase(localeList: LocaleList = LocaleList.current): AnnotatedString {
    return transform { str, start, end -> str.substring(start, end).toLowerCase(localeList) }
}

/**
 * Create capitalized [AnnotatedString]
 *
 * The capitalization sometimes maps different number of characters. This function adjusts the
 * text style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the capitalization context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for capitalize mapping. Only the first locale is
 *                   effective. If empty locale list is passed, use the current locale instead.
 *                   Note that, this locale is currently ignored since underlying Kotlin method
 *                   is experimental.
 * @return A capitalized string.
 */
fun AnnotatedString.capitalize(localeList: LocaleList = LocaleList.current): AnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).capitalize(localeList)
        } else {
            str.substring(start, end)
        }
    }
}

/**
 * Create capitalized [AnnotatedString]
 *
 * The decapitalization sometimes maps different number of characters. This function adjusts
 * the text style and paragraph style ranges to transformed offset.
 *
 * Note, if the style's offset is middle of the decapitalization context, this function won't
 * transform the character, e.g. style starts from between base alphabet character and accent
 * character.
 *
 * @param localeList A locale list used for decapitalize mapping. Only the first locale is
 *                   effective. If empty locale list is passed, use the current locale instead.
 *                   Note that, this locale is currently ignored since underlying Kotlin method
 *                   is experimental.
 * @return A decapitalized string.
 */
fun AnnotatedString.decapitalize(localeList: LocaleList = LocaleList.current): AnnotatedString {
    return transform { str, start, end ->
        if (start == 0) {
            str.substring(start, end).decapitalize(localeList)
        } else {
            str.substring(start, end)
        }
    }
}

/**
 * The core function of [AnnotatedString] transformation.
 *
 * @param transform the transformation method
 * @return newly allocated transformed AnnotatedString
 */
internal expect fun AnnotatedString.transform(
    transform: (String, Int, Int) -> String
): AnnotatedString

/**
 * Pushes [style] to the [AnnotatedString.Builder], executes [block] and then pops the [style].
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderWithStyleSample
 *
 * @param style [SpanStyle] to be applied
 * @param block function to be executed
 *
 * @return result of the [block]
 *
 * @see AnnotatedString.Builder.pushStyle
 * @see AnnotatedString.Builder.pop
 */
inline fun <R : Any> Builder.withStyle(
    style: SpanStyle,
    block: Builder.() -> R
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes [style] to the [AnnotatedString.Builder], executes [block] and then pops the [style].
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderWithStyleSample
 *
 * @param style [SpanStyle] to be applied
 * @param block function to be executed
 *
 * @return result of the [block]
 *
 * @see AnnotatedString.Builder.pushStyle
 * @see AnnotatedString.Builder.pop
 */
inline fun <R : Any> Builder.withStyle(
    style: ParagraphStyle,
    crossinline block: Builder.() -> R
): R {
    val index = pushStyle(style)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes an annotation to the [AnnotatedString.Builder], executes [block] and then pops the
 * annotation.
 *
 * @param tag the tag used to distinguish annotations
 * @param annotation the string annotation attached on this AnnotatedString
 * @param block function to be executed
 *
 * @return result of the [block]
 *
 * @see AnnotatedString.Builder.pushStringAnnotation
 * @see AnnotatedString.Builder.pop
 */
@ExperimentalTextApi
inline fun <R : Any> Builder.withAnnotation(
    tag: String,
    annotation: String,
    crossinline block: Builder.() -> R
): R {
    val index = pushStringAnnotation(tag, annotation)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Pushes an [TtsAnnotation] to the [AnnotatedString.Builder], executes [block] and then pops the
 * annotation.
 *
 * @param ttsAnnotation an object stores text to speech metadata that intended for the TTS engine.
 * @param block function to be executed
 *
 * @return result of the [block]
 *
 * @see AnnotatedString.Builder.pushStringAnnotation
 * @see AnnotatedString.Builder.pop
 */
@ExperimentalTextApi
inline fun <R : Any> Builder.withAnnotation(
    ttsAnnotation: TtsAnnotation,
    crossinline block: Builder.() -> R
): R {
    val index = pushTtsAnnotation(ttsAnnotation)
    return try {
        block(this)
    } finally {
        pop(index)
    }
}

/**
 * Filter the range list based on [Range.start] and [Range.end] to include ranges only in the range
 * of [start] (inclusive) and [end] (exclusive).
 *
 * @param start the inclusive start offset of the text range
 * @param end the exclusive end offset of the text range
 */
private fun <T> filterRanges(ranges: List<Range<out T>>, start: Int, end: Int): List<Range<T>> {
    require(start <= end) { "start ($start) should be less than or equal to end ($end)" }
    return ranges.fastFilter { intersect(start, end, it.start, it.end) }.fastMap {
        Range(
            item = it.item,
            start = maxOf(start, it.start) - start,
            end = minOf(end, it.end) - start,
            tag = it.tag
        )
    }
}

/**
 * Create an AnnotatedString with a [spanStyle] that will apply to the whole text.
 *
 * @param spanStyle [SpanStyle] to be applied to whole text
 * @param paragraphStyle [ParagraphStyle] to be applied to whole text
 */
fun AnnotatedString(
    text: String,
    spanStyle: SpanStyle,
    paragraphStyle: ParagraphStyle? = null
): AnnotatedString = AnnotatedString(
    text,
    listOf(Range(spanStyle, 0, text.length)),
    if (paragraphStyle == null) listOf() else listOf(Range(paragraphStyle, 0, text.length))
)

/**
 * Create an AnnotatedString with a [paragraphStyle] that will apply to the whole text.
 *
 * @param paragraphStyle [ParagraphStyle] to be applied to whole text
 */
fun AnnotatedString(
    text: String,
    paragraphStyle: ParagraphStyle
): AnnotatedString = AnnotatedString(
    text,
    listOf(),
    listOf(Range(paragraphStyle, 0, text.length))
)

/**
 * Build a new AnnotatedString by populating newly created [AnnotatedString.Builder] provided
 * by [builder].
 *
 * @sample androidx.compose.ui.text.samples.AnnotatedStringBuilderLambdaSample
 *
 * @param builder lambda to modify [AnnotatedString.Builder]
 */
inline fun buildAnnotatedString(builder: (Builder).() -> Unit): AnnotatedString =
    Builder().apply(builder).toAnnotatedString()

/**
 * Helper function that checks if the range [baseStart, baseEnd) contains the range
 * [targetStart, targetEnd).
 *
 * @return true if [baseStart, baseEnd) contains [targetStart, targetEnd), vice versa.
 * When [baseStart]==[baseEnd] it return true iff [targetStart]==[targetEnd]==[baseStart].
 */
internal fun contains(baseStart: Int, baseEnd: Int, targetStart: Int, targetEnd: Int) =
    (baseStart <= targetStart && targetEnd <= baseEnd) &&
        (baseEnd != targetEnd || (targetStart == targetEnd) == (baseStart == baseEnd))

/**
 * Helper function that checks if the range [lStart, lEnd) intersects with the range
 * [rStart, rEnd).
 *
 * @return [lStart, lEnd) intersects with range [rStart, rEnd), vice versa.
 */
internal fun intersect(lStart: Int, lEnd: Int, rStart: Int, rEnd: Int) =
    maxOf(lStart, rStart) < minOf(lEnd, rEnd) ||
        contains(lStart, lEnd, rStart, rEnd) || contains(rStart, rEnd, lStart, lEnd)

private val EmptyAnnotatedString: AnnotatedString = AnnotatedString("")

/**
 * Returns an AnnotatedString with empty text and no annotations.
 */
internal fun emptyAnnotatedString() = EmptyAnnotatedString