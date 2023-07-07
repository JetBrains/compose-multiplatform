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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset
import kotlin.test.Test
import kotlin.test.assertEquals

class RenderEffectTest {

    @Test
    fun testBlurEffectEquality() {
        val blur1 = BlurEffect(5f, 10f, TileMode.Clamp)
        val blur2 = BlurEffect(5f, 10f, TileMode.Clamp)
        assertEquals(blur1, blur2)
    }

    @Test
    fun testBlurEffectHashcode() {
        val blur1 = BlurEffect(5f, 10f, TileMode.Clamp)
        val blur2 = BlurEffect(5f, 10f, TileMode.Clamp)
        assertEquals(blur1.hashCode(), blur2.hashCode())
    }

    @Test
    fun testBlurEffectToString() {
        assertEquals(
            "BlurEffect(" +
                "renderEffect=null, " +
                "radiusX=5.0, " +
                "radiusY=10.0, " +
                "edgeTreatment=Clamp" +
                ")",
            BlurEffect(5f, 10.0f, TileMode.Clamp).toString()
        )
    }

    @Test
    fun testNestedRenderEffectEquality() {
        val innerBlur = BlurEffect(5.0f, 10.0f, TileMode.Mirror)
        val wrappedBlur = BlurEffect(innerBlur, 20.0f, 50.0f, TileMode.Clamp)
        val wrappedBlur2 = BlurEffect(
            BlurEffect(5.0f, 10.0f, TileMode.Mirror),
            20.0f,
            50.0f,
            TileMode.Clamp
        )
        assertEquals(wrappedBlur, wrappedBlur2)
    }

    @Test
    fun testNestedRenderEffectHashcode() {
        val innerBlur = BlurEffect(5.0f, 10.0f, TileMode.Mirror)
        val wrappedBlur = BlurEffect(innerBlur, 20.0f, 50.0f, TileMode.Clamp)
        val wrappedBlur2 = BlurEffect(
            BlurEffect(5.0f, 10.0f, TileMode.Mirror),
            20.0f,
            50.0f,
            TileMode.Clamp
        )
        assertEquals(wrappedBlur.hashCode(), wrappedBlur2.hashCode())
    }

    @Test
    fun testNestedRenderEffectToString() {
        val blur = BlurEffect(
            BlurEffect(5.0f, 10.0f, TileMode.Clamp),
            radiusX = 15.0f,
            radiusY = 20.0f,
            TileMode.Decal
        )
        assertEquals(
            "BlurEffect(" +
                "renderEffect=" +
                "BlurEffect(" +
                "renderEffect=null, " +
                "radiusX=5.0, " +
                "radiusY=10.0, " +
                "edgeTreatment=Clamp" +
                "), " +
                "radiusX=15.0, " +
                "radiusY=20.0, " +
                "edgeTreatment=Decal" +
                ")",
            blur.toString()
        )
    }

    @Test
    fun testOffsetEffectEquality() {
        val offsetEffect1 = OffsetEffect(10f, 15f)
        val offsetEffect2 = OffsetEffect(10f, 15f)
        assertEquals(offsetEffect1, offsetEffect2)
    }

    @Test
    fun testOffsetEffectHashcode() {
        val offsetEffect1 = OffsetEffect(10f, 15f)
        val offsetEffect2 = OffsetEffect(10f, 15f)
        assertEquals(offsetEffect1.hashCode(), offsetEffect2.hashCode())
    }

    @Test
    fun testOffsetEffectToString() {
        assertEquals(
            "OffsetEffect(" +
                "renderEffect=null, " +
                "offset=Offset(5.0, 10.0)" +
                ")",
            OffsetEffect(5f, 10.0f).toString()
        )
    }

    @Test
    fun testNestedOffsetEffectEquality() {
        val innerOffset = OffsetEffect(5.0f, 10.0f)
        val wrappedOffset = OffsetEffect(innerOffset, Offset(20.0f, 50.0f))
        val wrappedOffset2 = OffsetEffect(
            OffsetEffect(5.0f, 10.0f),
            Offset(20.0f, 50.0f)
        )
        assertEquals(wrappedOffset, wrappedOffset2)
    }

    @Test
    fun testNestedOffsetEffectHashcode() {
        val innerOffset = OffsetEffect(5.0f, 10.0f)
        val wrappedOffset = OffsetEffect(innerOffset, Offset(20.0f, 50.0f))
        val wrappedOffset2 = OffsetEffect(
            OffsetEffect(5.0f, 10.0f),
            Offset(20.0f, 50.0f)
        )
        assertEquals(wrappedOffset.hashCode(), wrappedOffset2.hashCode())
    }

    @Test
    fun testNestedOffsetEffectToString() {
        val renderEffect = OffsetEffect(
            BlurEffect(5.0f, 10.0f, TileMode.Clamp),
            Offset(15.0f, 20.0f)
        )
        assertEquals(
            "OffsetEffect(" +
                "renderEffect=" +
                "BlurEffect(" +
                "renderEffect=null, " +
                "radiusX=5.0, " +
                "radiusY=10.0, " +
                "edgeTreatment=Clamp" +
                "), " +
                "offset=Offset(15.0, 20.0))",
            renderEffect.toString()
        )
    }
}