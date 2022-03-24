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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.tokens.ElevatedCardTokens
import androidx.compose.material3.tokens.FilledCardTokens
import androidx.compose.material3.tokens.OutlinedCardTokens
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.flow.collect

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external" target="_blank">Material Design filled card</a>.
 *
 * Cards contain content and actions about a single subject. Filled cards provide subtle separation
 * from the background. This has less emphasis than elevated or outlined cards.
 *
 * This Card does not handle input events - see the other Card overloads if you want a clickable or
 * selectable Card.
 *
 * Card sample:
 * @sample androidx.compose.material3.samples.CardSample
 *
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the card's shape.
 * @param containerColor The container color of the card.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border [BorderStroke] to draw on top of the card.
 * @param elevation [CardElevation] used to resolve the elevation for this card. The resolved value
 * control the size of the shadow below the card, as well as its tonal elevation. When
 * [containerColor] is [ColorScheme.surface], a higher tonal elevation value will result in a darker
 * card color in light theme and lighter color in dark theme. See also [Surface].
 */
@ExperimentalMaterial3Api
@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = FilledCardTokens.ContainerShape.toShape(),
    containerColor: Color = FilledCardTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation.tonalElevation(interactionSource = null).value,
        shadowElevation = elevation.shadowElevation(interactionSource = null).value,
        border = border,
    ) {
        Column(content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external" target="_blank">Material Design filled card</a>.
 *
 * Cards contain content and actions about a single subject. Filled cards provide subtle separation
 * from the background. This has less emphasis than elevated or outlined cards.
 *
 * This Card handles click events, calling its [onClick] lambda.
 *
 * Clickable card sample:
 * @sample androidx.compose.material3.samples.ClickableCardSample
 *
 * @param onClick callback to be called when the card is clicked
 * @param modifier Modifier to be applied to the layout of the card.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Card. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this card in different states.
 * @param shape Defines the card's shape.
 * @param containerColor The container color of the card.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border [BorderStroke] to draw on top of the card.
 * @param elevation [CardElevation] used to resolve the elevation for this card when the
 * [interactionSource] emits its states. The resolved values control the size of the shadow below
 * the card, as well as its tonal elevation. When [containerColor] is [ColorScheme.surface], a
 * higher tonal elevation value will result in a darker card color in light theme and lighter color
 * in dark theme. See also [Surface].
 */
@ExperimentalMaterial3Api
@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = FilledCardTokens.ContainerShape.toShape(),
    containerColor: Color = FilledCardTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    border: BorderStroke? = null,
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        tonalElevation = elevation.tonalElevation(interactionSource).value,
        shadowElevation = elevation.shadowElevation(interactionSource).value,
        border = border,
        interactionSource = interactionSource,
    ) {
        Column(content = content)
    }
}

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external" target="_blank">Material Design elevated card</a>.
 *
 * Elevated cards contain content and actions about a single subject. They have a drop shadow,
 * providing more separation from the background than filled cards, but less than outlined cards.
 *
 * This ElevatedCard does not handle input events - see the other ElevatedCard overloads if you
 * want a clickable or selectable ElevatedCard.
 *
 * Elevated card sample:
 * @sample androidx.compose.material3.samples.ElevatedCardSample
 *
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the card's shape.
 * @param containerColor The container color of the card.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param elevation [CardElevation] used to resolve the elevation for this card. The resolved value
 * control the size of the shadow below the card, as well as its tonal elevation. When
 * [containerColor] is [ColorScheme.surface], a higher tonal elevation value will result in a darker
 * card color in light theme and lighter color in dark theme. See also [Surface].
 */
