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

package androidx.compose.ui.text

import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.util.fastMap

// import java.util.SortedSet

/**
 * The core function of [AnnotatedString] transformation.
 *
 * @param transform the transformation method
 * @return newly allocated transformed AnnotatedString
 */
internal actual fun AnnotatedString.transform(
    transform: (String, Int, Int) -> String
): AnnotatedString = TODO("implement native AnnotatedString.transform")


/*
{
    val transitions = sortedSetOf(0, text.length)
    collectRangeTransitions(spanStyles, transitions)
    collectRangeTransitions(paragraphStyles, transitions)

    var resultStr = ""
    val offsetMap = mutableMapOf(0 to 0)
    transitions.windowed(size = 2) { (start, end) ->
        resultStr += transform(text, start, end)
        offsetMap.put(end, resultStr.length)
    }

    val newSpanStyles = spanStyles.fastMap {
        // The offset map must have mapping entry from all style start, end position.
        Range(it.item, offsetMap[it.start]!!, offsetMap[it.end]!!)
    }
    val newParaStyles = paragraphStyles.fastMap {
        Range(it.item, offsetMap[it.start]!!, offsetMap[it.end]!!)
    }
    val newAnnotations = annotations.fastMap {
        Range(it.item, offsetMap[it.start]!!, offsetMap[it.end]!!)
    }

    return AnnotatedString(
        text = resultStr,
        spanStyles = newSpanStyles,
        paragraphStyles = newParaStyles,
        annotations = newAnnotations
    )
}

/**
 * Adds all [AnnotatedString.Range] transition points
 *
 * @param ranges The list of AnnotatedString.Range
 * @param target The output list
 */
private fun <T> collectRangeTransitions(
    ranges: List<Range<T>>,
    target: SortedSet<Int>
) {
    ranges.fastFold(target) { acc, range ->
        acc.apply {
            add(range.start)
            add(range.end)
        }
    }
}
*/