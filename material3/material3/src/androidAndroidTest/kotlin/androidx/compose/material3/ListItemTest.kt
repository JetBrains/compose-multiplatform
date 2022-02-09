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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.node.Ref
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterialApi::class)
class ListItemTest {

    @get:Rule
    val rule = createComposeRule()

    val icon24x24 by lazy { ImageBitmap(width = 24.dp.toIntPx(), height = 24.dp.toIntPx()) }
    val icon40x40 by lazy { ImageBitmap(width = 40.dp.toIntPx(), height = 40.dp.toIntPx()) }
    val icon56x56 by lazy { ImageBitmap(width = 56.dp.toIntPx(), height = 56.dp.toIntPx()) }

    @Test
    fun listItem_oneLine_size() {
        val expectedHeightNoIcon = 48.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(text = { Text("Primary text") })
            }
            .assertHeightIsEqualTo(expectedHeightNoIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_withIcon24_size() {
        val expectedHeightSmallIcon = 56.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    text = { Text("Primary text") },
                    icon = { Icon(icon24x24, null) }
                )
            }
            .assertHeightIsEqualTo(expectedHeightSmallIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_withIcon56_size() {
        val expectedHeightLargeIcon = 72.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    text = { Text("Primary text") },
                    icon = { Icon(icon56x56, null) }
                )
            }
            .assertHeightIsEqualTo(expectedHeightLargeIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_twoLine_size() {
        val expectedHeightNoIcon = 64.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    text = { Text("Primary text") },
                    secondaryText = { Text("Secondary text") }
                )
            }
            .assertHeightIsEqualTo(expectedHeightNoIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_twoLine_withIcon_size() {
        val expectedHeightWithIcon = 72.dp

        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    text = { Text("Primary text") },
                    secondaryText = { Text("Secondary text") },
                    icon = { Icon(icon24x24, null) }
                )
            }
            .assertHeightIsEqualTo(expectedHeightWithIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_threeLine_size() {
        val expectedHeight = 88.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    overlineText = { Text("OVERLINE") },
                    text = { Text("Primary text") },
                    secondaryText = { Text("Secondary text") }
                )
            }
            .assertHeightIsEqualTo(expectedHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_threeLine_noSingleLine_size() {
        val expectedHeight = 88.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    text = { Text("Primary text") },
                    secondaryText = { Text("Secondary text with long text") },
                    singleLineSecondaryText = false
                )
            }
            .assertHeightIsEqualTo(expectedHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_threeLine_metaText_size() {
        val expectedHeight = 88.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    overlineText = { Text("OVERLINE") },
                    text = { Text("Primary text") },
                    secondaryText = { Text("Secondary text") },
                    trailing = { Text("meta") }
                )
            }
            .assertHeightIsEqualTo(expectedHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_threeLine_noSingleLine_metaText_size() {
        val expectedHeight = 88.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    text = { Text("Primary text") },
                    secondaryText = { Text("Secondary text with long text") },
                    singleLineSecondaryText = false,
                    trailing = { Text("meta") }
                )
            }
            .assertHeightIsEqualTo(expectedHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_positioning_noIcon() {
        val listItemHeight = 48.dp
        val expectedLeftPadding = 16.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    text = { Text("Primary text", Modifier.saveLayout(textPosition, textSize)) },
                    trailing = {
                        Image(icon24x24, null, Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx()
                    .toFloat()
            )
            assertThat(textPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - textSize.value!!.height) / 2f).roundToInt().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedRightPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - trailingSize.value!!.height) / 2f).roundToInt()
                    .toFloat()
            )
        }
    }

    @Test
    fun listItem_oneLine_positioning_withIcon() {
        val listItemHeight = 56.dp
        val expectedLeftPadding = 16.dp
        val expectedTextLeftPadding = 32.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    text = { Text("Primary text", Modifier.saveLayout(textPosition, textSize)) },
                    icon = { Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize)) }
                )
            }
        }
        rule.runOnIdleWithDensity {
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - iconSize.value!!.height) / 2f).roundToInt().toFloat()
            )
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedTextLeftPadding.roundToPx().toFloat()
            )
            assertThat(textPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - textSize.value!!.height) / 2f).roundToInt().toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_noIcon() {
        val expectedLeftPadding = 16.dp
        val expectedRightPadding = 16.dp
        val expectedTextBaseline = 28.dp
        val expectedSecondaryTextBaselineOffset = 20.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingBaseline = Ref<Float>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    trailing = {
                        Text(
                            "meta",
                            Modifier.saveLayout(trailingPosition, trailingSize, trailingBaseline)
                        )
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat() +
                    expectedSecondaryTextBaselineOffset.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedRightPadding.roundToPx().toFloat()
            )
            assertThat(trailingBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_withSmallIcon() {
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 32.dp
        val expectedTextBaseline = 32.dp
        val expectedSecondaryTextBaselineOffset = 20.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    icon = {
                        Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize))
                    }
                )
            }
        }
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat() +
                    expectedSecondaryTextBaselineOffset.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_withLargeIcon() {
        val listItemHeight = 72.dp
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 16.dp
        val expectedTextBaseline = 32.dp
        val expectedSecondaryTextBaselineOffset = 20.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    icon = {
                        Image(icon40x40, null, Modifier.saveLayout(iconPosition, iconSize))
                    },
                    trailing = {
                        Image(icon24x24, null, Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat() +
                    expectedSecondaryTextBaselineOffset.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedRightPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - trailingSize.value!!.height) / 2).toFloat()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_noOverline_metaText() {
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 32.dp
        val expectedTextBaseline = 28.dp
        val expectedSecondaryTextBaselineOffset = 20.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    text = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    singleLineSecondaryText = false,
                    icon = {
                        Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize))
                    },
                    trailing = {
                        Image(icon24x24, null, Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedTextBaseline.roundToPx().toFloat() +
                    expectedSecondaryTextBaselineOffset.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(expectedLeftPadding.roundToPx().toFloat())
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width.toFloat() -
                    expectedRightPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_overline_trailingIcon() {
        val expectedLeftPadding = 16.dp
        val expectedIconTopPadding = 16.dp
        val expectedContentLeftPadding = 16.dp
        val expectedOverlineBaseline = 28.dp
        val expectedTextBaselineOffset = 20.dp
        val expectedSecondaryTextBaselineOffset = 20.dp
        val expectedRightPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val overlineTextPosition = Ref<Offset>()
        val overlineTextBaseline = Ref<Float>()
        val overlineTextSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()
        val trailingBaseline = Ref<Float>()
        rule.setMaterialContent {
            Box {
                ListItem(
                    overlineText = {
                        Text(
                            "OVERLINE",
                            Modifier.saveLayout(
                                overlineTextPosition,
                                overlineTextSize,
                                overlineTextBaseline
                            )
                        )
                    },
                    text = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    secondaryText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    icon = {
                        Image(
                            icon40x40,
                            null,
                            Modifier.saveLayout(iconPosition, iconSize)
                        )
                    },
                    trailing = {
                        Text(
                            "meta",
                            Modifier.saveLayout(
                                trailingPosition,
                                trailingSize,
                                trailingBaseline
                            )
                        )
                    }
                )
            }
        }

        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(textBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.roundToPx().toFloat() +
                    expectedTextBaselineOffset.roundToPx().toFloat()
            )
            assertThat(overlineTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(overlineTextBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentLeftPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.roundToPx().toFloat() +
                    expectedTextBaselineOffset.roundToPx().toFloat() +
                    expectedSecondaryTextBaselineOffset.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedLeftPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                expectedIconTopPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedRightPadding.roundToPx().toFloat()
            )
            assertThat(trailingBaseline.value!!).isEqualTo(
                expectedOverlineBaseline.roundToPx().toFloat()
            )
        }
    }

    private fun Dp.toIntPx() = (this.value * rule.density.density).roundToInt()

    private fun Modifier.saveLayout(
        coords: Ref<Offset>,
        size: Ref<IntSize>,
        baseline: Ref<Float> = Ref()
    ): Modifier = onGloballyPositioned { coordinates: LayoutCoordinates ->
        coords.value = coordinates.localToRoot(Offset.Zero)
        baseline.value = coordinates[FirstBaseline].toFloat() + coords.value!!.y
        size.value = coordinates.size
    }
}