@ExperimentalMaterial3Api
@Composable
fun ElevatedCard(
    modifier: Modifier = Modifier,
    shape: Shape = ElevatedCardTokens.ContainerShape.toShape(),
    containerColor: Color = ElevatedCardTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    border = null,
    elevation = elevation,
    content = content
)

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external" target="_blank">Material Design elevated card</a>.
 *
 * Elevated cards contain content and actions about a single subject. They have a drop shadow,
 * providing more separation from the background than filled cards, but less than outlined cards.
 *
 * This ElevatedCard handles click events, calling its [onClick] lambda.
 *
 * Clickable elevated card sample:
 * @sample androidx.compose.material3.samples.ClickableElevatedCardSample
 *
 * @param onClick callback to be called when the card is clicked
 * @param modifier Modifier to be applied to the layout of the card.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this card. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this card in different states.
 * @param shape Defines the card's shape.
 * @param containerColor The container color of the card.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param elevation [CardElevation] used to resolve the elevation for this card when the
 * [interactionSource] emits its states. The resolved values control the size of the shadow below
 * the card, as well as its tonal elevation. When [containerColor] is [ColorScheme.surface], a
 * higher tonal elevation value will result in a darker card color in light theme and lighter color
 * in dark theme. See also [Surface].
 */
@ExperimentalMaterial3Api
@Composable
fun ElevatedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = ElevatedCardTokens.ContainerShape.toShape(),
    containerColor: Color = ElevatedCardTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    elevation: CardElevation = CardDefaults.elevatedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) = Card(
    onClick = onClick,
    modifier = modifier,
    interactionSource = interactionSource,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    border = null,
    elevation = elevation,
    content = content
)

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external" target="_blank">Material Design outlined card</a>.
 *
 * Outlined cards contain content and actions about a single subject. They have a visual boundary
 * around the container. This can provide greater emphasis than the other types.
 *
 * This OutlinedCard does not handle input events - see the other OutlinedCard overloads if you want
 * a clickable or selectable OutlinedCard.
 *
 * Outlined card sample:
 * @sample androidx.compose.material3.samples.OutlinedCardSample
 *
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the card's shape.
 * @param containerColor The container color of the card.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border [BorderStroke] to draw on top of the card.
 * @param elevation [CardElevation] used to resolve the elevation for this card. The resolved value
 * control the size of the shadow below the card, as well as its tonal elevation. When
 * [containerColor] is [ColorScheme.surface], a higher tonal elevation value will result in a darker
 * card color in light theme and lighter color in dark theme. See also [Surface].
 */
@ExperimentalMaterial3Api
@Composable
fun OutlinedCard(
    modifier: Modifier = Modifier,
    shape: Shape = OutlinedCardTokens.ContainerShape.toShape(),
    containerColor: Color = OutlinedCardTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    border: BorderStroke = BorderStroke(
        OutlinedCardTokens.OutlineWidth,
        OutlinedCardTokens.OutlineColor.toColor()
    ),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    border = border,
    elevation = elevation,
    content = content
)

/**
 * <a href="https://m3.material.io/components/cards/overview" class="external" target="_blank">Material Design outlined card</a>.
 *
 * Outlined cards contain content and actions about a single subject. They have a visual boundary
 * around the container. This can provide greater emphasis than the other types.
 *
 * This OutlinedCard handles click events, calling its [onClick] lambda.
 *
 * Clickable outlined card sample:
 * @sample androidx.compose.material3.samples.ClickableOutlinedCardSample
 *
 * @param onClick callback to be called when the card is clicked
 * @param modifier Modifier to be applied to the layout of the card.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this card. You can create and pass in your own remembered
 * [MutableInteractionSource] to observe [Interaction]s that will customize the appearance
 * / behavior of this card in different states.
 * @param shape Defines the card's shape.
 * @param containerColor The container color of the card.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [containerColor], or if [containerColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border [BorderStroke] to draw on top of the card.
 * @param elevation [CardElevation] used to resolve the elevation for this card when the
 * [interactionSource] emits its states. The resolved values control the size of the shadow below
 * the card, as well as its tonal elevation. When [containerColor] is [ColorScheme.surface], a
 * higher tonal elevation value will result in a darker card color in light theme and lighter color
 * in dark theme. See also [Surface].
 */
