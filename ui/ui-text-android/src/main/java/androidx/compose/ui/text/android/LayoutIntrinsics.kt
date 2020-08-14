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

package androidx.compose.ui.text.android

import android.text.BoringLayout
import android.text.Layout
import android.text.TextPaint
import java.text.BreakIterator
import java.util.PriorityQueue

/**
 * Computes and caches the text layout intrinsic values such as min/max width.
 *
 * @suppress
 */
@InternalPlatformTextApi
class LayoutIntrinsics(
    charSequence: CharSequence,
    textPaint: TextPaint,
    @LayoutCompat.TextDirection textDirectionHeuristic: Int
) {
    /**
     * Compute Android platform BoringLayout metrics. A null value means the provided CharSequence
     * cannot be laid out using a BoringLayout.
     */
    val boringMetrics: BoringLayout.Metrics? by lazy {
        val frameworkTextDir = getTextDirectionHeuristic(textDirectionHeuristic)
        BoringLayoutFactory.measure(charSequence, textPaint, frameworkTextDir)
    }

    /**
     * Calculate minimum intrinsic width of the CharSequence.
     *
     * @see androidx.compose.ui.text.android.minIntrinsicWidth
     */
    val minIntrinsicWidth: Float by lazy {
        minIntrinsicWidth(charSequence, textPaint)
    }

    /**
     * Calculate maximum intrinsic width for the CharSequence. Maximum intrinsic width is the width
     * of text where no soft line breaks are applied.
     */
    val maxIntrinsicWidth: Float by lazy {
        boringMetrics?.width?.toFloat()
            ?: Layout.getDesiredWidth(charSequence, 0, charSequence.length, textPaint)
    }
}

/**
 * Returns the word with the longest length. To calculate it in a performant way, it applies a heuristics where
 *  - it first finds a set of words with the longest length
 *  - finds the word with maximum width in that set
 */
internal fun minIntrinsicWidth(text: CharSequence, paint: TextPaint): Float {
    val iterator = BreakIterator.getLineInstance(paint.textLocale)
    iterator.text = CharSequenceCharacterIterator(text, 0, text.length)

    // 10 is just a random number that limits the size of the candidate list
    val heapSize = 10
    // min heap that will hold [heapSize] many words with max length
    val longestWordCandidates = PriorityQueue<Pair<Int, Int>>(
        heapSize,
        Comparator<Pair<Int, Int>> { left, right ->
            (left.second - left.first) - (right.second - right.first)
        }
    )

    var start = 0
    var end = iterator.next()
    while (end != BreakIterator.DONE) {
        if (longestWordCandidates.size < heapSize) {
            longestWordCandidates.add(Pair(start, end))
        } else {
            longestWordCandidates.peek()?.let { minPair ->
                if ((minPair.second - minPair.first) < (end - start)) {
                    longestWordCandidates.poll()
                    longestWordCandidates.add(Pair(start, end))
                }
            }
        }

        start = end
        end = iterator.next()
    }

    return longestWordCandidates.map { pair ->
        Layout.getDesiredWidth(text, pair.first, pair.second, paint)
    }.maxOrNull() ?: 0f
}
