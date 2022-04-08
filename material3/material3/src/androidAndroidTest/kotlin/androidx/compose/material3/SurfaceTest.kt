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

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.testutils.assertPixels
import androidx.compose.testutils.assertShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalMaterial3Api::class)
@MediumTest
@RunWith(AndroidJUnit4::class)
class SurfaceTest {

    @get:Rule
    val rule = createComposeRule()

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun noTonalElevationColorIsSetOnNonElevatedSurfaceColor() {
        var absoluteTonalElevation: Dp = 0.dp
        var surfaceColor: Color = Color.Unspecified
        rule.setMaterialContent(lightColorScheme()) {
            surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(color = surfaceColor, tonalElevation = 0.dp) {
                    absoluteTonalElevation = LocalAbsoluteTonalElevation.current
                    Box(Modifier.fillMaxSize())
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(absoluteTonalElevation).isEqualTo(0.dp)
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = surfaceColor,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun tonalElevationColorIsSetOnElevatedSurfaceColor() {
        var absoluteTonalElevation: Dp = 0.dp
        var surfaceTonalColor: Color = Color.Unspecified
        var surfaceColor: Color
        rule.setMaterialContent(lightColorScheme()) {
            surfaceColor = MaterialTheme.colorScheme.surface
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(color = surfaceColor, tonalElevation = 2.dp) {
                    absoluteTonalElevation = LocalAbsoluteTonalElevation.current
                    Box(Modifier.fillMaxSize())
                }
                surfaceTonalColor =
                    MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteTonalElevation)
            }
        }

        rule.runOnIdle {
            Truth.assertThat(absoluteTonalElevation).isEqualTo(2.dp)
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = surfaceTonalColor,
                backgroundColor = Color.White
            )
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun tonalElevationColorIsNotSetOnNonSurfaceColor() {
        var absoluteTonalElevation: Dp = 0.dp
        rule.setMaterialContent(lightColorScheme()) {
            Box(
                Modifier
                    .size(10.dp, 10.dp)
                    .semantics(mergeDescendants = true) {}
                    .testTag("box")
            ) {
                Surface(color = Color.Green, tonalElevation = 2.dp) {
                    Box(Modifier.fillMaxSize())
                    absoluteTonalElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(absoluteTonalElevation).isEqualTo(2.dp)
        }

        rule.onNodeWithTag("box")
            .captureToImage()
            .assertShape(
                density = rule.density,
                shape = RectangleShape,
                shapeColor = Color.Green,
                backgroundColor = Color.White
            )
    }

    @Test
    fun absoluteElevationCompositionLocalIsSet() {
        var outerElevation: Dp? = null
        var innerElevation: Dp? = null
        rule.setMaterialContent(lightColorScheme()) {
            Surface(tonalElevation = 2.dp) {
                outerElevation = LocalAbsoluteTonalElevation.current
                Surface(tonalElevation = 4.dp) {
                    innerElevation = LocalAbsoluteTonalElevation.current
                }
            }
        }

        rule.runOnIdle {
            Truth.assertThat(outerElevation).isEqualTo(2.dp)
            Truth.assertThat(innerElevation).isEqualTo(6.dp)
        }
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun absoluteElevationIsNotUsedForShadows() {
        rule.setMaterialContent(lightColorScheme()) {
            Column {
                Box(
                    Modifier
                        .padding(10.dp)
                        .size(10.dp, 10.dp)
                        .semantics(mergeDescendants = true) {}
                        .testTag("top level")
                ) {
                    Surface(
                        Modifier.fillMaxSize().padding(0.dp),
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp,
                        color = Color.Blue,
                        content = {}
                    )
                }

                // Set LocalAbsoluteTonalElevation to increase the absolute elevation
                CompositionLocalProvider(
                    LocalAbsoluteTonalElevation provides 2.dp
                ) {
                    Box(
                        Modifier
                            .padding(10.dp)
                            .size(10.dp, 10.dp)
                            .semantics(mergeDescendants = true) {}
                            .testTag("nested")
                    ) {
                        Surface(
                            Modifier.fillMaxSize().padding(0.dp),
                            tonalElevation = 0.dp,
                            shadowElevation = 2.dp,
                            color = Color.Blue,
                            content = {}
                        )
                    }
                }
            }
        }

        val topLevelSurfaceBitmap = rule.onNodeWithTag("top level").captureToImage()
        val nestedSurfaceBitmap = rule.onNodeWithTag("nested").captureToImage()
            .asAndroidBitmap()

        topLevelSurfaceBitmap.assertPixels {
            Color(nestedSurfaceBitmap.getPixel(it.x, it.y))
        }
    }

    /**
     * Tests that composed modifiers applied to Surface are applied within the changes to
     * [LocalContentColor], so they can consume the updated values.
     */
    @Test
    fun contentColorSetBeforeModifier() {
        var contentColor: Color = Color.Unspecified
        val expectedColor = Color.Blue
        rule.setMaterialContent(lightColorScheme()) {
            CompositionLocalProvider(LocalContentColor provides Color.Red) {
                Surface(
                    Modifier.composed {
                        contentColor = LocalContentColor.current
                        Modifier
                    },
                    tonalElevation = 2.dp,
                    contentColor = expectedColor,
                    content = {}
                )
            }
        }

        rule.runOnIdle {
            Truth.assertThat(contentColor).isEqualTo(expectedColor)
        }
    }

    @Test
    fun clickable_semantics() {
        val count = mutableStateOf(0)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                onClick = { count.value += 1 },
                modifier = Modifier.testTag("surface"),
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performClick()
            .assertTextEquals("1")
    }

    @Test
    fun clickable_withCustomSemantics() {
        val count = mutableStateOf(0)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                onClick = { count.value += 1 },
                modifier = Modifier.semantics { role = Role.Checkbox }.testTag("surface"),
            ) {
                Text("${count.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Checkbox))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("0")
            .performClick()
            .assertTextEquals("1")
    }

    @Test
    fun clickable_clickAction() {
        val count = mutableStateOf(0f)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                onClick = { count.value += 1 },
                modifier = Modifier.testTag("surface")
            ) { Spacer(Modifier.size(30.dp)) }
        }
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(count.value).isEqualTo(1)

        rule.onNodeWithTag("surface").performClick().performClick()
        Truth.assertThat(count.value).isEqualTo(3)
    }

    @Test
    fun clickable_clickOutsideShapeBounds() {
        val count = mutableStateOf(0f)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                onClick = { count.value += 1 },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(100.dp)) }
        }
        // Click inside the circular shape bounds. Expecting an increase in count.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(count.value).isEqualTo(1)

        // Click outside the circular shape bounds. Expecting unchanged count.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(10f, 10f)) }
        Truth.assertThat(count.value).isEqualTo(1)
    }

    @Test
    fun clickable_smallTouchTarget_clickOutsideShapeBounds() {
        val count = mutableStateOf(0f)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                onClick = { count.value += 1 },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(40.dp)) }
        }
        // Click inside the circular shape bounds. Expecting an increase in count.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(count.value).isEqualTo(1)

        // Click outside the circular shape bounds. Still expecting an increase in count, as the
        // touch target has a minimum size of 48dp.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(2f, 2f)) }
        Truth.assertThat(count.value).isEqualTo(2)
    }

    @Test
    fun clickable_enabled_disabled() {
        val count = mutableStateOf(0f)
        val enabled = mutableStateOf(true)
        val interactionSource = MutableInteractionSource()
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                modifier = Modifier.testTag("surface")
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled.value,
                        onClick = { count.value += 1 },
                    )
            ) { Spacer(Modifier.size(30.dp)) }
        }
        rule.onNodeWithTag("surface")
            .assertIsEnabled()
            .performClick()

        Truth.assertThat(count.value).isEqualTo(1)
        rule.runOnIdle {
            enabled.value = false
        }

        rule.onNodeWithTag("surface")
            .assertIsNotEnabled()
            .performClick()
            .performClick()
        Truth.assertThat(count.value).isEqualTo(1)
    }

    @Test
    fun clickable_interactionSource() {
        val interactionSource = MutableInteractionSource()

        var scope: CoroutineScope? = null

        rule.mainClock.autoAdvance = false

        rule.setContent {
            scope = rememberCoroutineScope()
            Surface(
                onClick = {},
                modifier =
                Modifier.testTag("surface"),
                interactionSource = interactionSource,
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

        rule.onNodeWithTag("surface")
            .performTouchInput { down(center) }

        // Advance past the tap timeout
        rule.mainClock.advanceTimeBy(100)

        rule.runOnIdle {
            Truth.assertThat(interactions).hasSize(1)
            Truth.assertThat(interactions.first()).isInstanceOf(PressInteraction.Press::class.java)
        }

        rule.onNodeWithTag("surface")
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
    fun surface_blockClicksBehind() {
        val state = mutableStateOf(0)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Button(
                    modifier = Modifier.fillMaxSize().testTag("clickable"),
                    onClick = { state.value += 1 }
                ) { Text("button fullscreen") }
                Surface(
                    Modifier.fillMaxSize().testTag("surface"),
                ) {}
            }
        }
        rule.onNodeWithTag("clickable").assertHasClickAction().performClick()
        // still 0
        Truth.assertThat(state.value).isEqualTo(0)
    }

    @Test
    fun selectable_semantics() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface"),
            ) {
                Text("${selected.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performClick()
            .assertTextEquals("true")
    }

    @Test
    fun selectable_customSemantics() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.semantics { role = Role.Switch }.testTag("surface"),
            ) {
                Text("${selected.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performClick()
            .assertTextEquals("true")
    }

    @Test
    fun selectable_clickAction() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface")
            ) { Spacer(Modifier.size(30.dp)) }
        }
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isTrue()

        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isFalse()
    }

    @Test
    fun selectable_clickOutsideShapeBounds() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(100.dp)) }
        }
        // Click inside the circular shape bounds. Expecting a selection change.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isTrue()

        // Click outside the circular shape bounds. Expecting a selection to stay as it.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(10f, 10f)) }
        Truth.assertThat(selected.value).isTrue()
    }

    @Test
    fun selectable_smallTouchTarget_clickOutsideShapeBounds() {
        val selected = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                selected = selected.value,
                onClick = { selected.value = !selected.value },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(40.dp)) }
        }
        // Click inside the circular shape bounds. Expecting a selection change.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(selected.value).isTrue()

        // Click outside the circular shape bounds. Still expecting a selection change, as the
        // touch target has a minimum size of 48dp.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(2f, 2f)) }
        Truth.assertThat(selected.value).isFalse()
    }

    @Test
    fun toggleable_semantics() {
        val toggled = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                checked = toggled.value,
                onCheckedChange = { toggled.value = !toggled.value },
                modifier = Modifier.testTag("surface"),
            ) {
                Text("${toggled.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performClick()
            .assertTextEquals("true")
    }

    @Test
    fun toggleable_customSemantics() {
        val toggled = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                checked = toggled.value,
                onCheckedChange = { toggled.value = !toggled.value },
                modifier = Modifier.semantics { role = Role.Tab }.testTag("surface"),
            ) {
                Text("${toggled.value}")
                Spacer(Modifier.size(30.dp))
            }
        }
        rule.onNodeWithTag("surface")
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsEnabled()
            // since we merge descendants we should have text on the same node
            .assertTextEquals("false")
            .performClick()
            .assertTextEquals("true")
    }

    @Test
    fun toggleable_toggleAction() {
        val toggled = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                checked = toggled.value,
                onCheckedChange = { toggled.value = !toggled.value },
                modifier = Modifier.testTag("surface")
            ) { Spacer(Modifier.size(30.dp)) }
        }
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(toggled.value).isTrue()

        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(toggled.value).isFalse()
    }

    @Test
    fun toggleable_clickOutsideShapeBounds() {
        val checked = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                checked = checked.value,
                onCheckedChange = { checked.value = !checked.value },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(100.dp)) }
        }
        // Click inside the circular shape bounds. Expecting a checked state change.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(checked.value).isTrue()

        // Click outside the circular shape bounds. Expecting the checked state to stay as it.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(10f, 10f)) }
        Truth.assertThat(checked.value).isTrue()
    }

    @Test
    fun toggleable_smallTouchTarget_clickOutsideShapeBounds() {
        val checked = mutableStateOf(false)
        rule.setMaterialContent(lightColorScheme()) {
            Surface(
                checked = checked.value,
                onCheckedChange = { checked.value = !checked.value },
                modifier = Modifier.testTag("surface"),
                shape = CircleShape
            ) { Spacer(Modifier.size(40.dp)) }
        }
        // Click inside the circular shape bounds. Expecting a checked state change.
        rule.onNodeWithTag("surface").performClick()
        Truth.assertThat(checked.value).isTrue()

        // Click outside the circular shape bounds. Still expecting a checked state change, as the
        // touch target has a minimum size of 48dp.
        rule.onNodeWithTag("surface").performTouchInput { click(Offset(2f, 2f)) }
        Truth.assertThat(checked.value).isFalse()
    }

    // regression test for b/189411183
    @Test
    fun surface_allowsFinalPassChildren() {
        val hitTested = mutableStateOf(false)
        rule.setContent {
            Box(Modifier.fillMaxSize()) {
                Surface(
                    Modifier.fillMaxSize().testTag("surface"),
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .testTag("clickable")
                            .pointerInput(Unit) {
                                forEachGesture {
                                    awaitPointerEventScope {
                                        hitTested.value = true
                                        val event = awaitPointerEvent(PointerEventPass.Final)
                                        Truth.assertThat(event.changes[0].isConsumed)
                                            .isFalse()
                                    }
                                }
                            }
                    )
                }
            }
        }
        rule.onNodeWithTag("clickable")
            .performTouchInput {
                down(center)
                up()
            }
        Truth.assertThat(hitTested.value).isTrue()
    }
}
