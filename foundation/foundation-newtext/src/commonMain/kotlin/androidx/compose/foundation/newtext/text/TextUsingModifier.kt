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

package androidx.compose.foundation.newtext.text

import androidx.compose.foundation.newtext.text.copypasta.selection.LocalSelectionRegistrar
import androidx.compose.foundation.newtext.text.copypasta.selection.LocalTextSelectionColors
import androidx.compose.foundation.newtext.text.modifiers.StaticTextModifier
import androidx.compose.foundation.newtext.text.modifiers.StaticTextSelectionModifierController
import androidx.compose.foundation.newtext.text.modifiers.TextInlineContentLayoutDrawParams
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Rewrite of BasicText
 */
@ExperimentalTextApi
@Composable
fun TextUsingModifier(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
) {

    val selectionRegistrar = LocalSelectionRegistrar.current
    val selectionController = if (selectionRegistrar != null) {
        val backgroundSelectionColor = LocalTextSelectionColors.current.backgroundColor
        remember(selectionRegistrar, backgroundSelectionColor) {
            StaticTextSelectionModifierController(
                selectionRegistrar,
                backgroundSelectionColor
            )
        }
    } else {
        null
    }
    Layout(
        content = {},
        modifier = modifier.textModifier(
            AnnotatedString(text),
            style = style,
            onTextLayout = onTextLayout,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            fontFamilyResolver = LocalFontFamilyResolver.current,
            placeholders = null,
            onPlaceholderLayout = null,
            selectionController = selectionController
        ),
        TextMeasurePolicy { null }
    )
}

/**
 * Rewrite of BasicText
 */
@ExperimentalTextApi
@Composable
fun TextUsingModifier(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent>? = null,
) {
    val fontFamilyResolver = LocalFontFamilyResolver.current

    val selectionRegistrar = LocalSelectionRegistrar.current
    val selectionController = if (selectionRegistrar != null) {
        val backgroundSelectionColor = LocalTextSelectionColors.current.backgroundColor
        remember(selectionRegistrar, backgroundSelectionColor) {
            StaticTextSelectionModifierController(
                selectionRegistrar,
                backgroundSelectionColor
            )
        }
    } else {
        null
    }

    val (placeholders, inlineComposables) = text.resolveInlineContent(inlineContent)
    val measuredPlaceholderPositions = remember {
        mutableStateOf<List<Rect?>?>(null)
    }
    Layout(
        content = if (inlineComposables.isEmpty()) {
            {}
        } else {
            { InlineChildren(text, inlineComposables) }
        },
        modifier = modifier.textModifier(
            text = text,
            style = style,
            onTextLayout = onTextLayout,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = placeholders,
            onPlaceholderLayout = { measuredPlaceholderPositions.value = it },
            selectionController = selectionController
        ),
        measurePolicy = TextMeasurePolicy { measuredPlaceholderPositions.value }
    )
}

private class TextMeasurePolicy(
    private val placements: () -> List<Rect?>?
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val toPlace = placements()?.fastMapIndexedNotNull { index, rect ->
            // PlaceholderRect will be null if it's ellipsized. In that case, the corresponding
            // inline children won't be measured or placed.
            rect?.let {
                Pair(
                    measurables[index].measure(
                        Constraints(
                            maxWidth = floor(it.width).toInt(),
                            maxHeight = floor(it.height).toInt()
                        )
                    ),
                    IntOffset(it.left.roundToInt(), it.top.roundToInt())
                )
            }
        }
        return layout(
            constraints.maxWidth,
            constraints.maxHeight,
        ) {
            toPlace?.fastForEach { (placeable, position) ->
                placeable.place(position)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun Modifier.textModifier(
    text: AnnotatedString,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?,
    onPlaceholderLayout: ((List<Rect?>) -> Unit)?,
    selectionController: StaticTextSelectionModifierController?
): Modifier {
    val params = TextInlineContentLayoutDrawParams(
        text,
        style,
        fontFamilyResolver,
        onTextLayout,
        overflow,
        softWrap,
        maxLines,
        minLines,
        placeholders,
        onPlaceholderLayout,
        selectionController
    )
    val staticTextModifier = object : ModifierNodeElement<StaticTextModifier>(
        params,
        false,
        debugInspectorInfo {}
    ) {
        override fun create(): StaticTextModifier = StaticTextModifier(params)
        override fun update(node: StaticTextModifier): StaticTextModifier =
            node.also { it.update(params) }
    }
    val selectionModifier = selectionController?.modifier ?: Modifier
    return this then selectionModifier then staticTextModifier
}
