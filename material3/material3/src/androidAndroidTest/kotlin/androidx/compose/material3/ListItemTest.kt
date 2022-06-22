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
import androidx.compose.material3.tokens.ListTokens
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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import com.google.common.truth.Truth.assertThat

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMaterial3Api::class)
class ListItemTest {

    @get:Rule
    val rule = createComposeRule()

    val icon24x24 by lazy { ImageBitmap(width = 24.dp.toIntPx(), height = 24.dp.toIntPx()) }
    val icon40x40 by lazy { ImageBitmap(width = 40.dp.toIntPx(), height = 40.dp.toIntPx()) }

    @Test
    fun listItem_oneLine_size() {
        val expectedHeightNoIcon = ListTokens.ListItemContainerHeight
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(headlineText = { Text("Primary text") })
            }
            .assertHeightIsEqualTo(expectedHeightNoIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_withIcon_size() {
        val expectedHeightSmallIcon = ListTokens.ListItemContainerHeight
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    headlineText = { Text("Primary text") },
                    leadingContent = { Icon(icon24x24, null) }
                )
            }
            .assertHeightIsEqualTo(expectedHeightSmallIcon)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_twoLine_size() {
        val expectedHeightNoIcon = 72.dp
        rule
            .setMaterialContentForSizeAssertions {
                ListItem(
                    headlineText = { Text("Primary text") },
                    supportingText = { Text("Secondary text") }
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
                    headlineText = { Text("Primary text") },
                    supportingText = { Text("Secondary text") },
                    leadingContent = { Icon(icon24x24, null) }
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
                    headlineText = { Text("Primary text") },
                    supportingText = { Text("Secondary text") }
                )
            }
            .assertHeightIsEqualTo(expectedHeight)
            .assertWidthIsEqualTo(rule.rootWidth())
    }

    @Test
    fun listItem_oneLine_positioning_noIcon() {
        val listItemHeight = ListTokens.ListItemContainerHeight
        val expectedStartPadding = 16.dp
        val expectedEndPadding = 24.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingSize = Ref<IntSize>()

        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineText = {
                        Text("Primary text", Modifier.saveLayout(textPosition, textSize))
                    },
                    trailingContent = {
                        Image(
                            icon24x24,
                            null,
                            Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }

        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx()
                    .toFloat()
            )
            assertThat(textPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - textSize.value!!.height) / 2f).roundToInt().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedEndPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - trailingSize.value!!.height) / 2f).roundToInt()
                    .toFloat()
            )
        }
    }

    @Test
    fun listItem_oneLine_positioning_withIcon() {
        val listItemHeight = ListTokens.ListItemContainerHeight
        val expectedStartPadding = 16.dp
        val expectedTextStartPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineText = {
                        Text("Primary text", Modifier.saveLayout(textPosition, textSize))
                    },
                    leadingContent = {
                        Image(
                            icon24x24,
                            null,
                            Modifier.saveLayout(iconPosition, iconSize))
                    }
                )
            }
        }
        rule.runOnIdleWithDensity {
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - iconSize.value!!.height) / 2f).roundToInt().toFloat()
            )
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedTextStartPadding.roundToPx().toFloat()
            )
            assertThat(textPosition.value!!.y).isEqualTo(
                ((listItemHeight.roundToPx() - textSize.value!!.height) / 2f).roundToInt().toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_noIcon() {
        val expectedStartPadding = 16.dp
        val expectedEndPadding = 24.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val trailingPosition = Ref<Offset>()
        val trailingBaseline = Ref<Float>()
        val trailingSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineText = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    supportingText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    trailingContent = {
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
                expectedStartPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedEndPadding.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun listItem_twoLine_positioning_withIcon() {
        val expectedStartPadding = 16.dp
        val expectedContentStartPadding = 16.dp

        val textPosition = Ref<Offset>()
        val textBaseline = Ref<Float>()
        val textSize = Ref<IntSize>()
        val secondaryTextPosition = Ref<Offset>()
        val secondaryTextBaseline = Ref<Float>()
        val secondaryTextSize = Ref<IntSize>()
        val iconPosition = Ref<Offset>()
        val iconSize = Ref<IntSize>()
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineText = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    supportingText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    leadingContent = {
                        Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize))
                    }
                )
            }
        }
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_noOverline_metaText() {
        val expectedStartPadding = 16.dp
        val expectedContentStartPadding = 16.dp
        val expectedEndPadding = 24.dp

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
        rule.setMaterialContent(lightColorScheme()) {
            Box {
                ListItem(
                    headlineText = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    supportingText = {
                        Text(
                            "Very long supporting text which will span two lines",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    leadingContent = {
                        Image(icon24x24, null, Modifier.saveLayout(iconPosition, iconSize))
                    },
                    trailingContent = {
                        Image(icon24x24, null, Modifier.saveLayout(trailingPosition, trailingSize))
                    }
                )
            }
        }
        val ds = rule.onRoot().getUnclippedBoundsInRoot()
        rule.runOnIdleWithDensity {
            assertThat(textPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() + iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(expectedStartPadding.roundToPx().toFloat())
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width.toFloat() -
                    expectedEndPadding.roundToPx().toFloat()
            )
        }
    }

    @Test
    fun listItem_threeLine_positioning_overline_trailingIcon() {
        val expectedTopPadding = 16.dp
        val expectedStartPadding = 16.dp
        val expectedContentStartPadding = 16.dp
        val expectedEndPadding = 24.dp

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
        rule.setMaterialContent(lightColorScheme()) {
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
                    headlineText = {
                        Text(
                            "Primary text",
                            Modifier.saveLayout(textPosition, textSize, textBaseline)
                        )
                    },
                    supportingText = {
                        Text(
                            "Secondary text",
                            Modifier.saveLayout(
                                secondaryTextPosition,
                                secondaryTextSize,
                                secondaryTextBaseline
                            )
                        )
                    },
                    leadingContent = {
                        Image(
                            icon40x40,
                            null,
                            Modifier.saveLayout(iconPosition, iconSize)
                        )
                    },
                    trailingContent = {
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
                expectedStartPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(secondaryTextPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(iconPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat()
            )
            assertThat(trailingPosition.value!!.x).isEqualTo(
                ds.width.roundToPx() - trailingSize.value!!.width -
                    expectedEndPadding.roundToPx().toFloat()
            )
            assertThat(overlineTextPosition.value!!.x).isEqualTo(
                expectedStartPadding.roundToPx().toFloat() +
                    iconSize.value!!.width +
                    expectedContentStartPadding.roundToPx().toFloat()
            )
            assertThat(overlineTextPosition.value!!.y).isEqualTo(
                expectedTopPadding.roundToPx().toFloat()
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