@ExperimentalMaterial3Api
@Composable
fun OutlinedCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = OutlinedCardTokens.ContainerShape.toShape(),
    containerColor: Color = OutlinedCardTokens.ContainerColor.toColor(),
    contentColor: Color = contentColorFor(containerColor),
    border: BorderStroke = BorderStroke(
        OutlinedCardTokens.OutlineWidth,
        OutlinedCardTokens.OutlineColor.toColor()
    ),
    elevation: CardElevation = CardDefaults.outlinedCardElevation(),
    content: @Composable ColumnScope.() -> Unit
) = Card(
    onClick = onClick,
    modifier = modifier,
    interactionSource = interactionSource,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    border = border,
    elevation = elevation,
    content = content
)

/**
 * Represents the elevation for a card in different states.
 *
 * - See [CardDefaults.cardElevation] for the default elevation used in a [Card].
 * - See [CardDefaults.elevatedCardElevation] for the default elevation used in an [ElevatedCard].
 * - See [CardDefaults.outlinedCardElevation] for the default elevation used in an [OutlinedCard].
 */
@Stable
interface CardElevation {
    /**
     * Represents the tonal elevation used in a card, depending on its [interactionSource].
     *
     * Tonal elevation is used to apply a color shift to the surface to give the it higher emphasis.
     *
     * For all Material cards with elevation, this returns the same value as [shadowElevation].
     *
     * - See [shadowElevation] for an elevation that draws a shadow around the card's bounds.
     *
     * @param interactionSource the [InteractionSource] for this card
     */
    @Composable
    fun tonalElevation(interactionSource: InteractionSource?): State<Dp>

    /**
     * Represents the shadow elevation used in a card, depending on the [interactionSource].
     *
     * Shadow elevation is used to apply a drop shadow around the card to give it higher emphasis.
     *
     * For all Material cards with elevation, this returns the same value as [tonalElevation].
     *
     * - See [tonalElevation] for an elevation that applies a color shift to the surface.
     *
     * @param interactionSource the [InteractionSource] for this card
     */
    @Composable
    fun shadowElevation(interactionSource: InteractionSource?): State<Dp>
}

/**
 * Contains the default values used by all card types.
 */
object CardDefaults {

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for a [Card].
     *
     * @param defaultElevation the elevation used when the [Card] is has no other [Interaction]s.
     * @param pressedElevation the elevation used when the [Card] is pressed.
     * @param focusedElevation the elevation used when the [Card] is focused.
     * @param hoveredElevation the elevation used when the [Card] is hovered.
     * @param draggedElevation the elevation used when the [Card] is dragged.
     */
    @Composable
    fun cardElevation(
        defaultElevation: Dp = FilledCardTokens.ContainerElevation,
        pressedElevation: Dp = FilledCardTokens.PressedContainerElevation,
        focusedElevation: Dp = FilledCardTokens.FocusContainerElevation,
        hoveredElevation: Dp = FilledCardTokens.HoverContainerElevation,
        draggedElevation: Dp = FilledCardTokens.DraggedContainerElevation
    ): CardElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation
        ) {
            DefaultCardElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
            )
        }
    }

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [ElevatedCard].
     *
     * @param defaultElevation the elevation used when the [ElevatedCard] is has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [ElevatedCard] is pressed.
     * @param focusedElevation the elevation used when the [ElevatedCard] is focused.
     * @param hoveredElevation the elevation used when the [ElevatedCard] is hovered.
     * @param draggedElevation the elevation used when the [ElevatedCard] is dragged.
     */
    @Composable
    fun elevatedCardElevation(
        defaultElevation: Dp = ElevatedCardTokens.ContainerElevation,
        pressedElevation: Dp = ElevatedCardTokens.PressedContainerElevation,
        focusedElevation: Dp = ElevatedCardTokens.FocusContainerElevation,
        hoveredElevation: Dp = ElevatedCardTokens.HoverContainerElevation,
        draggedElevation: Dp = ElevatedCardTokens.DraggedContainerElevation
    ): CardElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation
        ) {
            DefaultCardElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
            )
        }
    }

    /**
     * Creates a [CardElevation] that will animate between the provided values according to the
     * Material specification for an [OutlinedCard].
     *
     * @param defaultElevation the elevation used when the [OutlinedCard] is has no other
     * [Interaction]s.
     * @param pressedElevation the elevation used when the [OutlinedCard] is pressed.
     * @param focusedElevation the elevation used when the [OutlinedCard] is focused.
     * @param hoveredElevation the elevation used when the [OutlinedCard] is hovered.
     * @param draggedElevation the elevation used when the [OutlinedCard] is dragged.
     */
    @Composable
    fun outlinedCardElevation(
        defaultElevation: Dp = OutlinedCardTokens.ContainerElevation,
        pressedElevation: Dp = defaultElevation,
        focusedElevation: Dp = defaultElevation,
        hoveredElevation: Dp = defaultElevation,
        draggedElevation: Dp = OutlinedCardTokens.DraggedContainerElevation
    ): CardElevation {
        return remember(
            defaultElevation,
            pressedElevation,
            focusedElevation,
            hoveredElevation,
            draggedElevation
        ) {
            DefaultCardElevation(
                defaultElevation = defaultElevation,
                pressedElevation = pressedElevation,
                focusedElevation = focusedElevation,
                hoveredElevation = hoveredElevation,
                draggedElevation = draggedElevation,
            )
        }
    }
}

