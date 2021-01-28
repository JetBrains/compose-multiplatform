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

package androidx.compose.ui.tooling.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.DrawModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.GraphicLayerInfo
import androidx.compose.ui.layout.LayoutModifier
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UiToolingDataApi
@MediumTest
@RunWith(AndroidJUnit4::class)
class ModifierInfoTest : ToolingTest() {
    fun Group.all(): Collection<Group> =
        listOf(this) + this.children.flatMap { it.all() }

    @Test
    fun testBounds() {
        val slotTableRecord = CompositionDataRecord.create()
        show {
            Inspectable(slotTableRecord) {
                with(LocalDensity.current) {
                    val px10 = 10f.toDp()
                    val px5 = 5f.toDp()
                    Box {
                        Column(
                            Modifier.padding(px10).graphicsLayer().background(color = Color.Blue)
                        ) {
                            Box(Modifier.padding(px5).size(px5))
                        }
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            val tree = slotTableRecord.store.first().asTree()
            val firstGroup = tree.firstOrNull {
                it.location?.sourceFile?.equals("ModifierInfoTest.kt") == true && it.box.right > 0
            }!!
            val modifierInfoItems = firstGroup.all()
                .filter { it.modifierInfo.isNotEmpty() }
                .sortedBy { it.modifierInfo.size }

            val modifierInfo = modifierInfoItems.map {
                it.modifierInfo
            }

            assertEquals(2, modifierInfo.size)

            val boxModifierInfo = modifierInfo[0]
            assertEquals(2, boxModifierInfo.size)
            assertTrue(
                "Box should only have LayoutModifiers, but the first was " +
                    "${boxModifierInfo[0].modifier}",
                boxModifierInfo[0].modifier is LayoutModifier
            )
            assertEquals(10f, boxModifierInfo[0].coordinates.positionInRoot().x)

            assertTrue(
                "Box should only have LayoutModifiers, but the second was " +
                    "${boxModifierInfo[1].modifier}",
                boxModifierInfo[1].modifier is LayoutModifier
            )
            assertEquals(15f, boxModifierInfo[1].coordinates.positionInRoot().x)

            val columnModifierInfo = modifierInfo[1]
            assertEquals(3, columnModifierInfo.size)
            assertTrue(
                "The first modifier in the column should be a LayoutModifier" +
                    "but was ${columnModifierInfo[0].modifier}",
                columnModifierInfo[0].modifier is LayoutModifier
            )
            assertEquals(0f, columnModifierInfo[0].coordinates.positionInRoot().x)
            assertTrue(
                "The second modifier in the column should be a LayoutModifier" +
                    "but was ${columnModifierInfo[1].modifier}",
                columnModifierInfo[1].modifier is LayoutModifier
            )
            assertTrue(columnModifierInfo[2].extra is GraphicLayerInfo)
            assertEquals(10f, columnModifierInfo[1].coordinates.positionInRoot().x)
            assertTrue(
                "The third modifier in the column should be a DrawModifier" +
                    "but was ${columnModifierInfo[2].modifier}",
                columnModifierInfo[2].modifier is DrawModifier
            )
            assertEquals(10f, columnModifierInfo[2].coordinates.positionInRoot().x)
        }
    }
}
