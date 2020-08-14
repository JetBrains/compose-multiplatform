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

package androidx.compose.material.icons.generator.vector

import kotlin.math.min

/**
 * Trimmed down copy of PathParser that doesn't handle interacting with Paths, and only is
 * responsible for parsing path strings.
 */
object PathParser {
    /**
     * Parses the path string to create a collection of PathNode instances with their corresponding
     * arguments
     * throws an IllegalArgumentException or NumberFormatException if the parameters are invalid
     */
    fun parsePathString(pathData: String): List<PathNode> {
        val nodes = mutableListOf<PathNode>()

        fun addNode(cmd: Char, args: FloatArray) {
            nodes.addAll(cmd.toPathNodes(args))
        }

        var start = 0
        var end = 1
        while (end < pathData.length) {
            end = nextStart(pathData, end)
            val s = pathData.substring(start, end).trim { it <= ' ' }
            if (s.isNotEmpty()) {
                val args = getFloats(s)
                addNode(s[0], args)
            }

            start = end
            end++
        }
        if (end - start == 1 && start < pathData.length) {
            addNode(pathData[start], FloatArray(0))
        }

        return nodes
    }

    private fun nextStart(s: String, end: Int): Int {
        var index = end
        var c: Char

        while (index < s.length) {
            c = s[index]
            // Note that 'e' or 'E' are not valid path commands, but could be
            // used for floating point numbers' scientific notation.
            // Therefore, when searching for next command, we should ignore 'e'
            // and 'E'.
            if (((c - 'A') * (c - 'Z') <= 0 || (c - 'a') * (c - 'z') <= 0) &&
                c != 'e' && c != 'E'
            ) {
                return index
            }
            index++
        }
        return index
    }

    @Throws(NumberFormatException::class)
    private fun getFloats(s: String): FloatArray {
        if (s[0] == 'z' || s[0] == 'Z') {
            return FloatArray(0)
        }
        val results = FloatArray(s.length)
        var count = 0
        var startPosition = 1
        var endPosition: Int

        val result =
            ExtractFloatResult()
        val totalLength = s.length

        // The startPosition should always be the first character of the
        // current number, and endPosition is the character after the current
        // number.
        while (startPosition < totalLength) {
            extract(s, startPosition, result)
            endPosition = result.endPosition

            if (startPosition < endPosition) {
                results[count++] = java.lang.Float.parseFloat(
                    s.substring(startPosition, endPosition)
                )
            }

            startPosition = if (result.endWithNegativeOrDot) {
                // Keep the '-' or '.' sign with next number.
                endPosition
            } else {
                endPosition + 1
            }
        }
        return copyOfRange(results, 0, count)
    }

    private fun copyOfRange(original: FloatArray, start: Int, end: Int): FloatArray {
        if (start > end) {
            throw IllegalArgumentException()
        }
        val originalLength = original.size
        if (start < 0 || start > originalLength) {
            throw ArrayIndexOutOfBoundsException()
        }
        val resultLength = end - start
        val copyLength = min(resultLength, originalLength - start)
        val result = FloatArray(resultLength)
        original.copyInto(result, 0, start, start + copyLength)
        return result
    }

    private fun extract(s: String, start: Int, result: ExtractFloatResult) {
        // Now looking for ' ', ',', '.' or '-' from the start.
        var currentIndex = start
        var foundSeparator = false
        result.endWithNegativeOrDot = false
        var secondDot = false
        var isExponential = false
        while (currentIndex < s.length) {
            val isPrevExponential = isExponential
            isExponential = false
            when (s[currentIndex]) {
                ' ', ',' -> foundSeparator = true
                '-' ->
                    // The negative sign following a 'e' or 'E' is not a separator.
                    if (currentIndex != start && !isPrevExponential) {
                        foundSeparator = true
                        result.endWithNegativeOrDot = true
                    }
                '.' ->
                    if (!secondDot) {
                        secondDot = true
                    } else {
                        // This is the second dot, and it is considered as a separator.
                        foundSeparator = true
                        result.endWithNegativeOrDot = true
                    }
                'e', 'E' -> isExponential = true
            }
            if (foundSeparator) {
                break
            }
            currentIndex++
        }
        // When there is nothing found, then we put the end position to the end
        // of the string.
        result.endPosition = currentIndex
    }

    private data class ExtractFloatResult(
        // We need to return the position of the next separator and whether the
        // next float starts with a '-' or a '.'.
        var endPosition: Int = 0,
        var endWithNegativeOrDot: Boolean = false
    )
}
