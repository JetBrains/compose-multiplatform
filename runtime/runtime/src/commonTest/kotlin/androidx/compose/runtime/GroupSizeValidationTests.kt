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

@file:Suppress("RemoveEmptyClassBody")

package androidx.compose.runtime

import androidx.compose.runtime.mock.CompositionTestScope
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.runtime.tooling.CompositionGroup
import androidx.compose.runtime.mock.View
import kotlin.jvm.JvmInline

import kotlin.test.Test
import kotlinx.test.IgnoreJsTarget


@IgnoreJsTarget
// TODO (o.k.): figure out. Can fail on js because we have some extra logic there which leads to
// more groups than these tests expect. The behaviour of the composition is still correct (?).
class GroupSizeValidationTests {

    @Test
    fun spacerLike() = compositionTest {
        slotExpect(
            name = "SpacerLike",
            noMoreGroupsThan = 6,
            noMoreSlotsThan = 9,
        ) {
            SpacerLike(Modifier)
        }
    }

    @Test
    fun columnLikeSize() = compositionTest {
        slotExpect(
            name = "ColumnLike",
            noMoreGroupsThan = 9,
            noMoreSlotsThan = 8,
        ) {
            ColumnLike { }
        }
    }

    @Test
    fun textLikeSize() = compositionTest {
        slotExpect(
            name = "TextLike",
            noMoreGroupsThan = 13,
            noMoreSlotsThan = 15
        ) {
            TextLike("")
        }
    }

    @Test
    fun checkboxLike() = compositionTest {
        slotExpect(
            name = "CheckboxLike",
            noMoreGroupsThan = 13,
            noMoreSlotsThan = 20
        ) {
            CheckboxLike(checked = false, onCheckedChange = { })
        }
    }
}

// The following are a sketch of how compose ui uses composition to produce some important
// composable functions. These are derived from the implementation as of Oct 2022.

// The slot usage should be validated against the actual usage in GroupSizeTests in the
// integration-tests periodically to avoid these skewing too far.

@Stable
private fun interface MeasurePolicy {
    fun measure()
}

private val LocalDensity = staticCompositionLocalOf { 0 }
private val LocalLayoutDirection = staticCompositionLocalOf { 0 }
private val LocalViewConfiguration = staticCompositionLocalOf { 0 }

private object ViewHelper {
    val Constructor = ::View
    val SetModifier: View.(Modifier) -> Unit = { attributes["modifier"] = it }
    val SetMeasurePolicy: View.(MeasurePolicy) -> Unit = { attributes["measurePolicy"] = it }
    val SetDensity: View.(Int) -> Unit = { attributes["density"] = it }
    val SetLayoutDirection: View.(Int) -> Unit = { attributes["layoutDirection"] = it }
    val SetViewConfiguration: View.(Int) -> Unit = { attributes["viewConfiguration"] = it }
}

@Composable
private inline fun LayoutLike(
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    measurePolicy: MeasurePolicy
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    ReusableComposeNode<View, Applier<Any>>(
        factory = ViewHelper.Constructor,
        update = {
            set(modifier, ViewHelper.SetModifier)
            set(measurePolicy, ViewHelper.SetMeasurePolicy)
            set(density, ViewHelper.SetDensity)
            set(layoutDirection, ViewHelper.SetLayoutDirection)
            set(viewConfiguration, ViewHelper.SetViewConfiguration)
        },
        content = content
    )
}

@Composable
@NonRestartableComposable
private fun LayoutLike(modifier: Modifier, measurePolicy: MeasurePolicy) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    ReusableComposeNode<View, Applier<Any>>(
        factory = ViewHelper.Constructor,
        update = {
            set(modifier, ViewHelper.SetModifier)
            set(measurePolicy, ViewHelper.SetMeasurePolicy)
            set(density, ViewHelper.SetDensity)
            set(layoutDirection, ViewHelper.SetLayoutDirection)
            set(viewConfiguration, ViewHelper.SetViewConfiguration)
        }
    )
}

@Stable
private interface Modifier {
    companion object : Modifier
}

@Immutable
private object Arrangement {
    @Stable
    interface Vertical

    @Stable
    val Top = object : Vertical { }
}

@Immutable
private object Alignment {
    @Stable
    interface Horizontal

    @Stable
    val Start = object : Horizontal { }
}

private object SpacerMeasurePolicy : MeasurePolicy {
    override fun measure() { }
}

@Composable
private fun SpacerLike(modifier: Modifier) {
    LayoutLike(measurePolicy = SpacerMeasurePolicy, modifier = modifier)
}

@Immutable
private interface ColumnScope

private object ColumnScopeInstance : ColumnScope

// A stable version of Column used for group size tests
@Composable
private inline fun ColumnLike(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val measurePolicy =
        columnMeasurePolicy(verticalArrangement, horizontalAlignment)
    LayoutLike(
        content = { ColumnScopeInstance.content() },
        measurePolicy = measurePolicy,
        modifier = modifier
    )
}

