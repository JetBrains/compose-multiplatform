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

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTest {

    private val velocity1 = Velocity(pixelsPerSecond = Offset(3f, -7f))
    private val velocity2 = Velocity(pixelsPerSecond = Offset(5f, 13f))

    @Test
    fun operatorUnaryMinus() {
        Truth.assertThat(-velocity1)
            .isEqualTo(Velocity(pixelsPerSecond = Offset(-3f, 7f)))
        Truth.assertThat(-velocity2)
            .isEqualTo(Velocity(pixelsPerSecond = Offset(-5f, -13f)))
    }

    @Test
    fun operatorPlus() {
        Truth.assertThat(velocity2 + velocity1)
            .isEqualTo(Velocity(pixelsPerSecond = Offset(8f, 6f)))
        Truth.assertThat(velocity1 + velocity2)
            .isEqualTo(Velocity(pixelsPerSecond = Offset(8f, 6f)))
    }

    @Test
    fun operatorMinus() {
        Truth.assertThat(velocity1 - velocity2)
            .isEqualTo(Velocity(pixelsPerSecond = Offset(-2f, -20f)))
        Truth.assertThat(velocity2 - velocity1)
            .isEqualTo(Velocity(pixelsPerSecond = Offset(2f, 20f)))
    }
}