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

import android.graphics.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.roundToInt

/**
 * This class contains the implementation of text segment iterators
 * for accessibility support.
 *
 * Note: We want to be able to iterator over [SemanticsProperties.ContentDescription] of any
 * component.
 */
internal class AccessibilityIterators {

    interface TextSegmentIterator {
        /**
         * Given the current position, returning the start and end of next element in an array.
         */
        fun following(current: Int): IntArray?
        /**
         * Given the current position, returning the start and end of previous element in an array.
         */
        fun preceding(current: Int): IntArray?
    }

    abstract class AbstractTextSegmentIterator : TextSegmentIterator {

        protected lateinit var text: String

        private val segment = IntArray(2)

        open fun initialize(text: String) {
            this.text = text
        }

        protected fun getRange(start: Int, end: Int): IntArray? {
            if (start < 0 || end < 0 || start == end) {
                return null
            }
            segment[0] = start
            segment[1] = end
            return segment
        }
    }

    open class CharacterTextSegmentIterator private constructor(locale: Locale) :
        AbstractTextSegmentIterator() {
        companion object {
            private var instance: CharacterTextSegmentIterator? = null
            fun getInstance(locale: Locale): CharacterTextSegmentIterator {
                if (instance == null) {
                    instance = CharacterTextSegmentIterator(locale)
                }
                return instance as CharacterTextSegmentIterator
            }
        }

        private lateinit var impl: BreakIterator

        init {
            onLocaleChanged(locale)
            // TODO(yingleiw): register callback for locale change
            // ViewRootImpl.addConfigCallback(this);
        }

        override fun initialize(text: String) {
            super.initialize(text)
            impl.setText(text)
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= textLength) {
                return null
            }
            var start = current
            if (start < 0) {
                start = 0
            }
            while (!impl.isBoundary(start)) {
                start = impl.following(start)
                if (start == BreakIterator.DONE) {
                    return null
                }
            }
            val end = impl.following(start)
            if (end == BreakIterator.DONE) {
                return null
            }
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            var end = current
            if (end > textLength) {
                end = textLength
            }
            while (!impl.isBoundary(end)) {
                end = impl.preceding(end)
                if (end == BreakIterator.DONE) {
                    return null
                }
            }
            val start = impl.preceding(end)
            if (start == BreakIterator.DONE) {
                return null
            }
            return getRange(start, end)
        }

        // TODO(yingleiw): callback for locale change
        /*
        @Override
        public void onConfigurationChanged(Configuration globalConfig) {
            final Locale locale = globalConfig.getLocales().get(0);
            if (locale == null) {
                return;
            }
            if (!mLocale.equals(locale)) {
                mLocale = locale;
                onLocaleChanged(locale);
            }
        }
        */

