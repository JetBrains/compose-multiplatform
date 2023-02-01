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

package androidx.compose.material3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.SearchBarDefaults.InputFieldHeight
import androidx.compose.material3.tokens.FilledTextFieldTokens
import androidx.compose.material3.tokens.MotionTokens
import androidx.compose.material3.tokens.SearchBarTokens
import androidx.compose.material3.tokens.SearchViewTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.max
import kotlin.math.min

// TODO(260864875): Add material.io link and image when available
/**
 * Material Design search bar
 *
 * A search bar represents a floating search field that allows users to enter a keyword or phrase
 * and get relevant information. It can be used as a way to navigate through an app via search
 * queries.
 *
 * An active search bar expands into a search "view" and can be used to display dynamic suggestions.
 *
 * A [SearchBar] expands to occupy the entirety of its allowed size when active. For full-screen
 * behavior as specified by Material guidelines, parent layouts of the [SearchBar] must not pass
 * any [Constraints] that limit its size, and the host activity should set
 * `WindowCompat.setDecorFitsSystemWindows(window, false)`.
 *
 * If this expansion behavior is undesirable, for example on large tablet screens, [DockedSearchBar]
 * can be used instead.
 *
 * An example looks like:
 * @sample androidx.compose.material3.samples.SearchBarSample
 *
 * @param query the query text to be shown in the search bar's input field
 * @param onQueryChange the callback to be invoked when the input service updates the query. An
 * updated text comes as a parameter of the callback.
 * @param onSearch the callback to be invoked when the input service triggers the [ImeAction.Search]
 * action. The current [query] comes as a parameter of the callback.
 * @param active whether this search bar is active
 * @param onActiveChange the callback to be invoked when this search bar's active state is changed
 * @param modifier the [Modifier] to be applied to this search bar
 * @param enabled controls the enabled state of this search bar. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param placeholder the placeholder to be displayed when the search bar's [query] is empty.
 * @param leadingIcon the leading icon to be displayed at the beginning of the search bar container
 * @param trailingIcon the trailing icon to be displayed at the end of the search bar container
 * @param shape the shape of this search bar when it is not [active]. When [active], the shape will
 * always be [SearchBarDefaults.fullScreenShape].
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 * in different states. See [SearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 * translucent primary color overlay is applied on top of the container. A higher tonal elevation
 * value will result in a darker color in light theme and lighter color in dark theme. See also:
 * [Surface].
 * @param windowInsets the window insets that the search bar will respect
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this search bar. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this search bar in different states.
 * @param content the content of this search bar that will be displayed below the input field
 */
