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

package androidx.compose.ui.text.input

import androidx.compose.ui.text.InternalTextApi

/**
 * Like [toCharArray] but copies the entire source string.
 * Workaround for compiler error when giving [toCharArray] above default parameters.
 */
private fun String.toCharArray(
    destination: CharArray,
    destinationOffset: Int
) = toCharArray(destination, destinationOffset, startIndex = 0, endIndex = this.length)

/**
 * Copies characters from this [String] into [destination].
 *
 * Platform-specific implementations should use native functions for performing this operation if
 * they exist, since they will likely be more efficient than copying each character individually.
 *
 * @param destination The [CharArray] to copy into.
 * @param destinationOffset The index in [destination] to start copying to.
 * @param startIndex The index in `this` of the first character to copy from (inclusive).
 * @param endIndex The index in `this` of the last character to copy from (exclusive).
 */
internal expect fun String.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
)

/**
 * The gap buffer implementation
 *
 * @param initBuffer An initial buffer. This class takes ownership of this object, so do not modify
 *                   array after passing to this constructor
 * @param initGapStart An initial inclusive gap start offset of the buffer
 * @param initGapEnd An initial exclusive gap end offset of the buffer
 */
private class GapBuffer(initBuffer: CharArray, initGapStart: Int, initGapEnd: Int) {

    /**
     * The current capacity of the buffer
     */
    private var capacity = initBuffer.size

    /**
     * The buffer
     */
    private var buffer = initBuffer

    /**
     * The inclusive start offset of the gap
     */
    private var gapStart = initGapStart

    /**
     * The exclusive end offset of the gap
     */
    private var gapEnd = initGapEnd

    /**
     * The length of the gap.
     */
    private fun gapLength(): Int = gapEnd - gapStart

    /**
     * [] operator for the character at the index.
     */
    operator fun get(index: Int): Char {
        if (index < gapStart) {
            return buffer[index]
        } else {
            return buffer[index - gapStart + gapEnd]
        }
    }

    /**
     * Check if the gap has a requested size, and allocate new buffer if there is enough space.
     */
    private fun makeSureAvailableSpace(requestSize: Int) {
        if (requestSize <= gapLength()) {
            return
        }

        // Allocating necessary memory space by doubling the array size.
        val necessarySpace = requestSize - gapLength()
        var newCapacity = capacity * 2
        while ((newCapacity - capacity) < necessarySpace) {
            newCapacity *= 2
        }

        val newBuffer = CharArray(newCapacity)
        buffer.copyInto(newBuffer, 0, 0, gapStart)
        val tailLength = capacity - gapEnd
        val newEnd = newCapacity - tailLength
        buffer.copyInto(newBuffer, newEnd, gapEnd, gapEnd + tailLength)

        buffer = newBuffer
        capacity = newCapacity
        gapEnd = newEnd
    }

    /**
     * Delete the given range of the text.
     */
    private fun delete(start: Int, end: Int) {
        if (start < gapStart && end <= gapStart) {
            // The remove happens in the head buffer. Copy the tail part of the head buffer to the
            // tail buffer.
            //
            // Example:
            // Input:
            //   buffer:     ABCDEFGHIJKLMNOPQ*************RSTUVWXYZ
            //   del region:     |-----|
            //
            // First, move the remaining part of the head buffer to the tail buffer.
            //   buffer:     ABCDEFGHIJKLMNOPQ*****KLKMNOPQRSTUVWXYZ
            //   move data:            ^^^^^^^ =>  ^^^^^^^^
            //
            // Then, delete the given range. (just updating gap positions)
            //   buffer:     ABCD******************KLKMNOPQRSTUVWXYZ
            //   del region:     |-----|
            //
            // Output:       ABCD******************KLKMNOPQRSTUVWXYZ
            val copyLen = gapStart - end
            buffer.copyInto(buffer, gapEnd - copyLen, end, gapStart)
            gapStart = start
            gapEnd -= copyLen
        } else if (start < gapStart && end >= gapStart) {
            // The remove happens with accrossing the gap region. Just update the gap position
            //
            // Example:
            // Input:
            //   buffer:     ABCDEFGHIJKLMNOPQ************RSTUVWXYZ
            //   del region:             |-------------------|
            //
            // Output:       ABCDEFGHIJKL********************UVWXYZ
            gapEnd = end + gapLength()
            gapStart = start
        } else { // start > gapStart && end > gapStart
            // The remove happens in the tail buffer. Copy the head part of the tail buffer to the
            // head buffer.
            //
            // Example:
            // Input:
            //   buffer:     ABCDEFGHIJKL************MNOPQRSTUVWXYZ
            //   del region:                            |-----|
            //
            // First, move the remaining part in the tail buffer to the head buffer.
            //   buffer:     ABCDEFGHIJKLMNO*********MNOPQRSTUVWXYZ
            //   move dat:               ^^^    <=   ^^^
            //
            // Then, delete the given range. (just updating gap positions)
            //   buffer:     ABCDEFGHIJKLMNO******************VWXYZ
            //   del region:                            |-----|
            //
            // Output:       ABCDEFGHIJKLMNO******************VWXYZ
            val startInBuffer = start + gapLength()
            val endInBuffer = end + gapLength()
            val copyLen = startInBuffer - gapEnd
            buffer.copyInto(buffer, gapStart, gapEnd, startInBuffer)
            gapStart += copyLen
            gapEnd = endInBuffer
        }
    }

