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

package androidx.compose.animation.core

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MotionTest {
    @Test
    fun testMotionCopy() {
        val motion = Motion(100f, 200f)
        Assert.assertEquals(motion, motion.copy())
    }

    @Test
    fun testMotionCopyOverwriteValue() {
        val motion = Motion(100f, 200f)
        val copy = motion.copy(value = 50f)
        Assert.assertEquals(50f, copy.value)
        Assert.assertEquals(200f, copy.velocity)
    }

    @Test
    fun testMotionCopyOverwriteY() {
        val radius = Motion(100f, 200f)
        val copy = radius.copy(velocity = 300f)
        Assert.assertEquals(100f, copy.value)
        Assert.assertEquals(300f, copy.velocity)
    }
}