@ExperimentalMaterial3Api
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.inputFieldShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.Elevation,
    windowInsets: WindowInsets = SearchBarDefaults.windowInsets,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    val animationProgress: Float by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(
            durationMillis = AnimationDurationMillis,
            easing = MotionTokens.EasingLegacyCubicBezier,
        )
    )

    val defaultInputFieldShape = SearchBarDefaults.inputFieldShape
    val defaultFullScreenShape = SearchBarDefaults.fullScreenShape
    val animatedShape by remember {
        derivedStateOf {
            when {
                shape == defaultInputFieldShape -> {
                    // The shape can only be animated if it's the default spec value
                    val animatedRadius = SearchBarCornerRadius * (1 - animationProgress)
                    RoundedCornerShape(CornerSize(animatedRadius))
                }
                animationProgress == 1f -> defaultFullScreenShape
                else -> shape
            }
        }
    }

    // The main animation trickery is allowing the component to smoothly expand while keeping the
    // input field at the same relative location on screen. For this, Modifier.windowInsetsPadding
    // is not suitable. Instead, we convert the insets to a padding applied to the Surface, which
    // gradually becomes padding applied to the input field as the animation proceeds.
    val unconsumedInsets = remember { Ref<WindowInsets>() }
    val topPadding = SearchBarVerticalPadding +
        (unconsumedInsets.value ?: ZeroWindowInsets).asPaddingValues().calculateTopPadding()
    val animatedSurfaceTopPadding = lerp(topPadding, 0.dp, animationProgress)
    val animatedInputFieldPadding by remember {
        derivedStateOf {
            PaddingValues(
                top = topPadding * animationProgress,
                bottom = SearchBarVerticalPadding * animationProgress,
            )
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .zIndex(1f)
            .onConsumedWindowInsetsChanged { consumedInsets ->
                unconsumedInsets.value = windowInsets.exclude(consumedInsets)
            }
            .consumeWindowInsets(unconsumedInsets.value ?: ZeroWindowInsets),
        propagateMinConstraints = true
    ) {
        val height: Dp
        val width: Dp
        with(LocalDensity.current) {
            val startWidth = max(constraints.minWidth, SearchBarMinWidth.roundToPx())
                .coerceAtMost(min(constraints.maxWidth, SearchBarMaxWidth.roundToPx()))
                .toFloat()
            val startHeight = max(constraints.minHeight, InputFieldHeight.roundToPx())
                .coerceAtMost(constraints.maxHeight)
                .toFloat()
            val endWidth = constraints.maxWidth.toFloat()
            val endHeight = constraints.maxHeight.toFloat()

            height = lerp(startHeight, endHeight, animationProgress).toDp()
            width = lerp(startWidth, endWidth, animationProgress).toDp()
        }

        Surface(
            shape = animatedShape,
            color = colors.containerColor,
            contentColor = contentColorFor(colors.containerColor),
            tonalElevation = tonalElevation,
            modifier = Modifier
                .padding(top = animatedSurfaceTopPadding)
                .size(width = width, height = height)
        ) {
            Column {
                SearchBarInputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = onSearch,
                    active = active,
                    onActiveChange = onActiveChange,
                    modifier = Modifier.padding(animatedInputFieldPadding),
                    enabled = enabled,
                    placeholder = placeholder,
                    leadingIcon = leadingIcon,
                    trailingIcon = trailingIcon,
                    colors = colors.inputFieldColors,
                    interactionSource = interactionSource,
                )

                if (animationProgress > 0) {
                    Column(Modifier.alpha(animationProgress)) {
                        Divider(color = colors.dividerColor)
                        content()
                    }
                }
            }
        }
    }

    BackHandler(enabled = active) {
        onActiveChange(false)
    }
}

// TODO(260864875): Add material.io link and image when available
/**
 * Material Design search bar
 *
 * A search bar represents a floating search field that allows users to enter a keyword or phrase
 * and get relevant information. It can be used as a way to navigate through an app via search
 * queries.
 *
 * An active search bar expands into a search "view" and can be used to display dynamic suggestions.
 *
 * A [DockedSearchBar] displays search results in a bounded table below the input field. It is meant
 * to be an alternative to [SearchBar] when expanding to full-screen size is undesirable on large
 * screens such as tablets.
 *
 * An example looks like:
 * @sample androidx.compose.material3.samples.DockedSearchBarSample
 *
 * @param query the query text to be shown in the search bar's input field
 * @param onQueryChange the callback to be invoked when the input service updates the query. An
 * updated text comes as a parameter of the callback.
 * @param onSearch the callback to be invoked when the input service triggers the [ImeAction.Search]
 * action. The current [query] comes as a parameter of the callback.
 * @param active whether this search bar is active
 * @param onActiveChange the callback to be invoked when this search bar's active state is changed
 * @param modifier the [Modifier] to be applied to this search bar
 * @param enabled controls the enabled state of this search bar. When `false`, this component will
 * not respond to user input, and it will appear visually disabled and disabled to accessibility
 * services.
 * @param placeholder the placeholder to be displayed when the search bar's [query] is empty.
 * @param leadingIcon the leading icon to be displayed at the beginning of the search bar container
 * @param trailingIcon the trailing icon to be displayed at the end of the search bar container
 * @param shape the shape of this search bar
 * @param colors [SearchBarColors] that will be used to resolve the colors used for this search bar
 * in different states. See [SearchBarDefaults.colors].
 * @param tonalElevation when [SearchBarColors.containerColor] is [ColorScheme.surface], a
 * translucent primary color overlay is applied on top of the container. A higher tonal elevation
 * value will result in a darker color in light theme and lighter color in dark theme. See also:
 * [Surface].
 * @param interactionSource the [MutableInteractionSource] representing the stream of [Interaction]s
 * for this search bar. You can create and pass in your own `remember`ed instance to observe
 * [Interaction]s and customize the appearance / behavior of this search bar in different states.
 * @param content the content of this search bar that will be displayed below the input field
 */
