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

package androidx.compose.ui.layout

import androidx.compose.ui.Remeasurement
import androidx.compose.ui.RemeasurementModifier
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@SmallTest
@RunWith(AndroidJUnit4::class)
class RemeasurementModifierTest {
    @Test
    @OptIn(ExperimentalLayoutNodeApi::class)
    fun nodeIsRemeasuredAfterForceRemeasureBlocking() {
        var remeasurementObj: Remeasurement? = null
        val root = root {
            add(
                node {
                    modifier = object : RemeasurementModifier {
                        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
                            remeasurementObj = remeasurement
                        }
                    }
                }
            )
        }

        createDelegate(root)

        assertThat(remeasurementObj).isNotNull()
        // node is not dirty
        assertMeasuredAndLaidOut(root.first)
        // but still remeasured
        assertRemeasured(root.first) {
            remeasurementObj!!.forceRemeasure()
        }
    }
}