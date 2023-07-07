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

package androidx.compose.ui.unit

import com.google.common.truth.Truth
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTest {

    private val velocity1 = Velocity(3f, -7f)
    private val velocity2 = Velocity(5f, 13f)

    @Test
    fun operatorUnaryMinus() {
        Truth.assertThat(-velocity1)
            .isEqualTo(Velocity(-3f, 7f))
        Truth.assertThat(-velocity2)
            .isEqualTo(Velocity(-5f, -13f))
    }

    @Test
    fun operatorPlus() {
        Truth.assertThat(velocity2 + velocity1)
            .isEqualTo(Velocity(8f, 6f))
        Truth.assertThat(velocity1 + velocity2)
            .isEqualTo(Velocity(8f, 6f))
    }

    @Test
    fun operatorMinus() {
        Truth.assertThat(velocity1 - velocity2)
            .isEqualTo(Velocity(-2f, -20f))
        Truth.assertThat(velocity2 - velocity1)
            .isEqualTo(Velocity(2f, 20f))
    }

    @Test
    fun operatorDivide() {
        Truth.assertThat(velocity1 / 10f)
            .isEqualTo(Velocity(0.3f, -0.7f))
    }

    @Test
    fun operatorTimes() {
        Truth.assertThat(velocity1 * 10f)
            .isEqualTo(Velocity(30f, -70f))
    }

    @Test
    fun operatorRem() {
        Truth.assertThat(velocity1 % 3f)
            .isEqualTo(Velocity(0f, -1f))
    }

    @Test
    fun components() {
        val (x, y) = velocity1
        Truth.assertThat(x).isEqualTo(3f)
        Truth.assertThat(y).isEqualTo(-7f)
    }

    @Test
    fun xy() {
        Truth.assertThat(velocity1.x).isEqualTo(3f)
        Truth.assertThat(velocity1.y).isEqualTo(-7f)
    }

    @Test
    fun testOffsetCopy() {
        val offset = Velocity(100f, 200f)
        Assert.assertEquals(offset, offset.copy())
    }

    @Test
    fun testOffsetCopyOverwriteX() {
        val offset = Velocity(100f, 200f)
        val copy = offset.copy(x = 50f)
        Assert.assertEquals(50f, copy.x)
        Assert.assertEquals(200f, copy.y)
    }

    @Test
    fun testOffsetCopyOverwriteY() {
        val offset = Velocity(100f, 200f)
        val copy = offset.copy(y = 300f)
        Assert.assertEquals(100f, copy.x)
        Assert.assertEquals(300f, copy.y)
    }
}