@ExperimentalMaterial3Api
@Composable
fun DockedSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = SearchBarDefaults.dockedShape,
    colors: SearchBarColors = SearchBarDefaults.colors(),
    tonalElevation: Dp = SearchBarDefaults.Elevation,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = shape,
        color = colors.containerColor,
        contentColor = contentColorFor(colors.containerColor),
        tonalElevation = tonalElevation,
        modifier = modifier
            .zIndex(1f)
            .width(SearchBarMinWidth)
    ) {
        Column {
            SearchBarInputField(
                query = query,
                onQueryChange = onQueryChange,
                onSearch = onSearch,
                active = active,
                onActiveChange = onActiveChange,
                enabled = enabled,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                colors = colors.inputFieldColors,
                interactionSource = interactionSource,
            )

            AnimatedVisibility(
                visible = active,
                enter = DockedEnterTransition,
                exit = DockedExitTransition,
            ) {
                val screenHeight = LocalConfiguration.current.screenHeightDp.dp
                val maxHeight = remember(screenHeight) {
                    screenHeight * DockedActiveTableMaxHeightScreenRatio
                }
                val minHeight = remember(maxHeight) {
                    DockedActiveTableMinHeight.coerceAtMost(maxHeight)
                }

                Column(Modifier.heightIn(min = minHeight, max = maxHeight)) {
                    Divider(color = colors.dividerColor)
                    content()
                }
            }
        }
    }

    BackHandler(enabled = active) {
        onActiveChange(false)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    active: Boolean,
    onActiveChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = SearchBarDefaults.inputFieldColors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    val focusRequester = remember { FocusRequester() }
    val searchSemantics = getString(Strings.SearchBarSearch)
    val suggestionsAvailableSemantics = getString(Strings.SuggestionsAvailable)
    val textColor = LocalTextStyle.current.color.takeOrElse {
        colors.textColor(enabled, isError = false, interactionSource = interactionSource).value
    }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(InputFieldHeight)
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Must be PointerEventPass.Initial to observe events before the text field
                    // consumes them in the Main pass
                    awaitFirstDown(pass = PointerEventPass.Initial)
                    val upEvent = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                    if (upEvent != null) {
                        onActiveChange(true)
                    }
                }
            }
            .semantics {
                contentDescription = searchSemantics
                if (active) {
                    stateDescription = suggestionsAvailableSemantics
                }
                onClick {
                    onActiveChange(true)
                    focusRequester.requestFocus()
                    true
                }
            },
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(TextStyle(color = textColor)),
        cursorBrush = SolidColor(colors.cursorColor(isError = false).value),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        interactionSource = interactionSource,
        decorationBox = @Composable { innerTextField ->
            TextFieldDefaults.TextFieldDecorationBox(
                value = query,
                innerTextField = innerTextField,
                enabled = enabled,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interactionSource,
                placeholder = placeholder,
                leadingIcon = leadingIcon?.let { leading -> {
                    Box(Modifier.offset(x = SearchBarIconOffsetX)) { leading() }
                } },
                trailingIcon = trailingIcon?.let { trailing -> {
                    Box(Modifier.offset(x = -SearchBarIconOffsetX)) { trailing() }
                } },
                shape = SearchBarDefaults.inputFieldShape,
                colors = colors,
                contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(),
                container = {},
            )
        }
    )
}

/**
 * Defaults used in [SearchBar] and [DockedSearchBar].
 */
object SearchBarDefaults {
    /** Default elevation for a search bar. */
    val Elevation: Dp = SearchBarTokens.ContainerElevation

    /** Default height for a search bar's input field, or a search bar in the inactive state. */
    val InputFieldHeight: Dp = SearchBarTokens.ContainerHeight

    /** Default shape for a search bar's input field, or a search bar in the inactive state. */
    val inputFieldShape: Shape @Composable get() = SearchBarTokens.ContainerShape.toShape()

    /** Default shape for a [SearchBar] in the active state. */
    val fullScreenShape: Shape
        @Composable get() = SearchViewTokens.FullScreenContainerShape.toShape()

    /** Default shape for a [DockedSearchBar]. */
    val dockedShape: Shape @Composable get() = SearchViewTokens.DockedContainerShape.toShape()

    /** Default window insets for a [SearchBar]. */
    val windowInsets: WindowInsets @Composable get() = WindowInsets.statusBars

