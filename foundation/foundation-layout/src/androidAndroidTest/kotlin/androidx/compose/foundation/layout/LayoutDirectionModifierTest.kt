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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.emptyContent
import androidx.compose.ui.Layout
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.Ref
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@SmallTest
@RunWith(AndroidJUnit4::class)
class LayoutDirectionModifierTest : LayoutTest() {

    @Test
    fun testModifiedLayoutDirection_inMeasureScope() {
        val latch = CountDownLatch(1)
        val resultLayoutDirection = Ref<LayoutDirection>()

        show {
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Layout(children = @Composable {}) { _, _ ->
                    resultLayoutDirection.value = layoutDirection
                    latch.countDown()
                    layout(0, 0) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(LayoutDirection.Rtl == resultLayoutDirection.value)
    }

    @Test
    fun testModifiedLayoutDirection_inIntrinsicsMeasure() {
        val latch = CountDownLatch(1)
        var resultLayoutDirection: LayoutDirection? = null

        show {
            @OptIn(ExperimentalLayout::class)
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Layout(
                    children = @Composable {},
                    modifier = Modifier.preferredWidth(IntrinsicSize.Max),
                    minIntrinsicWidthMeasureBlock = { _, _ -> 0 },
                    minIntrinsicHeightMeasureBlock = { _, _ -> 0 },
                    maxIntrinsicWidthMeasureBlock = { _, _ ->
                        resultLayoutDirection = this.layoutDirection
                        latch.countDown()
                        0
                    },
                    maxIntrinsicHeightMeasureBlock = { _, _ -> 0 }
                ) { _, _ ->
                    layout(0, 0) {}
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertNotNull(resultLayoutDirection)
        assertTrue(LayoutDirection.Rtl == resultLayoutDirection)
    }

    @Test
    fun testRestoreLocaleLayoutDirection() {
        val latch = CountDownLatch(1)
        val resultLayoutDirection = Ref<LayoutDirection>()

        show {
            val initialLayoutDirection = LayoutDirectionAmbient.current
            Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                Box {
                    Providers(LayoutDirectionAmbient provides initialLayoutDirection) {
                        Layout(emptyContent()) { _, _ ->
                            resultLayoutDirection.value = layoutDirection
                            latch.countDown()
                            layout(0, 0) {}
                        }
                    }
                }
            }
        }

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(LayoutDirection.Ltr, resultLayoutDirection.value)
    }
}
