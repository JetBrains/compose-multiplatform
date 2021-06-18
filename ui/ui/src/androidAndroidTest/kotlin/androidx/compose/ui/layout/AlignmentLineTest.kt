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

package androidx.compose.ui.layout

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import kotlin.math.min
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class AlignmentLineTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun queryingLinesOfUnmeasuredChild() {
        val root = root {
            queryAlignmentLineDuringMeasure()
            add(
                node {
                    doNotMeasure()
                    add(node())
                }
            )
        }

        createDelegate(root)
        assertMeasuredAndLaidOut(root)
    }

    @Test
    fun alignmentLinesPositionInCooperation_whenModifierDisobeys() {
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        val hLinePosition = 50
        val vLinePosition = 150
        val constrainedSize = 100
        val actualSize = 200
        rule.setContent {
            val contentWithAlignmentLines = @Composable {
                Box(Modifier.requiredSize(with(rule.density) { actualSize.toDp() })) {
                    Layout({}, Modifier) { _, _ ->
                        layout(0, 0, mapOf(hLine to hLinePosition, vLine to vLinePosition)) {}
                    }
                }
            }
            Layout(contentWithAlignmentLines) { measurables, _ ->
                val placeable = measurables.first().measure(
                    Constraints(maxWidth = constrainedSize, maxHeight = constrainedSize)
                )
                val obtainedHLinePosition = placeable[hLine]
                val obtainedVLinePosition = placeable[vLine]
                assertEquals(
                    hLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedHLinePosition
                )
                assertEquals(
                    vLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedVLinePosition
                )
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun alignmentLinesPositionInCooperation_whenLayoutDisobeys() {
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        val hLinePosition = 50
        val vLinePosition = 150
        val constrainedSize = 100
        val actualSize = 200
        rule.setContent {
            val contentWithAlignmentLines = @Composable {
                Layout({}, Modifier) { _, _ ->
                    layout(
                        actualSize,
                        actualSize,
                        mapOf(hLine to hLinePosition, vLine to vLinePosition)
                    ) {}
                }
            }
            Layout(contentWithAlignmentLines) { measurables, _ ->
                val placeable = measurables.first().measure(
                    Constraints(maxWidth = constrainedSize, maxHeight = constrainedSize)
                )
                val obtainedHLinePosition = placeable[hLine]
                val obtainedVLinePosition = placeable[vLine]
                assertEquals(
                    hLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedHLinePosition
                )
                assertEquals(
                    vLinePosition - (actualSize - constrainedSize) / 2,
                    obtainedVLinePosition
                )
                layout(0, 0) {}
            }
        }
        rule.waitForIdle()
    }

    @Test
    fun alignmentLinesArePropagated_whenSuppliedViaModifier() {
        val size = 50
        val sizeDp = with(rule.density) { size.toDp() }
        val linePosition = 25
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        rule.setContent {
            val content = @Composable {
                Box(Modifier.size(sizeDp)) {
                    Box(
                        Modifier.supplyAlignmentLines {
                            mapOf(
                                hLine to linePosition,
                                vLine to linePosition
                            )
                        }.size(sizeDp)
                    )
                }
            }

            Layout(content) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                assertEquals(linePosition, placeable[hLine])
                assertEquals(linePosition, placeable[vLine])
                layout(0, 0) {}
            }
        }
    }

    @Test
    fun alignmentLinesArePropagated_whenSuppliedViaModifier_withCorrectPosition() {
        val size = 50
        val sizeDp = with(rule.density) { size.toDp() }
        val offset = 10
        val offsetDp = with(rule.density) { offset.toDp() }
        val linePosition = 25
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        rule.setContent {
            val content = @Composable {
                Box(Modifier.size(sizeDp)) {
                    Box(
                        Modifier.offset(offsetDp, offsetDp)
                            .supplyAlignmentLines {
                                mapOf(
                                    hLine to linePosition,
                                    vLine to linePosition
                                )
                            }.size(sizeDp)
                            .offset(offsetDp, offsetDp)
                    )
                }
            }

            Layout(content) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                assertEquals(linePosition + offset, placeable[hLine])
                assertEquals(linePosition + offset, placeable[vLine])
                layout(0, 0) {}
            }
        }
    }

    @Test
    fun alignmentLinesChangeCausesRemeasure_whenSuppliedViaModifier() {
        val size = 50
        val sizeDp = with(rule.density) { size.toDp() }
        val offset = 10
        val linePosition = 25
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        val alignmentLines = mutableStateMapOf(hLine to linePosition, vLine to linePosition)
        var obtainedHLinePosition = -1
        var obtainedVLinePosition = -1
        rule.setContent {
            val content = @Composable {
                Box(Modifier.size(sizeDp)) {
                    Box(Modifier.supplyAlignmentLines { alignmentLines.toMap() }.size(sizeDp))
                }
            }

            Layout(content) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                obtainedHLinePosition = placeable[hLine]
                obtainedVLinePosition = placeable[vLine]
                layout(0, 0) {}
            }
        }

        rule.runOnIdle {
            assertEquals(linePosition, obtainedHLinePosition)
            assertEquals(linePosition, obtainedVLinePosition)
            alignmentLines[hLine] = linePosition + offset
            alignmentLines[vLine] = linePosition + offset
        }

        rule.runOnIdle {
            assertEquals(linePosition + offset, obtainedHLinePosition)
            assertEquals(linePosition + offset, obtainedVLinePosition)
        }
    }

    @Test
    fun alignmentLinesChangeCausesRemeasure_whenSuppliedViaLayout() {
        val size = 50
        val sizeDp = with(rule.density) { size.toDp() }
        val offset = 10
        val linePosition = 25
        val hLine = HorizontalAlignmentLine(::min)
        val vLine = VerticalAlignmentLine(::min)
        val alignmentLines = mutableStateMapOf(hLine to linePosition, vLine to linePosition)
        var obtainedHLinePosition = -1
        var obtainedVLinePosition = -1
        rule.setContent {
            val content = @Composable {
                val innerContent = @Composable {
                    Layout({}) { _, _ ->
                        layout(size, size, alignmentLines) {}
                    }
                }
                Layout(content = innerContent, Modifier.size(sizeDp)) { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                }
            }

            Layout(content) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                obtainedHLinePosition = placeable[hLine]
                obtainedVLinePosition = placeable[vLine]
                layout(0, 0) {}
            }
        }

        rule.runOnIdle {
            assertEquals(linePosition, obtainedHLinePosition)
            assertEquals(linePosition, obtainedVLinePosition)
            alignmentLines[hLine] = linePosition + offset
            alignmentLines[vLine] = linePosition + offset
        }

        rule.runOnIdle {
            assertEquals(linePosition + offset, obtainedHLinePosition)
            assertEquals(linePosition + offset, obtainedVLinePosition)
        }
    }

    @Test
    fun scenario1() {
        var parentMeasures = 0
        var measures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }) {
                Parent(onMeasure = { ++measures }, readDuringMeasure = true) {
                    Parent {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(2, measures)
        }
    }

    @Test
    fun scenario2() {
        var parentLayouts = 0
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(onLayout = { ++parentLayouts }) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutBeforePlacing = true
                ) {
                    Parent {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario3() {
        var parentLayouts = 0
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(onLayout = { ++parentLayouts }) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutAfterPlacing = true
                ) {
                    Parent {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario4() {
        var parentMeasures = 0
        var parentLayouts = 0
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(
                onMeasure = { ++parentMeasures },
                onLayout = { ++parentLayouts },
                readDuringMeasure = true
            ) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutBeforePlacing = true
                ) {
                    Parent {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(2, parentMeasures)
            assertEquals(2, parentLayouts)
            assertEquals(1, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario5() {
        var parentMeasures = 0
        var parentLayouts = 0
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(
                onMeasure = { ++parentMeasures },
                onLayout = { ++parentLayouts },
                readDuringMeasure = true
            ) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutAfterPlacing = true
                ) {
                    Parent {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(2, parentMeasures)
            assertEquals(2, parentLayouts)
            assertEquals(1, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario6() {
        var parentMeasures = 0
        var parentLayouts = 0
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(
                onMeasure = { ++parentMeasures },
                onLayout = { ++parentLayouts },
                readDuringLayoutAfterPlacing = true
            ) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringMeasure = true
                ) {
                    Parent {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(2, parentLayouts)
            assertEquals(2, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario7() {
        var parentMeasures = 0
        var measures = 0
        var layouts = 0
        var childMeasures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringMeasure = true
                ) {
                    Parent(modifier = Modifier.provider(), onMeasure = { ++childMeasures }) {
                        Parent()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(2, measures)
            assertEquals(2, layouts)
            assertEquals(2, childMeasures)
        }
    }

    @Test
    fun scenario8() {
        var parentMeasures = 0
        var measures = 0
        var layouts = 0
        var childMeasures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutBeforePlacing = true
                ) {
                    Parent(modifier = Modifier.provider(), onMeasure = { ++childMeasures }) {
                        Parent()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(2, layouts)
            assertEquals(2, childMeasures)
        }
    }

    @Test
    fun scenario9() {
        var parentMeasures = 0
        var measures = 0
        var layouts = 0
        var childMeasures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutAfterPlacing = true
                ) {
                    Parent(modifier = Modifier.provider(), onMeasure = { ++childMeasures }) {
                        Parent()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(2, layouts)
            assertEquals(2, childMeasures)
        }
    }

    @Test
    fun scenario10() {
        var parentMeasures = 0
        var measures = 0
        var layouts = 0
        var childMeasures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }, readDuringMeasure = true) {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutAfterPlacing = true
                ) {
                    Parent(modifier = Modifier.provider(), onMeasure = { ++childMeasures }) {
                        Parent()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, layouts)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(2, parentMeasures)
            assertEquals(1, measures)
            assertEquals(2, layouts)
            assertEquals(2, childMeasures)
        }
    }

    @Test
    fun scenario11() {
        var measures = 0
        var layouts = 0
        var childMeasures = 0
        rule.setContent {
            Parent {
                Parent(
                    onMeasure = { ++measures },
                    onLayout = { ++layouts },
                    readDuringLayoutAfterPlacing = true
                ) {
                    Parent(
                        modifier = Modifier.reader(readDuringMeasure = true),
                        onMeasure = { ++childMeasures }
                    ) {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(1, layouts)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(2, layouts)
            assertEquals(2, childMeasures)
        }
    }

    @Test
    fun scenario12() {
        var childMeasures = 0
        rule.setContent {
            Parent {
                Provider(
                    modifier = Modifier.reader(readDuringMeasure = true),
                    onMeasure = { ++childMeasures }
                )
            }
        }
        rule.runOnIdle {
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(2, childMeasures)
        }
    }

    @Test
    fun scenario13() {
        var measures = 0
        var childMeasures = 0
        var childLayouts = 0
        rule.setContent {
            Parent(onMeasure = { ++measures }) {
                Provider(
                    modifier = Modifier.reader(readDuringLayoutBeforePlacing = true),
                    onMeasure = { ++childMeasures },
                    onLayout = { ++childLayouts }
                )
            }
        }
        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(1, childMeasures)
            assertEquals(1, childLayouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(2, childMeasures)
            assertEquals(2, childLayouts)
        }
    }

    @Test
    fun scenario14() {
        var measures = 0
        var childMeasures = 0
        var childLayouts = 0
        rule.setContent {
            Parent(onMeasure = { ++measures }) {
                Provider(
                    modifier = Modifier.reader(readDuringLayoutAfterPlacing = true),
                    onMeasure = { ++childMeasures },
                    onLayout = { ++childLayouts }
                )
            }
        }
        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(1, childMeasures)
            assertEquals(1, childLayouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(2, childMeasures)
            assertEquals(2, childLayouts)
        }
    }

    @Test
    fun scenario15() {
        var parentMeasures = 0
        var measures = 0
        var childMeasures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }) {
                Parent(
                    modifier = Modifier.reader(readDuringMeasure = true).provider(),
                    onMeasure = { ++measures }
                ) {
                    Parent(onMeasure = { ++childMeasures })
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(2, measures)
            assertEquals(1, childMeasures)
        }
    }

    @Test
    fun scenario16() {
        var parentMeasures = 0
        var measures = 0
        var childMeasures = 0
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }, readDuringMeasure = true) {
                Parent(
                    modifier = Modifier.reader(readDuringMeasure = true).provider(),
                    onMeasure = { ++measures }
                ) {
                    Parent(onMeasure = { ++childMeasures })
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(2, parentMeasures)
            assertEquals(2, measures)
            assertEquals(1, childMeasures)
        }
    }

    @Test
    fun scenario17() {
        var parentMeasures = 0
        var measures = 0
        var childMeasures = 0
        var read by mutableStateOf(true)
        rule.setContent {
            Parent(onMeasure = { ++parentMeasures }, readDuringMeasure = true) {
                ChangingParent(
                    onMeasure = { ++measures },
                    readDuringMeasure = { read }
                ) {
                    Parent(onMeasure = { ++childMeasures }) {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, measures)
            assertEquals(1, childMeasures)
            changeLinePosition()
            read = false
        }

        rule.runOnIdle {
            assertEquals(2, parentMeasures)
            assertEquals(2, measures)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(3, parentMeasures)
            assertEquals(2, measures)
            assertEquals(1, childMeasures)
        }
    }

    @Test
    fun scenario18() {
        var parentLayouts = 0
        var parentMeasures = 0
        var measures = 0
        var childMeasures = 0
        var read by mutableStateOf(true)
        rule.setContent {
            Parent(
                onMeasure = { ++parentMeasures },
                onLayout = { ++parentLayouts },
                readDuringLayoutAfterPlacing = true
            ) {
                ChangingParent(
                    onMeasure = { ++measures },
                    readDuringMeasure = { read }
                ) {
                    Parent(onMeasure = { ++childMeasures }) {
                        Provider()
                    }
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(1, parentLayouts)
            assertEquals(1, measures)
            assertEquals(1, childMeasures)
            changeLinePosition()
            read = false
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(2, parentLayouts)
            assertEquals(2, measures)
            assertEquals(1, childMeasures)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, parentMeasures)
            assertEquals(3, parentLayouts)
            assertEquals(2, measures)
            assertEquals(1, childMeasures)
        }
    }

    @Test
    fun scenario19() {
        var offset by mutableStateOf(IntOffset.Zero)
        rule.setContent {
            Parent(readDuringLayoutBeforePlacing = true) {
                Provider(modifier = Modifier.offset { offset })
            }
        }
        rule.runOnIdle {
            offset = IntOffset(10, 10)
            linePosition += 10
        }

        rule.waitForIdle()
    }

    @Test
    fun scenario20() {
        var parentLayouts = 0
        var offset by mutableStateOf(IntOffset.Zero)
        rule.setContent {
            Parent(readDuringLayoutBeforePlacing = true, onLayout = { ++parentLayouts }) {
                Parent {
                    Provider(modifier = Modifier.offset { offset })
                }
            }
        }
        rule.runOnIdle {
            offset = IntOffset(10, 10)
            linePosition += 10
        }

        rule.runOnIdle {
            assertEquals(2, parentLayouts)
        }

        rule.waitForIdle()
    }

    @Test
    fun scenario21() {
        var parentMeasures = 0
        var read by mutableStateOf(false)
        rule.setContent {
            ChangingParent(
                readDuringMeasure = { read },
                onMeasure = { ++parentMeasures }
            ) {
                Parent {
                    Provider()
                }
            }
        }
        rule.runOnIdle {
            read = true
        }

        rule.runOnIdle {
            assertEquals(2, parentMeasures)
        }
    }

    @Test
    fun scenario22() {
        var parentLayouts = 0
        var read by mutableStateOf(false)
        rule.setContent {
            ChangingParent(
                readDuringLayoutBeforePlacing = { read },
                onLayout = { ++parentLayouts }
            ) {
                Parent {
                    Provider()
                }
            }
        }
        rule.runOnIdle {
            read = true
        }

        rule.runOnIdle {
            assertEquals(2, parentLayouts)
        }
    }

    @Test
    fun scenario23() {
        var obtainedPosition = 0
        var changingState by mutableStateOf(false)
        rule.setContent {
            Layout(
                content = {
                    Parent {
                        Provider()
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    if (changingState) require(true)
                    obtainedPosition = placeable[TestLine]
                    placeable.place(0, 0)
                }
            }
        }
        rule.runOnIdle {
            assertEquals(linePosition, obtainedPosition)
            changeLinePosition()
            changingState = true
        }

        rule.runOnIdle {
            assertEquals(linePosition, obtainedPosition)
        }
    }

    @Test
    fun scenario24() {
        var obtainedPosition = 0
        var changingState by mutableStateOf(false)
        rule.setContent {
            Layout(
                content = {
                    Parent {
                        Provider()
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                layout(constraints.maxWidth, constraints.maxHeight) {
                    if (changingState) require(true)
                    placeable.place(0, 0)
                    obtainedPosition = placeable[TestLine]
                }
            }
        }
        rule.runOnIdle {
            assertEquals(linePosition, obtainedPosition)
            changeLinePosition()
            changingState = true
        }

        rule.runOnIdle {
            assertEquals(linePosition, obtainedPosition)
        }
    }

    @Test
    fun scenario25() {
        var obtainedPosition = 0
        rule.setContent {
            Parent(modifier = Modifier.onGloballyPositioned { obtainedPosition = it[TestLine] }) {
                Parent {
                    Provider()
                }
            }
        }
        rule.runOnIdle {
            assertEquals(linePosition, obtainedPosition)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(linePosition, obtainedPosition)
        }
    }

    @Test
    fun scenario26() {
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(
                modifier = Modifier.reader(
                    readDuringMeasure = true,
                    readDuringLayoutBeforePlacing = true,
                    onMeasure = { ++measures },
                    onLayout = { ++layouts }
                )
            ) {
                Parent {
                    Provider()
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(2, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario27() {
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(
                modifier = Modifier.reader(
                    readDuringLayoutBeforePlacing = true,
                    onMeasure = { ++measures },
                    onLayout = { ++layouts }
                )
            ) {
                Parent {
                    Provider()
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun scenario28() {
        var measures = 0
        var layouts = 0
        rule.setContent {
            Parent(
                modifier = Modifier.reader(
                    readDuringLayoutAfterPlacing = true,
                    onMeasure = { ++measures },
                    onLayout = { ++layouts }
                )
            ) {
                Parent {
                    Provider()
                }
            }
        }
        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(1, layouts)
            changeLinePosition()
        }

        rule.runOnIdle {
            assertEquals(1, measures)
            assertEquals(2, layouts)
        }
    }

    @Test
    fun notMeasuredChildIsNotCrashingWhenGrandParentQueriesAlignments() {
        var emit by mutableStateOf(false)

        rule.setContent {
            Layout(
                content = {
                    Layout(
                        content = {
                            if (emit) {
                                Box(Modifier.size(10.dp))
                            }
                        }
                    ) { _, constraints ->
                        layout(constraints.maxWidth, constraints.maxHeight) {}
                    }
                }
            ) { measurables, constraints ->
                val placeable = measurables.first().measure(constraints)
                placeable[FirstBaseline]
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }

        rule.runOnIdle {
            emit = true
        }

        rule.runOnIdle {}
    }

    private var linePosition = 10
    private var linePositionState by mutableStateOf(10)
    private fun changeLinePosition() {
        linePosition += 10
        linePositionState += 10
    }
    private val TestLine = HorizontalAlignmentLine(::min)

    @Composable
    private fun Parent(
        modifier: Modifier = Modifier,
        onMeasure: () -> Unit = {},
        onLayout: () -> Unit = {},
        readDuringMeasure: Boolean = false,
        readDuringLayoutBeforePlacing: Boolean = false,
        readDuringLayoutAfterPlacing: Boolean = false,
        content: @Composable () -> Unit = {}
    ) {
        ChangingParent(
            modifier,
            onMeasure,
            onLayout,
            { readDuringMeasure },
            { readDuringLayoutBeforePlacing },
            { readDuringLayoutAfterPlacing },
            content
        )
    }

    @Composable
    private fun ChangingParent(
        modifier: Modifier = Modifier,
        onMeasure: () -> Unit = {},
        onLayout: () -> Unit = {},
        readDuringMeasure: () -> Boolean = { false },
        readDuringLayoutBeforePlacing: () -> Boolean = { false },
        readDuringLayoutAfterPlacing: () -> Boolean = { false },
        content: @Composable () -> Unit = {}
    ) {
        Layout(content, modifier) { measurables, constraints ->
            onMeasure()
            val placeables = measurables.map {
                it.measure(constraints).also {
                    if (readDuringMeasure()) assertEquals(linePosition, it[TestLine])
                }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                onLayout()
                placeables.forEach { placeable ->
                    if (readDuringLayoutBeforePlacing()) {
                        // placeable[TestLine]
                        assertEquals(linePosition, placeable[TestLine])
                    }
                    placeable.place(0, 0)
                    if (readDuringLayoutAfterPlacing()) {
                        // placeable[TestLine]
                        assertEquals(linePosition, placeable[TestLine])
                    }
                }
            }
        }
    }

    @Composable
    private fun Provider(
        modifier: Modifier = Modifier,
        onMeasure: () -> Unit = {},
        onLayout: () -> Unit = {},
        content: @Composable () -> Unit = {}
    ) {
        Layout(content, modifier) { _, constraints ->
            onMeasure()
            layout(
                constraints.maxWidth,
                constraints.maxHeight,
                mapOf(TestLine to linePositionState)
            ) {
                onLayout()
            }
        }
    }

    private fun Modifier.reader(
        onMeasure: () -> Unit = {},
        onLayout: () -> Unit = {},
        readDuringMeasure: Boolean = false,
        readDuringLayoutBeforePlacing: Boolean = false,
        readDuringLayoutAfterPlacing: Boolean = false
    ) = this.then(
        ReaderModifier(
            onMeasure,
            onLayout,
            readDuringMeasure,
            readDuringLayoutBeforePlacing,
            readDuringLayoutAfterPlacing
        )
    )
    private inner class ReaderModifier(
        val onMeasure: () -> Unit,
        val onLayout: () -> Unit,
        val readDuringMeasure: Boolean,
        val readDuringLayoutBeforePlacing: Boolean,
        val readDuringLayoutAfterPlacing: Boolean
    ) : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            onMeasure()
            val placeable = measurable.measure(constraints)
            if (readDuringMeasure) assertEquals(linePosition, placeable[TestLine])
            return layout(constraints.maxWidth, constraints.maxHeight) {
                onLayout()
                if (readDuringLayoutBeforePlacing) assertEquals(linePosition, placeable[TestLine])
                placeable.place(0, 0)
                if (readDuringLayoutAfterPlacing) assertEquals(linePosition, placeable[TestLine])
            }
        }
    }

    private fun Modifier.provider() = this.then(ProviderModifier())
    private inner class ProviderModifier : LayoutModifier {
        override fun MeasureScope.measure(
            measurable: Measurable,
            constraints: Constraints
        ): MeasureResult {
            val placeable = measurable.measure(constraints)
            return layout(
                constraints.maxWidth,
                constraints.maxHeight,
                mapOf(TestLine to linePositionState)
            ) {
                placeable.place(0, 0)
            }
        }

        override fun hashCode(): Int {
            return 0
        }

        override fun equals(other: Any?): Boolean {
            return other is ProviderModifier
        }
    }

    private fun Modifier.supplyAlignmentLines(alignmentLines: () -> Map<AlignmentLine, Int>) =
        layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height, alignmentLines()) {
                placeable.place(0, 0)
            }
        }
}