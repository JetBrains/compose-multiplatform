/*
 * Copyright 2023 The Android Open Source Project
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

import kotlin.jvm.JvmInline

/**
 * Automatic hyphenation configuration.
 *
 * Hyphenation is a dash-like punctuation mark used to join two-words into one or separate
 * syl-lab-les of a word.
 *
 * Automatic hyphenation is added between syllables at appropriate hyphenation points, following
 * language rules.
 *
 * However, user can override automatic break point selection, suggesting line break
 * opportunities (see Suggesting line break opportunities below).
 *
 * Suggesting line break opportunities:
 *
 * - <code>\u2010</code> ("hard" hyphen)
 * Indicates a visible line break opportunity. Even if the line is not actually broken at that
 * point, the hyphen is still rendered.
 *
 * - <code>\u00AD</code> ("soft" hyphen)
 * This character is not rendered visibly; instead, it marks a place where the word can be broken if
 * hyphenation is necessary.
 *
 * The default configuration for [Hyphens] = [Hyphens.None]
 *
 */
@JvmInline
value class Hyphens private constructor(internal val value: Int) {
    companion object {
        /**
         *  Lines will break with no hyphenation.
         *
         *  "Hard" hyphens will
         *  still be respected. However, no automatic hyphenation will be
         *  attempted. If a word must be broken due to being longer than a line, it will break at
         *  any character and will not attempt to break at a syllable boundary.
         *
         * <pre>
         * +---------+
         * | Experim |
         * | ental   |
         * +---------+
         * </pre>
         */
        val None = Hyphens(1)

        /**
         * The words will be automatically broken at appropriate hyphenation points.
         *
         * However, suggested line break opportunities (see Suggesting line break opportunities
         * above) will override automatic break point selection when present.
         *
         * <pre>
         * +---------+
         * | Experi- |
         * | mental  |
         * +---------+
         * </pre>
         */
        val Auto = Hyphens(2)
    }

    override fun toString() = when (this) {
        None -> "Hyphens.None"
        Auto -> "Hyphens.Auto"
        else -> "Invalid"
    }
}
