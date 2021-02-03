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

package androidx.compose.ui.test

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.filters.MediumTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class LayoutCoordinatesHelperTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun positionInParent_noOffset() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setContent {
            Column(
                Modifier.onGloballyPositioned { coordinates: LayoutCoordinates ->
                    parentCoordinates = coordinates
                    latch.countDown()
                }
            ) {
                Box(
                    Modifier.size(10.dp)
                        .align(Alignment.Start)
                        .onGloballyPositioned { coordinates ->
                            childCoordinates = coordinates
                            latch.countDown()
                        }
                )
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            Offset.Zero,
            parentCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero)
        )
    }

    @Test
    fun positionInParent_centered() {
        val latch = CountDownLatch(2)
        var parentCoordinates: LayoutCoordinates? = null
        var childCoordinates: LayoutCoordinates? = null
        rule.setContent {
            with(LocalDensity.current) {
                Box(Modifier.width(40.toDp()), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier.width(20.toDp())
                            .onGloballyPositioned { coordinates: LayoutCoordinates ->
                                parentCoordinates = coordinates
                                latch.countDown()
                            }
                    ) {
                        Box(
                            Modifier.size(10.toDp())
                                .align(Alignment.CenterHorizontally)
                                .onGloballyPositioned { coordinates ->
                                    childCoordinates = coordinates
                                    latch.countDown()
                                }
                        )
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(
            Offset(5f, 0f),
            parentCoordinates!!.localPositionOf(childCoordinates!!, Offset.Zero)
        )
    }
}
