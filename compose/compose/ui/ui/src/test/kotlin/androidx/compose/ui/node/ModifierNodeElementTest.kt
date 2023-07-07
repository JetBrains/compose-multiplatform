/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.node

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.ValueElement
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(JUnit4::class)
class ModifierNodeElementTest {

    @Test
    fun testDefaultInspectableProperties() {
        @Suppress("unused", "UNUSED_PARAMETER")
        class AModifierElement(
            val string: String,
            val int: Int,
            val map: Map<String, Any>,
            inputParameter: Boolean
        ) : ModifierNodeElement<Modifier.Node>() {
            var classProperty = 0
            override fun create() = object : Modifier.Node() {}
            override fun update(node: Modifier.Node) = node
            // We don't use equals or hashCode in this test, so bad implementations are okay.
            override fun hashCode() = 0
            override fun equals(other: Any?) = (this === other)
        }

        val modifier = AModifierElement(
            string = "parameter 1",
            int = 12345,
            map = mapOf("key" to "value"),
            inputParameter = true
        ).apply {
            classProperty = 42
        }

        assertEquals(
            "The modifier's inspectable value was not automatically populated as expected",
            expectedName = "AModifierElement",
            expectedValue = null,
            expectedProperties = listOf(
                ValueElement("classProperty", 42),
                ValueElement("int", 12345),
                ValueElement("map", mapOf("key" to "value")),
                ValueElement("string", "parameter 1"),
            ),
            actual = modifier
        )
    }

    @Suppress("SameParameterValue")
    private fun assertEquals(
        message: String,
        expectedName: String? = null,
        expectedValue: Any? = null,
        expectedProperties: List<ValueElement> = emptyList(),
        actual: InspectableValue
    ) {
        assertEquals(
            "$message (unexpected name)",
            expectedName,
            actual.nameFallback
        )

        assertEquals(
            "$message (unexpected value)",
            expectedValue,
            actual.valueOverride
        )

        assertEquals(
            "$message (unexpected properties)",
            expectedProperties,
            actual.inspectableElements.toList()
        )
    }
}