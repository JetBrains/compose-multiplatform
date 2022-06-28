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

import androidx.compose.foundation.text.selection.LocalSelectionRegistrar
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/**
 * Basic element that displays text and provides semantics / accessibility information.
 * Typically you will instead want to use [androidx.compose.material.Text], which is
 * a higher level Text element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 */
@OptIn(InternalFoundationTextApi::class)
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    // NOTE(text-perf-review): consider precomputing layout here by pushing text to a channel...
    // something like:
    // remember(text) { precomputeTextLayout(text) }
    require(maxLines > 0) { "maxLines should be greater than 0" }

    // selection registrar, if no SelectionContainer is added ambient value will be null
    val selectionRegistrar = LocalSelectionRegistrar.current
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current

    // The ID used to identify this CoreText. If this CoreText is removed from the composition
    // tree and then added back, this ID should stay the same.
    // Notice that we need to update selectable ID when the input text or selectionRegistrar has
    // been updated.
    // When text is updated, the selection on this CoreText becomes invalid. It can be treated
    // as a brand new CoreText.
    // When SelectionRegistrar is updated, CoreText have to request a new ID to avoid ID collision.

    // NOTE(text-perf-review): potential bug. selectableId is regenerated here whenever text
    // changes, but it is only saved in the initial creation of TextState.
    val selectableId = if (selectionRegistrar == null) {
        SelectionRegistrar.InvalidSelectableId
    } else {
        rememberSaveable(text, selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
            selectionRegistrar.nextSelectableId()
        }
    }

    val controller = remember {
        TextController(
            TextState(
                TextDelegate(
                    text = AnnotatedString(text),
                    style = style,
                    density = density,
                    softWrap = softWrap,
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = overflow,
                    maxLines = maxLines,
                ),
                selectableId
            )
        )
    }
    val state = controller.state
    if (!currentComposer.inserting) {
        controller.setTextDelegate(
            updateTextDelegate(
                current = state.textDelegate,
                text = text,
                style = style,
                density = density,
                softWrap = softWrap,
                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow,
                maxLines = maxLines,
            )
        )
    }
    state.onTextLayout = onTextLayout
    controller.update(selectionRegistrar)
    if (selectionRegistrar != null) {
        state.selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor
    }

    Layout(modifier.then(controller.modifiers), controller.measurePolicy)
}

/**
 * Basic element that displays text and provides semantics / accessibility information.
 * Typically you will instead want to use [androidx.compose.material.Text], which is
 * a higher level Text element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. If it is not null, then it must be greater than zero.
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's
 * used to insert composables into text layout. Check [InlineTextContent] for more information.
 */
@OptIn(InternalFoundationTextApi::class)
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) {
    require(maxLines > 0) { "maxLines should be greater than 0" }

    // selection registrar, if no SelectionContainer is added ambient value will be null
    val selectionRegistrar = LocalSelectionRegistrar.current
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor

    val (placeholders, inlineComposables) = resolveInlineContent(text, inlineContent)

    // The ID used to identify this CoreText. If this CoreText is removed from the composition
    // tree and then added back, this ID should stay the same.
    // Notice that we need to update selectable ID when the input text or selectionRegistrar has
    // been updated.
    // When text is updated, the selection on this CoreText becomes invalid. It can be treated
    // as a brand new CoreText.
    // When SelectionRegistrar is updated, CoreText have to request a new ID to avoid ID collision.

    // NOTE(text-perf-review): potential bug. selectableId is regenerated here whenever text
    // changes, but it is only saved in the initial creation of TextState.
    val selectableId = if (selectionRegistrar == null) {
        SelectionRegistrar.InvalidSelectableId
    } else {
        rememberSaveable(text, selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
            selectionRegistrar.nextSelectableId()
        }
    }

    val controller = remember {
        TextController(
            TextState(
                TextDelegate(
                    text = text,
                    style = style,
                    density = density,
                    softWrap = softWrap,
                    fontFamilyResolver = fontFamilyResolver,
                    overflow = overflow,
                    maxLines = maxLines,
                    placeholders = placeholders
                ),
                selectableId
            )
        )
    }
    val state = controller.state
    if (!currentComposer.inserting) {
        controller.setTextDelegate(
            updateTextDelegate(
                current = state.textDelegate,
                text = text,
                style = style,
                density = density,
                softWrap = softWrap,
                fontFamilyResolver = fontFamilyResolver,
                overflow = overflow,
                maxLines = maxLines,
                placeholders = placeholders,
            )
        )
    }
    state.onTextLayout = onTextLayout
    state.selectionBackgroundColor = selectionBackgroundColor

    controller.update(selectionRegistrar)

    Layout(
        content = if (inlineComposables.isEmpty()) {
            {}
        } else {
            { InlineChildren(text, inlineComposables) }
        },
        modifier = modifier.then(controller.modifiers),
        measurePolicy = controller.measurePolicy
    )
}

/**
 * A custom saver that won't save if no selection is active.
 */
private fun selectionIdSaver(selectionRegistrar: SelectionRegistrar?) = Saver<Long, Long>(
    save = { if (selectionRegistrar.hasSelection(it)) it else null },
    restore = { it }
)