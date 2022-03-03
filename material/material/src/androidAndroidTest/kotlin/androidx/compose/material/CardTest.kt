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

import android.os.Build
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class CardTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    @LargeTest
    fun shapeAndColorFromThemeIsUsed() {
        val shape = CutCornerShape(8.dp)
        val background = Color.Yellow
        var cardColor = Color.Transparent
        rule.setMaterialContent {
            Surface(color = background) {
                Box {
                    cardColor = MaterialTheme.colors.surface
                    CompositionLocalProvider(LocalShapes provides Shapes(medium = shape)) {
                        Card(
                            modifier = Modifier
                                .semantics(mergeDescendants = true) {}
                                .testTag("card"),
                            elevation = 0.dp
                        ) {
                            Box(Modifier.size(50.dp, 50.dp))
                        }
                    }
                }
            }
        }

        rule.onNodeWithTag("card")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = shape,
                shapeColor = cardColor,
                backgroundColor = background,
                shapeOverlapPixelCount = with(rule.density) { 1.dp.toPx() }
            )
    }

    @ExperimentalMaterialApi
    @Test
    fun clickableOverload_semantics() {
        val count = mutableStateOf(0)
        rule.setMaterialContent {
            Card(
                onClick = { count.value += 1 },
                modifier = Modifier.testTag("card")
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("card")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performClick()
            .assertTextEquals("1")
    }

    @ExperimentalMaterialApi
    @Test
    fun clickableOverload_clickAction() {
        val count = mutableStateOf(0f)
        rule.setMaterialContent {
            Card(
                modifier = Modifier.testTag("card"),
                onClick = { count.value += 1 }
            ) {
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("card")
            .performClick()
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag("card")
            .performClick()
            .performClick()
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @ExperimentalMaterialApi
    @Test
    fun clickableOverload_enabled_disabled() {
        val count = mutableStateOf(0f)
        val enabled = mutableStateOf(true)
        rule.setMaterialContent {
            Card(
                modifier = Modifier.testTag("card"),
                enabled = enabled.value,
                onClick = { count.value += 1 }
            ) {
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("card")
            .assertIsEnabled()
            .performClick()

        Truth.assertThat(count.value).isEqualTo(1)
        rule.runOnIdle {
            enabled.value = false
        }

        rule.onNodeWithTag("card")
            .assertIsNotEnabled()
            .performClick()
            .performClick()
        Truth.assertThat(count.value).isEqualTo(1)
    }

    @ExperimentalMaterialApi
    @Test
    fun clickableOverload_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.setContent {
            scope = rememberCoroutineScope()
            Card(
                modifier = Modifier.testTag("card"),
                onClick = {},
                interactionSource = interactionSource
            ) {
                Spacer(Modifier.size(30.dp))
            }
        }

        val interactions = mutableListOf<Interaction>()

        scope!!.launch {
            interactionSource.interactions.collect { interactions.add(it) }
        }

        rule.runOnIdle {
            Truth.assertThat(interactions).isEmpty()
        }

        rule.onNodeWithTag("card")
            .performTouchInput { down(center) }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("card")
            .performTouchInput { up() }

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(2)
            Truth.assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
            Truth.assertThat(interactions[1]).isInstanceOf(PressInteraction.Release::class.java)
            Truth.assertThat((interactions[1] as PressInteraction.Release).press)
                .isEqualTo(interactions[0])
        }
    }

    @Test
    fun card_blockClicks() {
        val state = mutableStateOf(0)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Button(
                    modifier = Modifier.fillMaxSize().testTag("clickable"),
                    onClick = { state.value += 1 }
                ) {
                    Text("button fullscreen")
                }
                Card(Modifier.fillMaxSize()) {}
            }
        }
        rule.onNodeWithTag("clickable")
            .assertHasClickAction()
            .performClick()
        // still 0
        Truth.assertThat(state.value).isEqualTo(0)
    }
}