    /**
     * Creates a [SearchBarColors] that represents the different colors used in parts of the
     * search bar in different states.
     *
     * @param containerColor the container color of the search bar
     * @param dividerColor the color of the divider between the input field and the search results
     * @param inputFieldColors the colors of the input field
     */
    @ExperimentalMaterial3Api
    @Composable
    fun colors(
        containerColor: Color = SearchBarTokens.ContainerColor.toColor(),
        dividerColor: Color = SearchViewTokens.DividerColor.toColor(),
        inputFieldColors: TextFieldColors = inputFieldColors(),
    ): SearchBarColors = SearchBarColors(
        containerColor = containerColor,
        dividerColor = dividerColor,
        inputFieldColors = inputFieldColors,
    )

    /**
     * Creates a [TextFieldColors] that represents the different colors used in the search bar
     * input field in different states.
     *
     * Only a subset of the full list of [TextFieldColors] parameters are used in the input field.
     * All other parameters have no effect.
     *
     * @param focusedTextColor the color used for the input text of this input field when focused
     * @param unfocusedTextColor the color used for the input text of this input field when not
     * focused
     * @param disabledTextColor the color used for the input text of this input field when disabled
     * @param cursorColor the cursor color for this input field
     * @param selectionColors the colors used when the input text of this input field is selected
     * @param focusedLeadingIconColor the leading icon color for this input field when focused
     * @param unfocusedLeadingIconColor the leading icon color for this input field when not focused
     * @param disabledLeadingIconColor the leading icon color for this input field when disabled
     * @param focusedTrailingIconColor the trailing icon color for this input field when focused
     * @param unfocusedTrailingIconColor the trailing icon color for this input field when not
     * focused
     * @param disabledTrailingIconColor the trailing icon color for this input field when disabled
     * @param focusedPlaceholderColor the placeholder color for this input field when focused
     * @param unfocusedPlaceholderColor the placeholder color for this input field when not focused
     * @param disabledPlaceholderColor the placeholder color for this input field when disabled
     */
    @ExperimentalMaterial3Api
    @Composable
    fun inputFieldColors(
        focusedTextColor: Color = SearchBarTokens.InputTextColor.toColor(),
        unfocusedTextColor: Color = SearchBarTokens.InputTextColor.toColor(),
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        cursorColor: Color = FilledTextFieldTokens.CaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedLeadingIconColor: Color = SearchBarTokens.LeadingIconColor.toColor(),
        unfocusedLeadingIconColor: Color = SearchBarTokens.LeadingIconColor.toColor(),
        disabledLeadingIconColor: Color = FilledTextFieldTokens.DisabledLeadingIconColor
            .toColor().copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
        focusedTrailingIconColor: Color = SearchBarTokens.TrailingIconColor.toColor(),
        unfocusedTrailingIconColor: Color = SearchBarTokens.TrailingIconColor.toColor(),
        disabledTrailingIconColor: Color = FilledTextFieldTokens.DisabledTrailingIconColor
            .toColor().copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
        focusedPlaceholderColor: Color = SearchBarTokens.SupportingTextColor.toColor(),
        unfocusedPlaceholderColor: Color = SearchBarTokens.SupportingTextColor.toColor(),
        disabledPlaceholderColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
    ): TextFieldColors =
        TextFieldDefaults.textFieldColors(
            focusedTextColor = focusedTextColor,
            unfocusedTextColor = unfocusedTextColor,
            disabledTextColor = disabledTextColor,
            cursorColor = cursorColor,
            selectionColors = selectionColors,
            focusedLeadingIconColor = focusedLeadingIconColor,
            unfocusedLeadingIconColor = unfocusedLeadingIconColor,
            disabledLeadingIconColor = disabledLeadingIconColor,
            focusedTrailingIconColor = focusedTrailingIconColor,
            unfocusedTrailingIconColor = unfocusedTrailingIconColor,
            disabledTrailingIconColor = disabledTrailingIconColor,
            focusedPlaceholderColor = focusedPlaceholderColor,
            unfocusedPlaceholderColor = unfocusedPlaceholderColor,
            disabledPlaceholderColor = disabledPlaceholderColor,
        )

