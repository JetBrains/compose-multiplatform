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

package androidx.compose.foundation.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextPainter
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.jvm.JvmStatic

// visible for testing
internal const val DefaultWidthCharCount = 10 // min width for TextField is 10 chars long
internal val EmptyTextReplacement = "H".repeat(DefaultWidthCharCount) // just a reference character.

/**
 * Computed the default width and height for TextField.
 *
 * The bounding box or x-advance of the empty text is empty, i.e. 0x0 box or 0px advance. However
 * this is not useful for TextField since text field want to reserve some amount of height for
 * accepting touch for starting text input. In Android, uses FontMetrics of the first font in the
 * fallback chain to compute this height, this is because custom font may have different
 * ascender/descender from the default font in Android.
 *
 * Until we have font metrics APIs, use the height of reference text as a workaround.
 */
internal fun computeSizeForDefaultText(
    style: TextStyle,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver,
    text: String = EmptyTextReplacement,
    maxLines: Int = 1
): IntSize {
    val paragraph = Paragraph(
        text = text,
        style = style,
        spanStyles = listOf(),
        maxLines = maxLines,
        ellipsis = false,
        density = density,
        fontFamilyResolver = fontFamilyResolver,
        constraints = Constraints()
    )
    return IntSize(paragraph.minIntrinsicWidth.toIntPx(), paragraph.height.toIntPx())
}

private fun Float.toIntPx(): Int = ceil(this).roundToInt()

@OptIn(InternalFoundationTextApi::class)
internal class TextFieldDelegate {
    companion object {
        /**
         * Process text layout with given constraint.
         *
         * @param textDelegate The text painter
         * @param constraints The layout constraints
         * @return the bounding box size(width and height) of the layout result
         */
        @JvmStatic
        internal fun layout(
            textDelegate: TextDelegate,
            constraints: Constraints,
            layoutDirection: LayoutDirection,
            prevResultText: TextLayoutResult? = null
        ): Triple<Int, Int, TextLayoutResult> {
            val layoutResult = textDelegate.layout(constraints, layoutDirection, prevResultText)
            return Triple(layoutResult.size.width, layoutResult.size.height, layoutResult)
        }

        /**
         * Draw the text content to the canvas
         *
         * @param canvas The target canvas.
         * @param value The editor state
         * @param offsetMapping The offset map
         * @param selectionPaint The selection paint
         */
        @JvmStatic
        internal fun draw(
            canvas: Canvas,
            value: TextFieldValue,
            offsetMapping: OffsetMapping,
            textLayoutResult: TextLayoutResult,
            selectionPaint: Paint
        ) {
            if (!value.selection.collapsed) {
                val start = offsetMapping.originalToTransformed(value.selection.min)
                val end = offsetMapping.originalToTransformed(value.selection.max)
                if (start != end) {
                    val selectionPath = textLayoutResult.getPathForRange(start, end)
                    canvas.drawPath(selectionPath, selectionPaint)
                }
            }
            TextPainter.paint(canvas, textLayoutResult)
        }

        /**
         * Called when edit operations are passed from TextInputService
         *
         * @param ops A list of edit operations.
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        private fun onEditCommand(
            ops: List<EditCommand>,
            editProcessor: EditProcessor,
            onValueChange: (TextFieldValue) -> Unit
        ) {
            onValueChange(editProcessor.apply(ops))
        }

        /**
         * Sets the cursor position. Should be called when TextField has focus.
         *
         * @param position The event position in composable coordinate.
         * @param textLayoutResult The text layout result proxy
         * @param editProcessor The edit processor
         * @param offsetMapping The offset map
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        internal fun setCursorOffset(
            position: Offset,
            textLayoutResult: TextLayoutResultProxy,
            editProcessor: EditProcessor,
            offsetMapping: OffsetMapping,
            onValueChange: (TextFieldValue) -> Unit
        ) {
            val offset = offsetMapping.transformedToOriginal(
                textLayoutResult.getOffsetForPosition(position)
            )
            onValueChange(editProcessor.toTextFieldValue().copy(selection = TextRange(offset)))
        }

        /**
         * Starts a new input connection.
         *
         * @param textInputService The text input service
         * @param value The editor state
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         * @param onImeActionPerformed The callback called when the editor action arrives.
         * @param imeOptions Keyboard configuration such as single line, auto correct etc.
         */
        @JvmStatic
        internal fun restartInput(
            textInputService: TextInputService,
            value: TextFieldValue,
            editProcessor: EditProcessor,
            imeOptions: ImeOptions,
            onValueChange: (TextFieldValue) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ): TextInputSession {
            return textInputService.startInput(
                value = value,
                imeOptions = imeOptions,
                onEditCommand = { onEditCommand(it, editProcessor, onValueChange) },
                onImeActionPerformed = onImeActionPerformed
            )
        }

        /**
         * Called when the composable gained input focus
         *
         * @param textInputService The text input service
         * @param value The editor state
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         * @param onImeActionPerformed The callback called when the editor action arrives.
         * @param imeOptions Keyboard configuration such as single line, auto correct etc.
         */
        @JvmStatic
        internal fun onFocus(
            textInputService: TextInputService,
            value: TextFieldValue,
            editProcessor: EditProcessor,
            imeOptions: ImeOptions,
            onValueChange: (TextFieldValue) -> Unit,
            onImeActionPerformed: (ImeAction) -> Unit
        ): TextInputSession {
            // The keyboard will automatically be shown when the new IME connection is started.
            return restartInput(
                textInputService = textInputService,
                value = value,
                editProcessor = editProcessor,
                imeOptions = imeOptions,
                onValueChange = onValueChange,
                onImeActionPerformed = onImeActionPerformed
            )
        }

        /**
         * Called when the composable loses input focus
         *
         * @param textInputSession The current input session.
         * @param editProcessor The edit processor
         * @param onValueChange The callback called when the new editor state arrives.
         */
        @JvmStatic
        internal fun onBlur(
            textInputSession: TextInputSession,
            editProcessor: EditProcessor,
            onValueChange: (TextFieldValue) -> Unit
        ) {
            onValueChange(editProcessor.toTextFieldValue().copy(composition = null))
            // Don't hide the keyboard when losing focus. If the target system needs that behavior,
            // it can be implemented in the PlatformTextInputService.
            textInputSession.dispose()
        }

        /**
         *  Apply the composition text decoration (undeline) to the transformed text.
         *
         *  @param compositionRange An input state
         *  @param transformed A transformed text
         *  @return The transformed text with composition decoration.
         *
         *  @suppress
         */
        fun applyCompositionDecoration(
            compositionRange: TextRange,
            transformed: TransformedText
        ): TransformedText =
            TransformedText(
                AnnotatedString.Builder(transformed.text).apply {
                    addStyle(
                        SpanStyle(textDecoration = TextDecoration.Underline),
                        transformed.offsetMapping.originalToTransformed(compositionRange.start),
                        transformed.offsetMapping.originalToTransformed(compositionRange.end)
                    )
                }.toAnnotatedString(),
                transformed.offsetMapping
            )
    }
}
