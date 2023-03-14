/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class PathMeasureTest {

    @Test
    fun testGetPositionAndTangent() {
        val width = 100f
        val height = 100f
        val path = Path().apply {
            lineTo(width, height)
        }
        val pathMeasure = PathMeasure()

        pathMeasure.setPath(path, false)
        val distance = pathMeasure.length
        val position = pathMeasure.getPosition(distance * 0.5f)

        val tangent = pathMeasure.getTangent(distance * 0.5f)

        Assert.assertEquals(50f, position.x, 0.0001f)
        Assert.assertEquals(50f, position.y, 0.0001f)
        Assert.assertEquals(0.707106f, tangent.x, 0.0001f)
        Assert.assertEquals(0.707106f, tangent.y, 0.0001f)
    }
}