/**
 * Default [CardElevation] implementation.
 *
 * This default implementation supports animating the elevation for pressed, focused, hovered, and
 * dragged interactions.
 */
@Immutable
private class DefaultCardElevation(
    private val defaultElevation: Dp,
    private val pressedElevation: Dp,
    private val focusedElevation: Dp,
    private val hoveredElevation: Dp,
    private val draggedElevation: Dp,
) : CardElevation {
    @Composable
    override fun tonalElevation(interactionSource: InteractionSource?): State<Dp> {
        if (interactionSource == null) {
            return remember { mutableStateOf(defaultElevation) }
        }
        return animateElevation(interactionSource = interactionSource)
    }

    @Composable
    override fun shadowElevation(interactionSource: InteractionSource?): State<Dp> {
        if (interactionSource == null) {
            return remember { mutableStateOf(defaultElevation) }
        }
        return animateElevation(interactionSource = interactionSource)
    }

    @Composable
    private fun animateElevation(interactionSource: InteractionSource): State<Dp> {
        val interactions = remember { mutableStateListOf<Interaction>() }
        LaunchedEffect(interactionSource) {
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is HoverInteraction.Enter -> {
                        interactions.add(interaction)
                    }
                    is HoverInteraction.Exit -> {
                        interactions.remove(interaction.enter)
                    }
                    is FocusInteraction.Focus -> {
                        interactions.add(interaction)
                    }
                    is FocusInteraction.Unfocus -> {
                        interactions.remove(interaction.focus)
                    }
                    is PressInteraction.Press -> {
                        interactions.add(interaction)
                    }
                    is PressInteraction.Release -> {
                        interactions.remove(interaction.press)
                    }
                    is PressInteraction.Cancel -> {
                        interactions.remove(interaction.press)
                    }
                    is DragInteraction.Start -> {
                        interactions.add(interaction)
                    }
                    is DragInteraction.Stop -> {
                        interactions.remove(interaction.start)
                    }
                    is DragInteraction.Cancel -> {
                        interactions.remove(interaction.start)
                    }
                }
            }
        }

        val interaction = interactions.lastOrNull()

        val target = when (interaction) {
            is PressInteraction.Press -> pressedElevation
            is HoverInteraction.Enter -> hoveredElevation
            is FocusInteraction.Focus -> focusedElevation
            is DragInteraction.Start -> draggedElevation
            else -> defaultElevation
        }

        val animatable = remember { Animatable(target, Dp.VectorConverter) }

        LaunchedEffect(target) {
            val lastInteraction = when (animatable.targetValue) {
                pressedElevation -> PressInteraction.Press(Offset.Zero)
                hoveredElevation -> HoverInteraction.Enter()
                focusedElevation -> FocusInteraction.Focus()
                draggedElevation -> DragInteraction.Start()
                else -> null
            }
            animatable.animateElevation(
                from = lastInteraction,
                to = interaction,
                target = target
            )
        }

        return animatable.asState()
    }
}
