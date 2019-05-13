/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(ComposeRobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class ObserverMapTests {

    private val node1 = 1
    private val node2 = 2
    private lateinit var map: ObserverMap<TestModel, Int>

    @Before
    fun setup() {
        map = ObserverMap()
    }

    @Test
    fun testMapContainsPreviouslyAddedModel() {
        val model = TestModel()
        map.add(model, node1)

        map.assertNodes(model, node1)
    }

    @Test
    fun testMapAssociateBothNodesWithTheModel() {
        val model = TestModel()
        map.add(model, node1)
        map.add(model, node2)

        map.assertNodes(model, node1, node2)
    }

    @Test
    fun testMapContainsModelWithChangedHashCode() {
        val model = TestModel("Original")
        map.add(model, node1)
        model.content = "Changed"

        map.assertNodes(model, node1)
    }

    @Test
    fun testMapRemovesTheModel() {
        val model = TestModel()
        map.add(model, node1)
        map.add(model, node2)

        map.remove(model)

        map.assertNodes(model)
    }

    @Test
    fun testMapRemovesTheNode() {
        val model = TestModel()
        map.add(model, node1)
        map.add(model, node2)

        map.remove(model, node1)

        map.assertNodes(model, node2)
    }

    @Test
    fun testMapClearsAllTheModels() {
        val model1 = TestModel("Test1")
        val model2 = TestModel("Test2")
        map.add(model1, node1)
        map.add(model2, node2)

        map.clear()

        map.assertNodes(model1)
        map.assertNodes(model2)
    }

    @Test
    fun testMapClearsTheValuesByPredicate() {
        val model1 = TestModel("Test1")
        val model2 = TestModel("Test2")
        val node3 = 3
        map.add(model1, node1)
        map.add(model2, node2)
        map.add(model2, node3)

        map.clearValues { it == node1 || it == node3 }

        map.assertNodes(model1)
        map.assertNodes(model2, node2)
    }

    @Test
    fun testGetForMultipleModels() {
        val model1 = TestModel("Test1")
        val model2 = TestModel("Test2")
        val model3 = TestModel("Test2")
        val node3 = 3
        val node4 = 4
        map.add(model1, node1)
        map.add(model1, node2)
        map.add(model2, node3)
        map.add(model3, node4)

        map.assertNodes(listOf(model1, model2, model3), node1, node2, node3, node4)
    }

    @Test
    fun testGetFiltersDuplicates() {
        val model1 = TestModel("Test1")
        val model2 = TestModel("Test2")
        map.add(model1, node1)
        map.add(model2, node1)

        map.assertNodes(listOf(model1, model2), node1)
    }

    private data class TestModel(var content: String = "Test")

    private fun ObserverMap<TestModel, Int>.assertNodes(
        model: TestModel,
        vararg nodes: Int
    ) {
        assertNodes(listOf(model), *nodes)
    }

    private fun ObserverMap<TestModel, Int>.assertNodes(
        models: List<TestModel>,
        vararg nodes: Int
    ) {
        val expected = nodes.toList().sorted()
        val actual = get(models).sorted()
        assertEquals(expected, actual)
    }
}