private object DefaultColumnRowMeasurePolicy : MeasurePolicy {
    override fun measure() { }
}

@Composable private fun columnMeasurePolicy(
    verticalArrangement: Arrangement.Vertical,
    horizontalAlignment: Alignment.Horizontal
) = if (verticalArrangement == Arrangement.Top && horizontalAlignment == Alignment.Start) {
    DefaultColumnRowMeasurePolicy
} else {
    remember(verticalArrangement, horizontalAlignment) {
        DefaultColumnRowMeasurePolicy
    }
}

@Immutable
@JvmInline
private value class Color(val value: ULong) {

    @Stable
    @Suppress("UNUSED_PARAMETER")
    fun copy(
        alpha: Float = 0f,
        red: Float = 0f,
        green: Float = 0f,
        blue: Float = 0f
    ): Color = this

    companion object {
        @Stable
        val Unspecified = Color(0u)
    }
}
private val Color.isSpecified: Boolean get() = this != Color.Unspecified

private inline fun Color.takeOrElse(block: () -> Color): Color = if (isSpecified) this else block()

@Immutable
@JvmInline
private value class TextUnit(val packedValue: Long) {
    companion object {
        @Stable
        val Unspecified = TextUnit(0)
    }
}

@JvmInline
value class FontStyle(val value: Int)

@Immutable
@Suppress("unused")
private class FontWeight(val weight: Int)

@Immutable
@Suppress("UNUSED_PARAMETER")
private sealed class FontFamily(canLoadSynchronously: Boolean) {
    @Stable
    interface Resolver {
        companion object {
            val Default = object : Resolver { }
        }
    }
}

private val LocalFontFamilyResolver = staticCompositionLocalOf { FontFamily.Resolver.Default }

@Immutable
@Suppress("unused")
private class TextDecoration(val mask: Int)

@JvmInline
@Suppress("unused")
private value class TextAlign(val value: Int)

@JvmInline
private value class TextOverflow(val value: Int) {
    companion object {
        @Stable
        val Clip = TextOverflow(1)
    }
}

private class TextLayoutResult

@Immutable
@Suppress("unused")
private class TextStyle(
    val color: Color = Color.Unspecified,
    val fontSize: TextUnit = TextUnit.Unspecified,
    val fontWeight: FontWeight? = null,
    val textAlign: TextAlign? = null,
    val lineHeight: TextUnit = TextUnit.Unspecified,
    val fontFamily: FontFamily? = null,
    val textDecoration: TextDecoration? = null,
    val fontStyle: FontStyle? = null,
    val letterSpacing: TextUnit = TextUnit.Unspecified
) {
    @Stable
    @Suppress("UNUSED_PARAMETER")
    fun merge(other: TextStyle? = null) = this

    companion object {
        val Default = TextStyle()
    }
}

private interface SelectionRegistrar {
    fun nextSelectableId(): Long

    companion object {
        const val InvalidSelectableId = 0L
    }
}

private object DefaultSelectionRegister : SelectionRegistrar {
    override fun nextSelectableId() = 0L
}

private val DefaultTextStyle = TextStyle()
private val LocalTextStyle = staticCompositionLocalOf { DefaultTextStyle }
private val LocalContentColor = staticCompositionLocalOf { Color.Unspecified }
private val LocalContentAlpha = staticCompositionLocalOf { 1f }
private val LocalSelectionRegistrar = staticCompositionLocalOf<SelectionRegistrar?> {
    DefaultSelectionRegister
}

@Composable
private fun TextLike(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = LocalTextStyle.current
) {

    val textColor = color.takeOrElse {
        style.color.takeOrElse {
            LocalContentColor.current.copy(alpha = LocalContentAlpha.current)
        }
    }

    val mergedStyle = style.merge(
        TextStyle(
            color = textColor,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            lineHeight = lineHeight,
            fontFamily = fontFamily,
            textDecoration = textDecoration,
            fontStyle = fontStyle,
            letterSpacing = letterSpacing
        )
    )

    BasicTextLike(
        text = text,
        modifier = modifier,
        style = mergedStyle,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines
    )
}

private fun CompositionTestScope.slotExpect(
    name: String,
    noMoreGroupsThan: Int,
    noMoreSlotsThan: Int,
    content: @Composable () -> Unit
) {
    var compositionData: CompositionData? = null
    compose {
        compositionData = currentComposer.compositionData
        currentComposer.disableSourceInformation()
        Marker { content() }
    }

    val group = findMarkerGroup(compositionData!!)
    val receivedGroups = group.groupSize
    val receivedSlots = group.slotsSize

    if (receivedGroups > noMoreGroupsThan || receivedSlots > noMoreSlotsThan) {
        error("Expected $noMoreGroupsThan groups and $noMoreSlotsThan slots " +
            "but received $receivedGroups and $receivedSlots\n"
        )
    }
    if (receivedSlots < noMoreSlotsThan || receivedGroups < noMoreGroupsThan) {
        println(
            "WARNING: Improvement detected. Update test GroupSizeTests.$name to " +
                "$receivedGroups groups and $receivedSlots slots"
        )
    }
}

