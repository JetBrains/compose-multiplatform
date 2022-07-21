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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@UiToolingDataApi
@MediumTest
@RunWith(AndroidJUnit4::class)
class BoundsTest : ToolingTest() {
    fun Group.all(): Collection<Group> =
        listOf(this) + this.children.flatMap { it.all() }

    @Test
    fun testBounds() {
        val slotTableRecord = CompositionDataRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Box {
                    Column(Modifier.padding(10.dp)) {
                        Text("Hello", Modifier.padding(5.dp))
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            val tree = slotTableRecord.store.first().asTree()

            val boundingBoxes = tree.firstOrNull {
                it.location?.sourceFile?.equals("BoundsTest.kt") == true && it.box.right > 0
            }!!
                .all()
                .filter {
                    val name = it.location?.sourceFile
                    name != null && name.equals("BoundsTest.kt")
                }
                .map {
                    it.box.left
                }
                .distinct()
                .sorted()
                .toTypedArray()
            with(Density(activityTestRule.activity)) {
                println(boundingBoxes.contentDeepToString())
                arrayOf(
                    0.dp.roundToPx(), // Root
                    10.dp.roundToPx(), // Column
                    15.dp.roundToPx() // Text
                ).forEachIndexed { index, value ->
                    Assert.assertTrue(boundingBoxes[index] in value - 1..value + 1)
                }
            }
        }
    }

    @Test
    fun testBoundsWithoutParsingParameters() {
        val lefts = mutableMapOf<String, Dp>()
        val slotTableRecord = CompositionDataRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Box {
                    Column(Modifier.padding(10.dp)) {
                        Text("Hello", Modifier.padding(5.dp))
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            slotTableRecord.store.first().mapTree<Any>({ _, context, _ ->
                if (context.location?.sourceFile == "BoundsTest.kt") {
                    with(Density(activityTestRule.activity)) {
                        lefts[context.name!!] = context.bounds.left.toDp()
                    }
                }
            })

            assertThat(lefts["Box"]?.value).isWithin(1f).of(0f)
            assertThat(lefts["Column"]?.value).isWithin(1f).of(10f)
            assertThat(lefts["Text"]?.value).isWithin(0.5f).of(15f)
        }
    }

    @Test
    fun testBoundWithConstraints() {
        val slotTableRecord = CompositionDataRecord.create()
        show {
            Inspectable(slotTableRecord) {
                BoxWithConstraints {
                    Column {
                        Box {
                            Text("Hello")
                        }
                        Box {
                            Text("Hello")
                        }
                    }
                }
            }
        }

        activityTestRule.runOnUiThread {
            val store = slotTableRecord.store
            Assert.assertTrue(store.size > 1)
            val trees = slotTableRecord.store.map { it.asTree() }
            val boundingBoxes = trees.map {
                it.all().filter {
                    it.box.right > 0 && it.location?.sourceFile == "BoundsTest.kt"
                }
            }.flatten().groupBy { it.location }

            Assert.assertTrue(boundingBoxes.size >= 4)
        }
    }

    @Test
    @LargeTest
    fun testDisposeWithComposeTables() {
        val slotTableRecord = CompositionDataRecord.create()
        var value by mutableStateOf(0)
        var latch = CountDownLatch(1)
        show {
            Inspectable(slotTableRecord) {
                key(value) {
                    BoxWithConstraints {
                        requireNotNull(LocalDensity.current)
                        Text("Hello")
                    }
                }
            }
            latch.countDown()
        }

        activityTestRule.runOnUiThread {
            latch = CountDownLatch(1)
            value = 1
        }
        latch.await(1, TimeUnit.SECONDS)

        activityTestRule.runOnUiThread {
            latch = CountDownLatch(1)
            value = 2
        }
        latch.await(1, TimeUnit.SECONDS)

        Assert.assertTrue(slotTableRecord.store.size < 3)
    }
}
