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

@file:Suppress("Deprecation")

package androidx.compose.material

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Box
import androidx.compose.foundation.ContentGravity
import androidx.compose.foundation.IndicationAmbient
import androidx.compose.foundation.InteractionState
import androidx.compose.foundation.ProvideTextStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSizeConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.preferredSizeIn
import androidx.compose.foundation.layout.preferredWidth
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A floating action button (FAB) is a button that represents the primary action of a screen.
 *
 * This FAB is typically used with an [androidx.compose.foundation.Icon]:
 *
 * @sample androidx.compose.material.samples.SimpleFab
 *
 * See [ExtendedFloatingActionButton] for an extended FAB that contains text and an optional icon.
 *
 * @param modifier [Modifier] to be applied to this FAB.
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 *  when it is null.
 * @param shape The [Shape] of this FAB
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color for content inside this FAB
 * @param elevation The z-coordinate at which to place this FAB. This controls the size
 * of the shadow below the button.
 * @param icon the content of this FAB
 */
@Composable
fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 6.dp,
    icon: @Composable () -> Unit
) {
    // TODO(aelias): Avoid manually managing the ripple once http://b/157687898
    // is fixed and we have more flexibility to move the clickable modifier
    // (see candidate approach aosp/1361921)
    val interactionState = remember { InteractionState() }
    Surface(
        modifier = modifier.clickable(
            onClick = onClick,
            interactionState = interactionState,
            indication = null),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        ProvideTextStyle(MaterialTheme.typography.button) {
            Box(
                modifier = Modifier
                    .defaultMinSizeConstraints(minWidth = FabSize, minHeight = FabSize)
                    .indication(interactionState, IndicationAmbient.current()),
                gravity = ContentGravity.Center,
                children = icon
            )
        }
    }
}

/**
 * A floating action button (FAB) is a button that represents the primary action of a screen.
 *
 * This extended FAB contains text and an optional icon that will be placed at the start. See
 * [FloatingActionButton] for a FAB that just contains some content, typically an icon.
 *
 * @sample androidx.compose.material.samples.SimpleExtendedFabWithIcon
 *
 * If you want FAB’s container to have a fluid width (to be defined by its relationship to something
 * else on screen, such as screen width or the layout grid) just apply an appropriate modifier.
 * For example to fill the whole available width you can do:
 *
 * @sample androidx.compose.material.samples.FluidExtendedFab
 *
 * @param text Text label displayed inside this FAB
 * @param icon Optional icon for this FAB, typically this will be a [androidx.compose.foundation.Icon]
 * @param modifier [Modifier] to be applied to this FAB
 * @param onClick will be called when user clicked on this FAB. The FAB will be disabled
 * when it is null.
 * @param shape The [Shape] of this FAB
 * @param onClick will be called when user clicked on the button. The button will be disabled
 * when it is null.
 * @param backgroundColor The background color. Use [Color.Transparent] to have no color
 * @param contentColor The preferred content color. Will be used by text and iconography
 * @param elevation The z-coordinate at which to place this button. This controls the size
 * of the shadow below the button.
 */
@Composable
fun ExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.small.copy(CornerSize(percent = 50)),
    backgroundColor: Color = MaterialTheme.colors.secondary,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 6.dp
) {
    FloatingActionButton(
        modifier = modifier.preferredSizeIn(
            minWidth = ExtendedFabSize,
            minHeight = ExtendedFabSize
        ),
        onClick = onClick,
        shape = shape,
        backgroundColor = backgroundColor,
        contentColor = contentColor,
        elevation = elevation
    ) {
        Box(
            modifier = Modifier.padding(
                start = ExtendedFabTextPadding,
                end = ExtendedFabTextPadding
            ),
            gravity = ContentGravity.Center
        ) {
            if (icon == null) {
                text()
            } else {
                Row(verticalGravity = ContentGravity.CenterVertically) {
                    icon()
                    Spacer(Modifier.preferredWidth(ExtendedFabIconPadding))
                    text()
                }
            }
        }
    }
}

private val FabSize = 56.dp
private val ExtendedFabSize = 48.dp
private val ExtendedFabIconPadding = 12.dp
private val ExtendedFabTextPadding = 20.dp
