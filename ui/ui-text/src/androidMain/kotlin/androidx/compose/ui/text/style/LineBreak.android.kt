/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.text.style

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.style.LineBreak.Strategy
import androidx.compose.ui.text.style.LineBreak.Strictness
import androidx.compose.ui.text.style.LineBreak.WordBreak

// TODO(b/246340708): Remove @sample LineBreakSample from the actual class
/**
 * When soft wrap is enabled and the width of the text exceeds the width of its container,
 * line breaks are inserted in the text to split it over multiple lines.
 *
 * There are a number of parameters that affect how the line breaks are inserted.
 * For example, the breaking algorithm can be changed to one with improved readability
 * at the cost of speed.
 * Another example is the strictness, which in some languages determines which symbols can appear
 * at the start of a line.
 *
 * This represents a configuration for line breaking on Android, describing [Strategy], [Strictness],
 * and [WordBreak].
 *
 * @sample androidx.compose.ui.text.samples.LineBreakSample
 * @sample androidx.compose.ui.text.samples.AndroidLineBreakSample
 */
@Immutable
@JvmInline
actual value class LineBreak private constructor(
    internal val mask: Int
) {

    /**
     * This represents a configuration for line breaking on Android, describing [Strategy],
     * [Strictness], and [WordBreak].
     *
     * @param strategy defines the algorithm that inserts line breaks
     * @param strictness defines the line breaking rules
     * @param wordBreak defines how words are broken
     */
    constructor(
        strategy: Strategy,
        strictness: Strictness,
        wordBreak: WordBreak
    ) : this(packBytes(
        strategy.value,
        strictness.value,
        wordBreak.value
    ))

    val strategy: Strategy
        get() = Strategy(unpackByte1(mask))

    val strictness: Strictness
        get() = Strictness(unpackByte2(mask))

    val wordBreak: WordBreak
        get() = WordBreak(unpackByte3(mask))

    fun copy(
        strategy: Strategy = this.strategy,
        strictness: Strictness = this.strictness,
        wordBreak: WordBreak = this.wordBreak
    ): LineBreak = LineBreak(
        strategy = strategy,
        strictness = strictness,
        wordBreak = wordBreak
    )

    override fun toString(): String =
        "LineBreak(strategy=$strategy, strictness=$strictness, wordBreak=$wordBreak)"

    actual companion object {
        /**
         * The greedy, fast line breaking algorithm. Ideal for text that updates often,
         * such as a text editor, as the text will reflow minimally.
         *
         * <pre>
         * +---------+
         * | This is |
         * | an      |
         * | example |
         * | text.   |
         * | 今日は自  |
         * | 由が丘で  |
         * | 焼き鳥を  |
         * | 食べま   |
         * | す。     |
         * +---------+
         * </pre>
         */
        actual val Simple: LineBreak = LineBreak(
            strategy = Strategy.Simple,
            strictness = Strictness.Normal,
            wordBreak = WordBreak.Default
        )

        /**
         * Balanced line lengths, hyphenation, and phrase-based breaking.
         * Suitable for short text such as titles or narrow newspaper columns.
         *
         * <pre>
         * +---------+
         * | This    |
         * | is an   |
         * | example |
         * | text.   |
         * | 今日は   |
         * | 自由が丘  |
         * | で焼き鳥  |
         * | を食べ   |
         * | ます。   |
         * +---------+
         * </pre>
         */
        actual val Heading: LineBreak = LineBreak(
            strategy = Strategy.Balanced,
            strictness = Strictness.Loose,
            wordBreak = WordBreak.Phrase
        )

        /**
         * Slower, higher quality line breaking for improved readability.
         * Suitable for larger amounts of text.
         *
         * <pre>
         * +---------+
         * | This    |
         * | is an   |
         * | example |
         * | text.   |
         * | 今日は自  |
         * | 由が丘で  |
         * | 焼き鳥を  |
         * | 食べま   |
         * | す。     |
         * +---------+
         * </pre>
         */
        actual val Paragraph: LineBreak = LineBreak(
            strategy = Strategy.HighQuality,
            strictness = Strictness.Strict,
            wordBreak = WordBreak.Default
        )
    }

    /**
     * The strategy used for line breaking.
     */
    @JvmInline
    value class Strategy internal constructor(internal val value: Int) {
        companion object {
            /**
             * Basic, fast break strategy. Hyphenation, if enabled, is done only for words
             * that don't fit on an entire line by themselves.
             *
             * <pre>
             * +---------+
             * | This is |
             * | an      |
             * | example |
             * | text.   |
             * +---------+
             * </pre>
             */
            val Simple: Strategy = Strategy(1)

            /**
             * Does whole paragraph optimization for more readable text,
             * including hyphenation if enabled.
             *
             * <pre>
             * +---------+
             * | This    |
             * | is an   |
             * | example |
             * | text.   |
             * +---------+
             * </pre>
             */
            val HighQuality: Strategy = Strategy(2)

            /**
             * Attempts to balance the line lengths of the text, also applying automatic
             * hyphenation if enabled. Suitable for small screens.
             *
             * <pre>
             * +-----------------------+
             * | This is an            |
             * | example text.         |
             * +-----------------------+
             * </pre>
             */
            val Balanced: Strategy = Strategy(3)
        }

        override fun toString(): String = when (this) {
            Simple -> "Strategy.Simple"
            HighQuality -> "Strategy.HighQuality"
            Balanced -> "Strategy.Balanced"
            else -> "Invalid"
        }
    }

    /**
     * Describes the strictness of line breaking, determining before which characters
     * line breaks can be inserted. It is useful when working with CJK scripts.
     */
    @JvmInline
    value class Strictness internal constructor(internal val value: Int) {
        companion object {
            /**
             * Default breaking rules for the locale, which may correspond to [Normal] or [Strict].
             */
            val Default: Strictness = Strictness(1)

            /**
             * The least restrictive rules, suitable for short lines.
             *
             * For example, in Japanese it allows breaking before iteration marks, such as 々, 〻.
             */
            val Loose: Strictness = Strictness(2)

            /**
             * The most common rules for line breaking.
             *
             * For example, in Japanese it allows breaking before characters like
             * small hiragana (ぁ), small katakana (ァ), halfwidth variants (ｧ).
             */
            val Normal: Strictness = Strictness(3)

            /**
             * The most stringent rules for line breaking.
             *
             * For example, in Japanese it does not allow breaking before characters like
             * small hiragana (ぁ), small katakana (ァ), halfwidth variants (ｧ).
             */
            val Strict: Strictness = Strictness(4)
        }

        override fun toString(): String = when (this) {
            Default -> "Strictness.None"
            Loose -> "Strictness.Loose"
            Normal -> "Strictness.Normal"
            Strict -> "Strictness.Strict"
            else -> "Invalid"
        }
    }

    /**
     * Describes how line breaks should be inserted within words.
     */
    @JvmInline
    value class WordBreak internal constructor(internal val value: Int) {
        companion object {
            /**
             * Default word breaking rules for the locale.
             * In latin scripts this means inserting line breaks between words,
             * while in languages that don't use whitespace (e.g. Japanese) the line can break
             * between characters.
             *
             * <pre>
             * +---------+
             * | This is |
             * | an      |
             * | example |
             * | text.   |
             * | 今日は自  |
             * | 由が丘で  |
             * | 焼き鳥を  |
             * | 食べま   |
             * | す。     |
             * +---------+
             * </pre>
             */
            val Default: WordBreak = WordBreak(1)

            /**
             * Line breaking is based on phrases.
             * In languages that don't use whitespace (e.g. Japanese), line breaks are not inserted
             * between characters that are part of the same phrase unit.
             * This is ideal for short text such as titles and UI labels.
             *
             * <pre>
             * +---------+
             * | This    |
             * | is an   |
             * | example |
             * | text.   |
             * | 今日は   |
             * | 自由が丘  |
             * | で焼き鳥  |
             * | を食べ   |
             * | ます。   |
             * +---------+
             * </pre>
             */
            val Phrase: WordBreak = WordBreak(2)
        }

        override fun toString(): String = when (this) {
            Default -> "WordBreak.None"
            Phrase -> "WordBreak.Phrase"
            else -> "Invalid"
        }
    }
}

/**
 * Packs 3 bytes represented as Integers into a single Integer.
 *
 * A byte can represent any value between 0 and 256.
 *
 * Only the 8 least significant bits of any given value are packed into the returned Integer.
 *
 */
private fun packBytes(i1: Int, i2: Int, i3: Int): Int {
    return i1 or (i2 shl 8) or (i3 shl 16)
}

private fun unpackByte1(mask: Int) = 0x000000FF and mask

private fun unpackByte2(mask: Int) = 0x000000FF and (mask shr 8)

private fun unpackByte3(mask: Int) = 0x000000FF and (mask shr 16)