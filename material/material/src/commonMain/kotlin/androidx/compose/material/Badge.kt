/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A BadgeBox is used to decorate [content] with a [badge] that can contain dynamic information,
 * such
 * as the presence of a new notification or a number of pending requests. Badges can be icon only
 * or contain short text.
 *
 * A common use case is to display a badge with bottom navigation items.
 * For more information, see [Bottom Navigation](https://material.io/components/bottom-navigation#behavior)
 *
 * A simple icon with badge example looks like:
 * @sample androidx.compose.material.samples.BottomNavigationItemWithBadge
 *
 * @param badge the badge to be displayed - typically a [Badge]
 * @param modifier optional [Modifier] for this item
 * @param content the anchor to which this badge will be positioned
 *
 */
@ExperimentalMaterialApi
@Composable
fun BadgedBox(
    badge: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Layout(
        {
            Box(
                modifier = Modifier.layoutId("anchor"),
                contentAlignment = Alignment.Center,
                content = content
            )
            Box(
                modifier = Modifier.layoutId("badge"),
                content = badge
            )
        },
        modifier = modifier
    ) { measurables, constraints ->

        val badgePlaceable = measurables.first { it.layoutId == "badge" }.measure(
            // Measure with loose constraints for height as we don't want the text to take up more
            // space than it needs.
            constraints.copy(minHeight = 0)
        )

        val anchorPlaceable = measurables.first { it.layoutId == "anchor" }.measure(constraints)

        val firstBaseline = anchorPlaceable[FirstBaseline]
        val lastBaseline = anchorPlaceable[LastBaseline]
        val totalWidth = anchorPlaceable.width
        val totalHeight = anchorPlaceable.height

        layout(
            totalWidth,
            totalHeight,
            // Provide custom baselines based only on the anchor content to avoid default baseline
            // calculations from including by any badge content.
            mapOf(
                FirstBaseline to firstBaseline,
                LastBaseline to lastBaseline
            )
        ) {
            // Use the width of the badge to infer whether it has any content (based on radius used
            // in [Badge]) and determine its horizontal offset.
            val hasContent = badgePlaceable.width > (2 * BadgeRadius.roundToPx())
            val badgeHorizontalOffset =
                if (hasContent) BadgeWithContentHorizontalOffset else BadgeHorizontalOffset

            anchorPlaceable.placeRelative(0, 0)
            val badgeX = anchorPlaceable.width + badgeHorizontalOffset.roundToPx()
            val badgeY = -badgePlaceable.height / 2
            badgePlaceable.placeRelative(badgeX, badgeY)
        }
    }
}

/**
 * Badge is a component that can contain dynamic information, such as the presence of a new
 * notification or a number of pending requests. Badges can be icon only or contain short text.
 *
 * See [BadgedBox] for a top level layout that will properly place the badge relative to content
 * such as text or an icon.
 *
 * @param modifier optional [Modifier] for this item
 * @param backgroundColor the background color for the badge
 * @param contentColor the color of label text rendered in the badge
 * @param content optional content to be rendered inside the badge
 *
 */
@ExperimentalMaterialApi
@Composable
fun Badge(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.error,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (RowScope.() -> Unit)? = null,
) {
    val radius = if (content != null) BadgeWithContentRadius else BadgeRadius
    val shape = RoundedCornerShape(radius)

    // Draw badge container.
    Row(
        modifier = modifier
            .defaultMinSize(minWidth = radius * 2, minHeight = radius * 2)
            .background(
                color = backgroundColor,
                shape = shape
            )
            .clip(shape)
            .padding(
                horizontal = BadgeWithContentHorizontalPadding
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (content != null) {
            CompositionLocalProvider(
                LocalContentColor provides contentColor
            ) {
                val style = MaterialTheme.typography.button.copy(fontSize = BadgeContentFontSize)
                ProvideTextStyle(
                    value = style,
                    content = { content() }
                )
            }
        }
    }
}

/*@VisibleForTesting*/
internal val BadgeRadius = 4.dp

/*@VisibleForTesting*/
internal val BadgeWithContentRadius = 8.dp
private val BadgeContentFontSize = 10.sp

/*@VisibleForTesting*/
// Leading and trailing text padding when a badge is displaying text that is too long to fit in
// a circular badge, e.g. if badge number is greater than 9.
internal val BadgeWithContentHorizontalPadding = 4.dp

/*@VisibleForTesting*/
// Horizontally align start/end of text badge 6dp from the end/start edge of its anchor
internal val BadgeWithContentHorizontalOffset = -6.dp

/*@VisibleForTesting*/
// Horizontally align start/end of icon only badge 4dp from the end/start edge of anchor
internal val BadgeHorizontalOffset = -4.dp