@Suppress("unused")
private class AnnotatedString(val text: String)

@Suppress("unused")
private class TextState(
    val textDelegate: TextDelegate,
    val selectionId: Long
) {
    var selectionBackgroundColor: Color = Color.Unspecified
    var onTextLayout: (TextLayoutResult) -> Unit = { }
}

@Suppress("unused")
private class TextDelegate(
    val text: AnnotatedString,
    val style: TextStyle,
    val maxLines: Int = Int.MAX_VALUE,
    val minLines: Int = 1,
    val softWrap: Boolean = true,
    val overflow: TextOverflow = TextOverflow.Clip,
    val density: Int,
    val fontFamilyResolver: FontFamily.Resolver
)

@Suppress("UNUSED_PARAMETER")
private fun updateTextDelegate(
    current: TextDelegate,
    text: String,
    style: TextStyle,
    density: Int,
    fontFamilyResolver: FontFamily.Resolver,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
): TextDelegate = current

@Suppress("UNUSED_PARAMETER")
private class TextController(val state: TextState) {
    val measurePolicy: MeasurePolicy = DefaultColumnRowMeasurePolicy
    fun setTextDelegate(updateTextDelegate: TextDelegate) { }
    fun update(selectionRegistrar: SelectionRegistrar?) { }
}

@Immutable
@Suppress("unused")
private class TextSelectionColors(
    val handleColor: Color,
    val backgroundColor: Color
) {
    companion object {
        val Default = TextSelectionColors(Color.Unspecified, Color.Unspecified)
    }
}

private val LocalTextSelectionColors = staticCompositionLocalOf { TextSelectionColors.Default }

@Composable
private fun BasicTextLike(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1
) {
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
        remember(text, selectionRegistrar) {
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
                    minLines = minLines,
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
                minLines = minLines,
            )
        )
    }
    state.onTextLayout = onTextLayout
    controller.update(selectionRegistrar)
    if (selectionRegistrar != null) {
        state.selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor
    }

    LayoutLike(modifier = modifier, measurePolicy = controller.measurePolicy)
}

// Unlike this above, this one is much more speculative as it removes the materialized modifiers
// and interactions and focus just on the wrapper pattern used by Checkbox

@Composable
private fun CheckboxLike(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    TriStateCheckboxLike(
        state = ToggleableState(checked),
        onClick = if (onCheckedChange != null) { { onCheckedChange(!checked) } } else null,
        enabled = enabled,
        modifier = modifier
    )
}

@Suppress("unused")
private enum class ToggleableState {
    On,
    Off,
    Indeterminate
}

private fun ToggleableState(value: Boolean) = if (value) ToggleableState.On else ToggleableState.Off

@Suppress("UNUSED_PARAMETER")
@Composable
private fun TriStateCheckboxLike(
    state: ToggleableState,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    CheckboxImplLike(
        enabled = enabled,
        value = state,
        modifier = modifier
    )
}

@Suppress("UNUSED_EXPRESSION")
@Composable
private fun CheckboxImplLike(
    enabled: Boolean,
    value: ToggleableState,
    modifier: Modifier
) {
    CanvasLike(modifier) {
        enabled
        value
    }
}

private interface DrawScope

@Suppress("UNUSED_PARAMETER")
@Composable
private fun CanvasLike(modifier: Modifier, onDraw: DrawScope.() -> Unit) =
    SpacerLike(modifier)

// Utility functions for the tests

@Composable
private inline fun Marker(content: @Composable () -> Unit) = content()

// left unused for debugging. This is useful for debugging differences in the slot table
@Suppress("unused")
private fun CompositionGroup.asString(): String {
    fun stringOf(group: CompositionGroup, indent: String): String =
        "$indent ${group.key} ${group.groupSize}:${group.slotsSize}:\n${
            group.compositionGroups.joinToString("") {
                stringOf(it, "$indent  ")
            }}"
    return stringOf(this, "")
}

private const val MarkerGroup = -340126117

private fun findMarkerGroup(compositionData: CompositionData): CompositionGroup {
    fun findGroup(groups: Iterable<CompositionGroup>, key: Int): CompositionGroup? {
        for (group in groups) {
            if (group.key == key) return group
            findGroup(group.compositionGroups, key)?.let { return it }
        }
        return null
    }

    return findGroup(compositionData.compositionGroups, MarkerGroup)
        ?.compositionGroups
        ?.firstOrNull()
        ?: error("Could not find marker:\n${compositionData.compositionGroups.first().asString()}")
}