    /**
     * Replace the certain region of text with given text
     *
     * @param start an inclusive start offset for replacement.
     * @param end an exclusive end offset for replacement
     * @param text a text to replace
     */
    fun replace(start: Int, end: Int, text: String) {
        makeSureAvailableSpace(text.length - (end - start))

        delete(start, end)

        text.toCharArray(buffer, gapStart)
        gapStart += text.length
    }

    /**
     * Write the current text into outBuf.
     * @param builder The output string builder
     */
    fun append(builder: StringBuilder) {
        builder.append(buffer, 0, gapStart)
        builder.append(buffer, gapEnd, capacity - gapEnd)
    }

    /**
     * The lengh of this gap buffer.
     *
     * This doesn't include internal hidden gap length.
     */
    fun length() = capacity - gapLength()

    override fun toString(): String = StringBuilder().apply { append(this) }.toString()
}

/**
 * An editing buffer that uses Gap Buffer only around the cursor location.
 *
 * Different from the original gap buffer, this gap buffer doesn't convert all given text into
 * mutable buffer. Instead, this gap buffer converts cursor around text into mutable gap buffer
 * for saving construction time and memory space. If text modification outside of the gap buffer
 * is requested, this class flush the buffer and create new String, then start new gap buffer.
 *
 * @param text The initial text
 * @suppress
 */
@InternalTextApi // "Used by benchmarks"
class PartialGapBuffer(var text: String) {
    internal companion object {
        const val BUF_SIZE = 255
        const val SURROUNDING_SIZE = 64
        const val NOWHERE = -1
    }

    private var buffer: GapBuffer? = null
    private var bufStart = NOWHERE
    private var bufEnd = NOWHERE

    /**
     * The text length
     */
    val length: Int
        get() {
            val buffer = buffer ?: return text.length
            return text.length - (bufEnd - bufStart) + buffer.length()
        }

    /**
     * Replace the certain region of text with given text
     *
     * @param start an inclusive start offset for replacement.
     * @param end an exclusive end offset for replacement
     * @param text a text to replace
     */
    fun replace(start: Int, end: Int, text: String) {
        require(start <= end) {
            "start index must be less than or equal to end index: $start > $end"
        }
        require(start >= 0) {
            "start must be non-negative, but was $start"
        }

        val buffer = buffer
        if (buffer == null) { // First time to create gap buffer
            val charArray = CharArray(maxOf(BUF_SIZE, text.length + 2 * SURROUNDING_SIZE))

            // Convert surrounding text into buffer.
            val leftCopyCount = minOf(start, SURROUNDING_SIZE)
            val rightCopyCount = minOf(this.text.length - end, SURROUNDING_SIZE)

            // Copy left surrounding
            this.text.toCharArray(charArray, 0, start - leftCopyCount, start)

            // Copy right surrounding
            this.text.toCharArray(
                charArray,
                charArray.size - rightCopyCount,
                end,
                end + rightCopyCount
            )

            // Copy given text into buffer
            text.toCharArray(charArray, leftCopyCount)

            this.buffer = GapBuffer(
                charArray,
                initGapStart = leftCopyCount + text.length,
                initGapEnd = charArray.size - rightCopyCount
            )
            bufStart = start - leftCopyCount
            bufEnd = end + rightCopyCount
            return
        }

        // Convert user space offset into buffer space offset
        val bufferStart = start - bufStart
        val bufferEnd = end - bufStart
        if (bufferStart < 0 || bufferEnd > buffer.length()) {
            // Text modification outside of gap buffer has requested. Flush the buffer and try it
            // again.
            this.text = toString()
            this.buffer = null
            bufStart = NOWHERE
            bufEnd = NOWHERE
            return replace(start, end, text)
        }

        buffer.replace(bufferStart, bufferEnd, text)
    }

    /**
     * [] operator for the character at the index.
     */
    operator fun get(index: Int): Char {
        val buffer = buffer ?: return text[index]
        if (index < bufStart) {
            return text[index]
        }
        val gapBufLength = buffer.length()
        if (index < gapBufLength + bufStart) {
            return buffer[index - bufStart]
        }
        return text[index - (gapBufLength - bufEnd + bufStart)]
    }

    override fun toString(): String {
        val b = buffer ?: return text
        val sb = StringBuilder()
        sb.append(text, 0, bufStart)
        b.append(sb)
        sb.append(text, bufEnd, text.length)
        return sb.toString()
    }
}