        private fun onLocaleChanged(locale: Locale) {
            impl = BreakIterator.getCharacterInstance(locale)
        }
    }

    class WordTextSegmentIterator private constructor(locale: Locale) :
        AbstractTextSegmentIterator() {
        companion object {
            private var instance: WordTextSegmentIterator? = null

            fun getInstance(locale: Locale): WordTextSegmentIterator {
                if (instance == null) {
                    instance = WordTextSegmentIterator(locale)
                }
                return instance as WordTextSegmentIterator
            }
        }

        private lateinit var impl: BreakIterator

        init {
            onLocaleChanged(locale)
            // TODO: register callback for locale change
            // ViewRootImpl.addConfigCallback(this);
        }

        override fun initialize(text: String) {
            super.initialize(text)
            impl.setText(text)
        }

        private fun onLocaleChanged(locale: Locale) {
            impl = BreakIterator.getWordInstance(locale)
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= text.length) {
                return null
            }
            var start = current
            if (start < 0) {
                start = 0
            }
            while (!isLetterOrDigit(start) && !isStartBoundary(start)) {
                start = impl.following(start)
                if (start == BreakIterator.DONE) {
                    return null
                }
            }
            val end = impl.following(start)
            if (end == BreakIterator.DONE || !isEndBoundary(end)) {
                return null
            }
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            var end = current
            if (end > textLength) {
                end = textLength
            }
            while (end > 0 && !isLetterOrDigit(end - 1) && !isEndBoundary(end)) {
                end = impl.preceding(end)
                if (end == BreakIterator.DONE) {
                    return null
                }
            }
            val start = impl.preceding(end)
            if (start == BreakIterator.DONE || !isStartBoundary(start)) {
                return null
            }
            return getRange(start, end)
        }

        private fun isStartBoundary(index: Int): Boolean {
            return isLetterOrDigit(index) &&
                (index == 0 || !isLetterOrDigit(index - 1))
        }

        private fun isEndBoundary(index: Int): Boolean {
            return (index > 0 && isLetterOrDigit(index - 1)) &&
                (index == text.length || !isLetterOrDigit(index))
        }

        private fun isLetterOrDigit(index: Int): Boolean {
            if (index >= 0 && index < text.length) {
                val codePoint = text.codePointAt(index)
                return Character.isLetterOrDigit(codePoint)
            }
            return false
        }
    }

    class ParagraphTextSegmentIterator private constructor() : AbstractTextSegmentIterator() {
        companion object {
            private var instance: ParagraphTextSegmentIterator? = null

            fun getInstance(): ParagraphTextSegmentIterator {
                if (instance == null) {
                    instance = ParagraphTextSegmentIterator()
                }
                return instance as ParagraphTextSegmentIterator
            }
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= textLength) {
                return null
            }
            var start = current
            if (start < 0) {
                start = 0
            }
            while (start < textLength && text[start] == '\n' &&
                !isStartBoundary(start)
            ) {
                start++
            }
            if (start >= textLength) {
                return null
            }
            var end = start + 1
            while (end < textLength && !isEndBoundary(end)) {
                end++
            }
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            var end = current
            if (end > textLength) {
                end = textLength
            }
            while (end > 0 && text[end - 1] == '\n' && !isEndBoundary(end)) {
                end--
            }
            if (end <= 0) {
                return null
            }
            var start = end - 1
            while (start > 0 && !isStartBoundary(start)) {
                start--
            }
            return getRange(start, end)
        }

        private fun isStartBoundary(index: Int): Boolean {
            return (
                text[index] != '\n' &&
                    (index == 0 || text[index - 1] == '\n')
                )
        }

        private fun isEndBoundary(index: Int): Boolean {
            return (
                index > 0 && text[index - 1] != '\n' &&
                    (index == text.length || text[index] == '\n')
                )
        }
    }

    class LineTextSegmentIterator private constructor() : AbstractTextSegmentIterator() {
        companion object {
            private var lineInstance: LineTextSegmentIterator? = null
            private val DirectionStart = ResolvedTextDirection.Rtl
            private val DirectionEnd = ResolvedTextDirection.Ltr

            fun getInstance(): LineTextSegmentIterator {
                if (lineInstance == null) {
                    lineInstance = LineTextSegmentIterator()
                }
                return lineInstance as LineTextSegmentIterator
            }
        }

        private lateinit var layoutResult: TextLayoutResult

        fun initialize(text: String, layoutResult: TextLayoutResult) {
            this.text = text
            this.layoutResult = layoutResult
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= text.length) {
                return null
            }
            val nextLine = if (current < 0) {
                layoutResult.getLineForOffset(0)
            } else {
                val currentLine = layoutResult.getLineForOffset(current)
                if (getLineEdgeIndex(currentLine, DirectionStart) == current) {
                    currentLine
                } else {
                    currentLine + 1
                }
            }
            if (nextLine >= layoutResult.lineCount) {
                return null
            }
            val start = getLineEdgeIndex(nextLine, DirectionStart)
            val end = getLineEdgeIndex(nextLine, DirectionEnd) + 1
            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            val previousLine = if (current > text.length) {
                layoutResult.getLineForOffset(text.length)
            } else {
                val currentLine = layoutResult.getLineForOffset(current)
                if (getLineEdgeIndex(currentLine, DirectionEnd) + 1 == current) {
                    currentLine
                } else {
                    currentLine - 1
                }
            }
            if (previousLine < 0) {
                return null
            }
            val start = getLineEdgeIndex(previousLine, DirectionStart)
            val end = getLineEdgeIndex(previousLine, DirectionEnd) + 1
            return getRange(start, end)
        }

        private fun getLineEdgeIndex(lineNumber: Int, direction: ResolvedTextDirection): Int {
            val lineStart = layoutResult.getLineStart(lineNumber)
            val paragraphDirection = layoutResult.getParagraphDirection(lineStart)
            return if (direction != paragraphDirection) {
                layoutResult.getLineStart(lineNumber)
            } else {
                layoutResult.getLineEnd(lineNumber) - 1
            }
        }
    }

    // TODO(b/27505408): A11y movement by granularity page not working in edittext.
    class PageTextSegmentIterator private constructor() : AbstractTextSegmentIterator() {
        companion object {
            private var pageInstance: PageTextSegmentIterator? = null
            private val DirectionStart = ResolvedTextDirection.Rtl
            private val DirectionEnd = ResolvedTextDirection.Ltr
            fun getInstance(): PageTextSegmentIterator {
                if (pageInstance == null) {
                    pageInstance = PageTextSegmentIterator()
                }
                return pageInstance as PageTextSegmentIterator
            }
        }

        private lateinit var layoutResult: TextLayoutResult
        private lateinit var node: SemanticsNode

        private var tempRect = Rect()

        fun initialize(text: String, layoutResult: TextLayoutResult, node: SemanticsNode) {
            this.text = text
            this.layoutResult = layoutResult
            this.node = node
        }

        override fun following(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current >= text.length) {
                return null
            }
            val pageHeight: Int
            try {
                pageHeight = node.boundsInRoot.height.roundToInt()
                // TODO(b/153198816): check whether we still get this exception when R is in.
            } catch (e: IllegalStateException) {
                return null
            }

            val start = 0.coerceAtLeast(current)

            val currentLine = layoutResult.getLineForOffset(start)
            val currentLineTop = layoutResult.getLineTop(currentLine)
            // TODO: Please help me translate the below where mView is the TextView
            //  final int pageHeight = mTempRect.height() - mView.getTotalPaddingTop()
            //                    - mView.getTotalPaddingBottom();
            val nextPageStartY = currentLineTop + pageHeight
            val lastLineTop = layoutResult.getLineTop(layoutResult.lineCount - 1)
            val currentPageEndLine = if (nextPageStartY < lastLineTop)
                layoutResult.getLineForVerticalPosition(nextPageStartY) - 1
            else layoutResult.lineCount - 1

            val end = getLineEdgeIndex(currentPageEndLine, DirectionEnd) + 1

            return getRange(start, end)
        }

        override fun preceding(current: Int): IntArray? {
            val textLength = text.length
            if (textLength <= 0) {
                return null
            }
            if (current <= 0) {
                return null
            }
            val pageHeight: Int
            try {
                pageHeight = node.boundsInRoot.height.roundToInt()
                // TODO(b/153198816): check whether we still get this exception when R is in.
            } catch (e: IllegalStateException) {
                return null
            }

            val end = text.length.coerceAtMost(current)

            val currentLine = layoutResult.getLineForOffset(end)
            val currentLineTop = layoutResult.getLineTop(currentLine)
            // TODO: It won't work for text with padding yet.
            //  Please help me translate the below where mView is the TextView
            //  final int pageHeight = mTempRect.height() - mView.getTotalPaddingTop()
            //                    - mView.getTotalPaddingBottom();
            val previousPageEndY = currentLineTop - pageHeight
            var currentPageStartLine = if (previousPageEndY > 0)
                layoutResult.getLineForVerticalPosition(previousPageEndY) else 0
            // If we're at the end of text, we're at the end of the current line rather than the
            // start of the next line, so we should move up one fewer lines than we would otherwise.
            if (end == text.length && (currentPageStartLine < currentLine)) {
                currentPageStartLine += 1
            }

            val start = getLineEdgeIndex(currentPageStartLine, DirectionStart)

            return getRange(start, end)
        }

        private fun getLineEdgeIndex(lineNumber: Int, direction: ResolvedTextDirection): Int {
            val lineStart = layoutResult.getLineStart(lineNumber)
            val paragraphDirection = layoutResult.getParagraphDirection(lineStart)
            return if (direction != paragraphDirection) {
                layoutResult.getLineStart(lineNumber)
            } else {
                layoutResult.getLineEnd(lineNumber) - 1
            }
        }
    }
}
