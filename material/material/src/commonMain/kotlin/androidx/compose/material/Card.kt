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

package androidx.compose.material

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * <a href="https://material.io/components/cards" class="external" target="_blank">Material Design card</a>.
 *
 * Cards contain content and actions about a single subject.
 *
 * ![Cards image](https://developer.android.com/images/reference/androidx/compose/material/cards.png)
 *
 * This version of Card will block clicks behind it. For clickable card, please use another
 * overload that accepts `onClick` as a parameter.
 *
 * @sample androidx.compose.material.samples.CardSample
 *
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the card's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param backgroundColor The background color.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [backgroundColor], or if [backgroundColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this card. This controls
 *  the size of the shadow below the card.
 */
@Composable
@NonRestartableComposable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation,
        border = border,
        content = content
    )
}

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * This version of Card provides click handling as well. If you do not want Card to handle
 * clicks, consider using another overload.
 *
 * @sample androidx.compose.material.samples.ClickableCardSample
 *
 * @param onClick callback to be called when the card is clicked
 * @param modifier Modifier to be applied to the layout of the card.
 * @param enabled Controls the enabled state of the card. When `false`, this card will not
 * be clickable
 * @param shape Defines the card's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param backgroundColor The background color.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [backgroundColor], or if [backgroundColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this card. This controls
 *  the size of the shadow below the card.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Card. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the appearance
 * / behavior of this card in different [Interaction]s.
 */
@ExperimentalMaterialApi
@Composable
@NonRestartableComposable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = border,
        elevation = elevation,
        interactionSource = interactionSource,
        content = content
    )
}

/**
 * Cards are [Surface]s that display content and actions on a single topic.
 *
 * This version of Card provides click handling as well. If you do not want Card to handle
 * clicks, consider using another overload.
 *
 * @sample androidx.compose.material.samples.ClickableCardSample
 *
 * @param onClick callback to be called when the card is clicked
 * @param modifier Modifier to be applied to the layout of the card.
 * @param shape Defines the card's shape as well its shadow. A shadow is only
 *  displayed if the [elevation] is greater than zero.
 * @param backgroundColor The background color.
 * @param contentColor The preferred content color provided by this card to its children.
 * Defaults to either the matching content color for [backgroundColor], or if [backgroundColor]
 * is not a color from the theme, this will keep the same value set above this card.
 * @param border Optional border to draw on top of the card
 * @param elevation The z-coordinate at which to place this card. This controls
 *  the size of the shadow below the card.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this Card. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the appearance
 * / behavior of this card in different [Interaction]s.
 * @param indication indication to be shown when card is pressed. By default, indication from
 * [LocalIndication] will be used. Pass `null` to show no indication, or current value from
 * [LocalIndication] to show theme default
 * @param enabled Controls the enabled state of the card. When `false`, this card will not
 * be clickable
 * @param onClickLabel semantic / accessibility label for the [onClick] action
 * @param role the type of user interface element. Accessibility services might use this
 * to describe the element or do customizations. For example, if the Card acts as a button, you
 * should pass the [Role.Button]
 */
@ExperimentalMaterialApi
@Composable
@NonRestartableComposable
@Suppress()
@Deprecated(
    "This API is deprecated with the introduction a newer Card function" +
        " overload that accepts an onClick().",
    replaceWith = ReplaceWith(
        "Card(onClick, modifier, enabled, shape, backgroundColor, contentColor, border," +
            " elevation, interactionSource, content)"
    ),
    level = DeprecationLevel.ERROR
)
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    backgroundColor: Color = MaterialTheme.colors.surface,
    contentColor: Color = contentColorFor(backgroundColor),
    border: BorderStroke? = null,
    elevation: Dp = 1.dp,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    content: @Composable () -> Unit
) {
    @Suppress("DEPRECATION_ERROR")
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        border = border,
        elevation = elevation,
        interactionSource = interactionSource,
        indication = indication,
        enabled = enabled,
        onClickLabel = onClickLabel,
        role = role,
        content = content
    )
}