    @Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
    @ExperimentalMaterial3Api
    @Composable
    fun inputFieldColors(
        textColor: Color = SearchBarTokens.InputTextColor.toColor(),
        disabledTextColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
        cursorColor: Color = FilledTextFieldTokens.CaretColor.toColor(),
        selectionColors: TextSelectionColors = LocalTextSelectionColors.current,
        focusedLeadingIconColor: Color = SearchBarTokens.LeadingIconColor.toColor(),
        unfocusedLeadingIconColor: Color = SearchBarTokens.LeadingIconColor.toColor(),
        disabledLeadingIconColor: Color = FilledTextFieldTokens.DisabledLeadingIconColor
            .toColor().copy(alpha = FilledTextFieldTokens.DisabledLeadingIconOpacity),
        focusedTrailingIconColor: Color = SearchBarTokens.TrailingIconColor.toColor(),
        unfocusedTrailingIconColor: Color = SearchBarTokens.TrailingIconColor.toColor(),
        disabledTrailingIconColor: Color = FilledTextFieldTokens.DisabledTrailingIconColor
            .toColor().copy(alpha = FilledTextFieldTokens.DisabledTrailingIconOpacity),
        placeholderColor: Color = SearchBarTokens.SupportingTextColor.toColor(),
        disabledPlaceholderColor: Color = FilledTextFieldTokens.DisabledInputColor.toColor()
            .copy(alpha = FilledTextFieldTokens.DisabledInputOpacity),
    ) = inputFieldColors(
        focusedTextColor = textColor,
        unfocusedTextColor = textColor,
        disabledTextColor = disabledTextColor,
        cursorColor = cursorColor,
        selectionColors = selectionColors,
        focusedLeadingIconColor = focusedLeadingIconColor,
        unfocusedLeadingIconColor = unfocusedLeadingIconColor,
        disabledLeadingIconColor = disabledLeadingIconColor,
        focusedTrailingIconColor = focusedTrailingIconColor,
        unfocusedTrailingIconColor = unfocusedTrailingIconColor,
        disabledTrailingIconColor = disabledTrailingIconColor,
        focusedPlaceholderColor = placeholderColor,
        unfocusedPlaceholderColor = placeholderColor,
        disabledPlaceholderColor = disabledPlaceholderColor,
    )
}

/**
 * Represents the colors used by a search bar in different states.
 *
 * See [SearchBarDefaults.colors] for the default implementation that follows Material
 * specifications.
 */
@ExperimentalMaterial3Api
@Immutable
class SearchBarColors internal constructor(
    val containerColor: Color,
    val dividerColor: Color,
    val inputFieldColors: TextFieldColors,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchBarColors

        if (containerColor != other.containerColor) return false
        if (dividerColor != other.dividerColor) return false
        if (inputFieldColors != other.inputFieldColors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = containerColor.hashCode()
        result = 31 * result + dividerColor.hashCode()
        result = 31 * result + inputFieldColors.hashCode()
        return result
    }
}

private val ZeroWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0)

// Measurement specs
private val SearchBarCornerRadius: Dp = InputFieldHeight / 2
internal val DockedActiveTableMinHeight: Dp = 240.dp
private const val DockedActiveTableMaxHeightScreenRatio: Float = 2f / 3f
internal val SearchBarMinWidth: Dp = 360.dp
private val SearchBarMaxWidth: Dp = 720.dp
internal val SearchBarVerticalPadding: Dp = 8.dp
// Search bar has 16dp padding between icons and start/end, while by default text field has 12dp.
private val SearchBarIconOffsetX: Dp = 4.dp

// Animation specs
private const val AnimationDurationMillis: Int = MotionTokens.DurationMedium2.toInt()
private val SizeAnimationSpec: FiniteAnimationSpec<IntSize> =
    tween(durationMillis = AnimationDurationMillis, easing = MotionTokens.EasingLegacyCubicBezier)
private val OpacityAnimationSpec: FiniteAnimationSpec<Float> =
    tween(durationMillis = AnimationDurationMillis, easing = MotionTokens.EasingLegacyCubicBezier)
private val DockedEnterTransition: EnterTransition =
    fadeIn(OpacityAnimationSpec) + expandVertically(SizeAnimationSpec)
private val DockedExitTransition: ExitTransition =
    fadeOut(OpacityAnimationSpec) + shrinkVertically(SizeAnimationSpec)
