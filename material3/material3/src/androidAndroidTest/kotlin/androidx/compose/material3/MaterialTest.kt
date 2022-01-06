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

package androidx.compose.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getAlignmentLinePosition
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width

/**
 * Wraps Compose content in a [MaterialTheme] and a [Surface].
 *
 * @param colorScheme a [ColorScheme] to provide to the theme. Usually a [lightColorScheme],
 * [darkColorScheme], or a dynamic one
 * @param modifier a [Modifier] to be applied at the [Surface] wrapper
 */
fun ComposeContentTestRule.setMaterialContent(
    colorScheme: ColorScheme,
    modifier: Modifier = Modifier,
    composable: @Composable () -> Unit
) {
    setContent {
        MaterialTheme(colorScheme = colorScheme) {
            Surface(modifier = modifier, content = composable)
        }
    }
}

fun <T> ComposeTestRule.runOnIdleWithDensity(action: Density.() -> T): T {
    return runOnIdle {
        density.action()
    }
}

fun SemanticsNodeInteraction.getFirstBaselinePosition() = getAlignmentLinePosition(FirstBaseline)

fun SemanticsNodeInteraction.getLastBaselinePosition() = getAlignmentLinePosition(LastBaseline)

fun SemanticsNodeInteraction.assertIsSquareWithSize(expectedSize: Dp) =
    assertWidthIsEqualTo(expectedSize).assertHeightIsEqualTo(expectedSize)

fun ComposeTestRule.rootWidth(): Dp = onRoot().getUnclippedBoundsInRoot().width

fun ComposeTestRule.rootHeight(): Dp = onRoot().getUnclippedBoundsInRoot().height

/**
 * Constant to emulate very big but finite constraints
 */
val BigTestMaxWidth = 5000.dp
val BigTestMaxHeight = 5000.dp

fun ComposeContentTestRule.setMaterialContentForSizeAssertions(
    parentMaxWidth: Dp = BigTestMaxWidth,
    parentMaxHeight: Dp = BigTestMaxHeight,
    // TODO : figure out better way to make it flexible
    content: @Composable () -> Unit
): SemanticsNodeInteraction {
    setContent {
        MaterialTheme {
            Surface {
                Box {
                    Box(
                        Modifier.sizeIn(
                            maxWidth = parentMaxWidth,
                            maxHeight = parentMaxHeight
                        ).testTag("containerForSizeAssertion")
                    ) {
                        content()
                    }
                }
            }
        }
    }

    return onNodeWithTag("containerForSizeAssertion")
}
