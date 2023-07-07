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

package androidx.compose.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AndroidOwnerExtraAssertionsRule
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TestActivity
import androidx.compose.ui.test.assertLeftPositionInRootIsEqualTo
import androidx.compose.ui.test.assertTopPositionInRootIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class MultiContentLayoutTest {

    @get:Rule
    val rule = createAndroidComposeRule<TestActivity>()

    @get:Rule
    val excessiveAssertions = AndroidOwnerExtraAssertionsRule()

    var size: Dp = Dp.Unspecified

    @Before
    fun before() {
        size = with(rule.density) { 10.toDp() }
    }

    @Test
    fun haveOneSlotWithOneItem() {
        rule.setContent {
            val first = @Composable {
                Item(0)
            }
            Layout(
                contents = listOf(first),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(1)
                layoutAsRow(constraints, firstSlot)
            }
        }

        assertItemsLaidOutAsRow(0..0)
    }

    @Test
    fun haveOneSlotWithNoItems() {
        rule.setContent {
            val first = @Composable {}
            Layout(
                contents = listOf(first),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(0)
                layoutAsRow(constraints, firstSlot)
            }
        }
    }

    @Test
    fun haveOneSlotWithTwoItems() {
        rule.setContent {
            val first = @Composable {
                Item(0)
                Item(1)
            }
            Layout(
                contents = listOf(first),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(2)
                layoutAsRow(constraints, firstSlot)
            }
        }

        assertItemsLaidOutAsRow(0..1)
    }

    @Test
    fun haveTwoSlotsWithOneItem() {
        rule.setContent {
            val first = @Composable {
                Item(0)
            }
            val second = @Composable {
                Item(1)
            }
            Layout(
                contents = listOf(first, second),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot, secondSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(1)
                assertThat(secondSlot.size).isEqualTo(1)
                layoutAsRow(constraints, firstSlot + secondSlot)
            }
        }

        assertItemsLaidOutAsRow(0..1)
    }

    @Test
    fun haveTwoSlotsWithNoItemsInOne() {
        rule.setContent {
            val first = @Composable {
                Item(0)
            }
            val second = @Composable {}
            Layout(
                contents = listOf(first, second),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot, secondSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(1)
                assertThat(secondSlot.size).isEqualTo(0)
                layoutAsRow(constraints, firstSlot + secondSlot)
            }
        }

        assertItemsLaidOutAsRow(0..0)
    }

    @Test
    fun haveTwoSlotsWithDifferentNumberOfItems() {
        rule.setContent {
            val first = @Composable {
                Item(0)
                Item(1)
            }
            val second = @Composable {
                Item(2)
            }
            Layout(
                contents = listOf(first, second),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot, secondSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(2)
                assertThat(secondSlot.size).isEqualTo(1)
                layoutAsRow(constraints, firstSlot + secondSlot)
            }
        }

        assertItemsLaidOutAsRow(0..2)
    }

    @Test
    fun haveFiveSlots() {
        rule.setContent {
            val first = @Composable {
                Item(0)
            }
            val second = @Composable {
                Item(1)
            }
            val third = @Composable {
                Item(2)
                Item(3)
            }
            val fourth = @Composable {}
            val fifth = @Composable {
                Item(4)
            }
            Layout(
                contents = listOf(first, second, third, fourth, fifth),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot, secondSlot, thirdSlot, fourthSlot, fifthSlot), constraints ->
                assertThat(firstSlot.size).isEqualTo(1)
                assertThat(secondSlot.size).isEqualTo(1)
                assertThat(thirdSlot.size).isEqualTo(2)
                assertThat(fourthSlot.size).isEqualTo(0)
                assertThat(fifthSlot.size).isEqualTo(1)
                layoutAsRow(
                    constraints,
                    firstSlot + secondSlot + thirdSlot + fourthSlot + fifthSlot
                )
            }
        }

        assertItemsLaidOutAsRow(0..4)
    }

    @Test
    fun updatingItemCount() {
        var itemCount by mutableStateOf(1)
        rule.setContent {
            val first = @Composable {
                repeat(itemCount) {
                    Item(it)
                }
            }
            Layout(
                contents = listOf(first),
                modifier = Modifier.size(100.dp)
            ) { (firstSlot), constraints ->
                layoutAsRow(constraints, firstSlot)
            }
        }

        assertItemsLaidOutAsRow(0..0)

        rule.runOnIdle {
            itemCount = 3
        }

        assertItemsLaidOutAsRow(0..2)
    }

    @Test
    fun updatingSlotCount() {
        var slotCount by mutableStateOf(1)
        rule.setContent {
            val contents = buildList<@Composable () -> Unit> {
                repeat(slotCount) {
                    add {
                        Item(it)
                    }
                }
            }
            Layout(
                contents = contents,
                modifier = Modifier.size(100.dp)
            ) { slots, constraints ->
                assertThat(slots.size).isEqualTo(contents.size)
                layoutAsRow(constraints, slots.flatten())
            }
        }

        assertItemsLaidOutAsRow(0..0)

        rule.runOnIdle {
            slotCount = 3
        }

        assertItemsLaidOutAsRow(0..2)
    }

    @Test
    fun defaultIntrinsics() {
        rule.setContent {
            Layout({
                val first = @Composable {
                    BoxWithIntrinsics(1, 2, 100, 200)
                    BoxWithIntrinsics(4, 3, 300, 400)
                }
                val second = @Composable {
                    BoxWithIntrinsics(10, 11, 12, 13)
                }
                Layout(
                    contents = listOf(first, second)
                ) { (_, secondSlot), constraints ->
                    val placeable = secondSlot.first().measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            }) { measurables, _ ->
                val box = measurables[0]
                assertThat(box.minIntrinsicWidth(1000)).isEqualTo(10)
                assertThat(box.minIntrinsicHeight(1000)).isEqualTo(11)
                assertThat(box.maxIntrinsicWidth(1000)).isEqualTo(12)
                assertThat(box.maxIntrinsicHeight(1000)).isEqualTo(13)
                layout(10, 10) { }
            }
        }
    }

    @Test
    fun customIntrinsics() {
        rule.setContent {
            Layout({
                val first = @Composable {
                    BoxWithIntrinsics(1, 2, 100, 200)
                    BoxWithIntrinsics(4, 3, 300, 400)
                }
                val second = @Composable {
                    BoxWithIntrinsics(10, 11, 12, 13)
                }
                Layout(
                    contents = listOf(first, second),
                    measurePolicy = object : MultiContentMeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<List<Measurable>>,
                            constraints: Constraints
                        ) = throw IllegalStateException("shouldn't be called")

                        override fun IntrinsicMeasureScope.minIntrinsicWidth(
                            measurables: List<List<IntrinsicMeasurable>>,
                            height: Int
                        ): Int = measurables[1].first().minIntrinsicWidth(height)

                        override fun IntrinsicMeasureScope.minIntrinsicHeight(
                            measurables: List<List<IntrinsicMeasurable>>,
                            width: Int
                        ): Int = measurables[1].first().minIntrinsicHeight(width)

                        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                            measurables: List<List<IntrinsicMeasurable>>,
                            height: Int
                        ): Int = measurables[1].first().maxIntrinsicWidth(height)

                        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
                            measurables: List<List<IntrinsicMeasurable>>,
                            width: Int
                        ): Int = measurables[1].first().maxIntrinsicHeight(width)
                    }
                )
            }) { measurables, _ ->
                val box = measurables[0]
                assertThat(box.minIntrinsicWidth(1000)).isEqualTo(10)
                assertThat(box.minIntrinsicHeight(1000)).isEqualTo(11)
                assertThat(box.maxIntrinsicWidth(1000)).isEqualTo(12)
                assertThat(box.maxIntrinsicHeight(1000)).isEqualTo(13)
                layout(10, 10) { }
            }
        }
    }

    private fun assertItemsLaidOutAsRow(intRange: IntRange) {
        var currentX = 0.dp
        intRange.forEach {
            rule.onNodeWithTag("$it")
                .assertLeftPositionInRootIsEqualTo(currentX)
                .assertTopPositionInRootIsEqualTo(0.dp)
            currentX += size
        }

        rule.onNodeWithTag("${intRange.first - 1}")
            .assertDoesNotExist()
        rule.onNodeWithTag("${intRange.last + 1}")
            .assertDoesNotExist()
    }

    @Composable
    fun Item(id: Int) {
        Box(
            Modifier
                .size(size)
                .testTag("$id"))
    }
}

private fun MeasureScope.layoutAsRow(
    constraints: Constraints,
    list: List<Measurable>
): MeasureResult {
    val childConstraints =
        Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight)
    return layout(constraints.maxWidth, constraints.maxHeight) {
        var currentX = 0
        list.forEach {
            val placeable = it.measure(childConstraints)
            placeable.place(currentX, 0)
            currentX += placeable.width
        }
    }
}

@Composable
private fun BoxWithIntrinsics(minWidth: Int, minHeight: Int, maxWidth: Int, maxHeight: Int) {
    Layout(measurePolicy = object : MeasurePolicy {
        override fun MeasureScope.measure(
            measurables: List<Measurable>,
            constraints: Constraints
        ): MeasureResult {
            TODO("Not yet implemented")
        }

        override fun IntrinsicMeasureScope.minIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = minWidth

        override fun IntrinsicMeasureScope.minIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = minHeight

        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
            measurables: List<IntrinsicMeasurable>,
            height: Int
        ) = maxWidth

        override fun IntrinsicMeasureScope.maxIntrinsicHeight(
            measurables: List<IntrinsicMeasurable>,
            width: Int
        ) = maxHeight
    })
}
