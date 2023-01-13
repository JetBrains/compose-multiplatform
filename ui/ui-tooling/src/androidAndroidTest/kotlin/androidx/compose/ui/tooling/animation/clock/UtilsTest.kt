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

package androidx.compose.ui.tooling.animation.clock

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.animation.states.TargetState
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UtilsTest {

    @Test
    fun currentValueIsNull() {
        val value = parseParametersToValue<Int?>(null, 20, 30)
        assertNull(value)
    }

    @Test
    fun par2IsNull() {
        val value = parseParametersToValue(10, 20, null)
        assertNull(value)
    }

    @Test
    fun currentValueHasDifferentType() {
        val value = parseParametersToValue(10f, 20, 30)
        assertNull(value)
    }

    @Test
    fun par1HasDifferentType() {
        val value = parseParametersToValue(10, 20f, 30)
        assertNull(value)
    }

    @Test
    fun par2HasDifferentType() {
        val value = parseParametersToValue(10, 20, 30f)
        assertNull(value)
    }

    @Test
    fun listsHasNull() {
        val value = parseParametersToValue(IntSize(10, 20), listOf(10, null), listOf(20, 30))
        assertNull(value)
    }

    @Test
    fun listsAreEmpty() {
        val value = parseParametersToValue(IntSize(10, 20), emptyList<Int>(), emptyList<Int>())
        assertNull(value)
    }

    @Test
    fun offsetOutOfBounds() {
        val value = parseParametersToValue(
            Offset(10f, 20f),
            listOf(30f),
            listOf(50f, 60f)
        )
        assertNull(value)
    }

    @Test
    fun offsetIncorrectType() {
        val value = parseParametersToValue(
            Offset(10f, 20f),
            listOf("a", "b"),
            listOf(50f, 60f)
        )
        assertNull(value)
    }

    @Test
    fun intIsParsed() {
        val value = parseParametersToValue(10, 20, 30)
        assertEquals(TargetState(20, 30), value)
    }

    @Test
    fun intIsParsedAsList() {
        val value = parseParametersToValue(10, listOf(20), listOf(30))
        assertEquals(TargetState(20, 30), value)
    }

    @Test
    fun stringIsParsed() {
        val value = parseParametersToValue("a", "b", "c")
        assertEquals(TargetState("b", "c"), value)
    }

    @Test
    fun stringIsParsedAsList() {
        val value = parseParametersToValue("a", listOf("b"), listOf("c"))
        assertEquals(TargetState("b", "c"), value)
    }

    @Test
    fun booleanIsParsed() {
        val value = parseParametersToValue(currentValue = false, par1 = true, par2 = false)
        assertEquals(TargetState(initial = true, target = false), value)
    }

    @Test
    fun booleanIsParsedAsList() {
        val value = parseParametersToValue(false, listOf(true), listOf(false))
        assertEquals(TargetState(initial = true, target = false), value)
    }

    @Test
    fun dpIsParsed() {
        val value = parseParametersToValue(10.dp, 20.dp, 30.dp)
        assertEquals(TargetState(20.dp, 30.dp), value)
    }

    @Test
    fun dpIsParsedAsDoubleAndFloat() {
        val value = parseParametersToValue(10.dp, 20.0, 30f)
        assertEquals(TargetState(20.dp, 30.dp), value)
    }

    @Test
    fun dpIsParsedAsList() {
        val value = parseParametersToValue(10.dp, listOf(20f), listOf(30f))
        assertEquals(TargetState(20.dp, 30.dp), value)
    }

    @Test
    fun dpIsParsedAsDoubleAndFloatList() {
        val value = parseParametersToValue(10.dp, listOf(20.0), listOf(30f))
        assertEquals(TargetState(20.dp, 30.dp), value)
    }

    @Test
    fun intSizeIsParsed() {
        val value = parseParametersToValue(
            IntSize(10, 20),
            IntSize(30, 40),
            IntSize(50, 60)
        )
        assertEquals(TargetState(IntSize(30, 40), IntSize(50, 60)), value)
    }

    @Test
    fun intSizeIsParsedAsList() {
        val value = parseParametersToValue(
            IntSize(10, 20),
            listOf(30, 40),
            listOf(50, 60)
        )
        assertEquals(TargetState(IntSize(30, 40), IntSize(50, 60)), value)
    }

    @Test
    fun intOffsetIsParsed() {
        val value = parseParametersToValue(
            IntOffset(10, 20),
            IntOffset(30, 40),
            IntOffset(50, 60)
        )
        assertEquals(TargetState(IntOffset(30, 40), IntOffset(50, 60)), value)
    }

    @Test
    fun intOffsetIsParsedAsList() {
        val value = parseParametersToValue(
            IntOffset(10, 20),
            listOf(30, 40),
            listOf(50, 60)
        )
        assertEquals(TargetState(IntOffset(30, 40), IntOffset(50, 60)), value)
    }

    @Test
    fun sizeIsParsed() {
        val value = parseParametersToValue(
            Size(10f, 20f),
            Size(30f, 40f),
            Size(50f, 60f)
        )
        assertEquals(TargetState(Size(30f, 40f), Size(50f, 60f)), value)
    }

    @Test
    fun sizeIsParsedAsList() {
        val value = parseParametersToValue(
            Size(10f, 20f),
            listOf(30f, 40f),
            listOf(50f, 60f)
        )
        assertEquals(TargetState(Size(30f, 40f), Size(50f, 60f)), value)
    }

    @Test
    fun offsetIsParsed() {
        val value = parseParametersToValue(
            Offset(10f, 20f),
            Offset(30f, 40f),
            Offset(50f, 60f)
        )
        assertEquals(TargetState(Offset(30f, 40f), Offset(50f, 60f)), value)
    }

    @Test
    fun offsetIsParsedAsList() {
        val value = parseParametersToValue(
            Offset(10f, 20f),
            listOf(30f, 40f),
            listOf(50f, 60f)
        )
        assertEquals(TargetState(Offset(30f, 40f), Offset(50f, 60f)), value)
    }

    @Test
    fun rectIsParsed() {
        val value = parseParametersToValue(
            Rect(10f, 20f, 30f, 40f),
            Rect(50f, 60f, 70f, 80f),
            Rect(90f, 100f, 110f, 120f)
        )
        assertEquals(
            TargetState(
                Rect(50f, 60f, 70f, 80f),
                Rect(90f, 100f, 110f, 120f)
            ), value
        )
    }

    @Test
    fun rectIsParsedAsList() {
        val value = parseParametersToValue(
            Rect(10f, 20f, 30f, 40f),
            listOf(50f, 60f, 70f, 80f),
            listOf(90f, 100f, 110f, 120f)
        )
        assertEquals(
            TargetState(
                Rect(50f, 60f, 70f, 80f),
                Rect(90f, 100f, 110f, 120f)
            ), value
        )
    }

    @Test
    fun colorIsParsed() {
        val value = parseParametersToValue(
            Color(0.1f, 0.2f, 0.3f, 0.4f),
            Color(0.5f, 0.6f, 0.7f, 0.8f),
            Color(0.55f, 0.65f, 0.75f, 0.85f)
        )
        assertEquals(
            TargetState(
                Color(0.5f, 0.6f, 0.7f, 0.8f),
                Color(0.55f, 0.65f, 0.75f, 0.85f)
            ), value
        )
    }

    @Test
    fun colorIsParsedAsList() {
        val value = parseParametersToValue(
            Color(0.1f, 0.2f, 0.3f, 0.4f),
            listOf(0.5f, 0.6f, 0.7f, 0.8f),
            listOf(0.55f, 0.65f, 0.75f, 0.85f)
        )
        assertEquals(
            TargetState(
                Color(0.5f, 0.6f, 0.7f, 0.8f),
                Color(0.55f, 0.65f, 0.75f, 0.85f)
            ), value
        )
    }
}