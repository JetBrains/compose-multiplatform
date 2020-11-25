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

package androidx.compose.ui.tooling

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.material.Button
import androidx.compose.material.ModalDrawerLayout
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.SlotTable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class InspectableTests : ToolingTest() {
    @Test
    fun simpleInspection() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Column {
                    Box(
                        Modifier.preferredSize(100.dp).drawBehind {
                            drawRect(Color(0xFF))
                        }
                    )
                }
            }
        }

        // Should be able to find the group for this test
        val tree = slotTableRecord.store.first().asTree()
        val group = tree.firstOrNull {
            it.location?.sourceFile?.equals("InspectableTests.kt") == true && it.box.right > 0
        } ?: error("Expected a group from this file")
        assertNotNull(group)

        // The group should have a non-empty bounding box
        assertNotEquals(0, group.box.width)
        assertNotEquals(0, group.box.height)
    }

    @Test
    fun parametersTest() {
        val slotTableRecord = SlotTableRecord.create()
        fun unknown(i: Int) = i

        show {
            Inspectable(slotTableRecord) {
                OneParameter(1)
                OneParameter(2)

                OneDefaultParameter()
                OneDefaultParameter(2)

                ThreeParameters(1, 2, 3)

                ThreeDefaultParameters()
                ThreeDefaultParameters(a = 1)
                ThreeDefaultParameters(b = 2)
                ThreeDefaultParameters(a = 1, b = 2)
                ThreeDefaultParameters(c = 3)
                ThreeDefaultParameters(a = 1, c = 3)
                ThreeDefaultParameters(b = 2, c = 3)
                ThreeDefaultParameters(a = 1, b = 2, c = 3)

                val ua = unknown(1)
                val ub = unknown(2)
                val uc = unknown(3)

                ThreeDefaultParameters()
                ThreeDefaultParameters(a = ua)
                ThreeDefaultParameters(b = ub)
                ThreeDefaultParameters(a = ua, b = ub)
                ThreeDefaultParameters(c = uc)
                ThreeDefaultParameters(a = ua, c = uc)
                ThreeDefaultParameters(b = ub, c = uc)
                ThreeDefaultParameters(a = ua, b = ub, c = uc)
            }
        }

        val tree = slotTableRecord.store.first().asTree()
        val list = tree.asList()
        val parameters = list.filter {
            it.parameters.isNotEmpty() && it.location.let {
                it != null && it.sourceFile == "InspectableTests.kt"
            }
        }

        val callCursor = parameters.listIterator()
        class ParameterValidationReceiver(val parameterCursor: Iterator<ParameterInformation>) {
            fun parameter(
                name: String,
                value: Any,
                fromDefault: Boolean,
                static: Boolean,
                compared: Boolean
            ) {
                assertTrue(parameterCursor.hasNext())
                val parameter = parameterCursor.next()
                assertEquals(name, parameter.name)
                assertEquals(value, parameter.value)
                assertEquals(fromDefault, parameter.fromDefault)
                assertEquals(static, parameter.static)
                assertEquals(compared, parameter.compared)
            }
        }

        fun validate(block: ParameterValidationReceiver.() -> Unit) {
            assertTrue(callCursor.hasNext())
            val call = callCursor.next()
            val receiver = ParameterValidationReceiver(call.parameters.listIterator())
            receiver.block()
            assertFalse(receiver.parameterCursor.hasNext())
        }

        // OneParameter(1)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
        }

        // OneParameter(2)
        validate {
            parameter(name = "a", value = 2, fromDefault = false, static = true, compared = false)
        }

        // OneDefaultParameter()
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
        }

        // OneDefaultParameter(2)
        validate {
            parameter(name = "a", value = 2, fromDefault = false, static = true, compared = false)
        }

        // ThreeParameters(1, 2, 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters()
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = 1)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(b = 2)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = 1, b = 2)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters(a = 1, c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters(b = 2, c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters(a = 1, b = 2, c = 3)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = true, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = true, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = true, compared = false)
        }

        // ThreeDefaultParameters()
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = ua)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(b = ub)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(a = ua, b = ub)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = true, static = false, compared = false)
        }

        // ThreeDefaultParameters(c = uc)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        // ThreeDefaultParameters(a = ua, c = uc)
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = true, static = false, compared = false)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        // ThreeDefaultParameters(b = ub, c = uc)
        validate {
            parameter(name = "a", value = 1, fromDefault = true, static = false, compared = false)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        // ThreeDefaultParameters(a = ua, b = ub, c = uc)\
        validate {
            parameter(name = "a", value = 1, fromDefault = false, static = false, compared = true)
            parameter(name = "b", value = 2, fromDefault = false, static = false, compared = true)
            parameter(name = "c", value = 3, fromDefault = false, static = false, compared = true)
        }

        assertFalse(callCursor.hasNext())
    }

    @Test
    fun inInspectionMode() {
        var displayed = false
        show {
            Inspectable(SlotTableRecord.create()) {
                Column {
                    InInspectionModeOnly {
                        Box(Modifier.preferredSize(100.dp).background(color = Color(0xFF)))
                        displayed = true
                    }
                }
            }
        }

        assertTrue(displayed)
    }

    @Test
    fun notInInspectionMode() {
        var displayed = false
        show {
            Column {
                InInspectionModeOnly {
                    Box(Modifier.preferredSize(100.dp).background(color = Color(0xFF)))
                    displayed = true
                }
            }
        }

        assertFalse(displayed)
    }

    @InternalComposeApi
    @Test // regression test for b/161839910
    fun textParametersAreCorrect() {
        val slotTableRecord = SlotTableRecord.create()
        show {
            Inspectable(slotTableRecord) {
                Text("Test")
            }
        }
        val tree = slotTableRecord.store.first().asTree()
        val list = tree.asList()
        val parameters = list.filter {
            it.parameters.isNotEmpty() && it.location.let {
                it != null && it.sourceFile == "InspectableTests.kt"
            }
        }
        val names = parameters.first().parameters.map { it.name }
        assertEquals(
            "text, modifier, color, fontSize, fontStyle, fontWeight, fontFamily, " +
                "letterSpacing, textDecoration, textAlign, lineHeight, overflow, softWrap, " +
                "maxLines, onTextLayout, style",
            names.joinToString()
        )
    }

    @OptIn(InternalComposeApi::class)
    @Test // regression test for b/162092315
    fun inspectingModalDrawerLayout() {
        val positioned = CountDownLatch(1)
        val tables = showAndRecord {
            ModalDrawerLayout(
                drawerContent = { Text("Something") },
                bodyContent = {
                    Column(
                        Modifier.onGloballyPositioned {
                            positioned.countDown()
                        }
                    ) {
                        Text(text = "Hello World", color = Color.Green)
                        Button(onClick = {}) { Text(text = "OK") }
                    }
                }
            )
        }

        assertTrue(positioned.await(2, TimeUnit.SECONDS))

        // Wait for composition to complete
        activity.runOnUiThread { }

        assertFalse(tables.isNullOrEmpty())
        assertTrue(tables!!.size > 1)

        val calls = tables.flatMap { table ->
            if (!table.isEmpty) table.asTree().asList() else emptyList()
        }.filter {
            val location = it.location
            location != null && location.sourceFile == "InspectableTests.kt"
        }.map {
            it.name
        }
        assertTrue(calls.contains("Column"))
        assertTrue(calls.contains("Text"))
        assertTrue(calls.contains("Button"))
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun OneParameter(a: Int) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun OneDefaultParameter(a: Int = 1) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ThreeParameters(a: Int, b: Int, c: Int) {
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ThreeDefaultParameters(a: Int = 1, b: Int = 2, c: Int = 3) {
}

// BFS
internal fun Group.firstOrNull(predicate: (Group) -> Boolean): Group? {
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        val next = stack.removeAt(0)
        if (predicate(next)) return next
        stack.addAll(next.children)
    }
    return null
}

internal fun Group.asList(): List<Group> {
    val result = mutableListOf<Group>()
    val stack = mutableListOf(this)
    while (stack.isNotEmpty()) {
        val next = stack.removeAt(stack.size - 1)
        result.add(next)
        stack.addAll(next.children.reversed())
    }
    return result
}

internal fun SlotTableRecord.findGroupForFile(fileName: String) =
    store.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()

@OptIn(InternalComposeApi::class)
fun SlotTable.findGroupForFile(fileName: String) = asTree().findGroupForFile(fileName)
fun Group.findGroupForFile(fileName: String): Group? {
    val position = position
    if (position != null && position.contains(fileName)) return this
    return children.map { it.findGroupForFile(fileName) }.filterNotNull().firstOrNull()
}
