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

package androidx.compose.ui.platform

import android.os.Parcelable
import android.util.SparseArray
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.util.isEmpty
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test

class ComposeViewSavedStateSizeTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun composeViewIsProducingEmptySavedState() {
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box {
                BasicText("hello world")
            }
        }

        rule.runOnIdle {
            val array = SparseArray<Parcelable>()
            view.saveHierarchyState(array)
            assertThat(array.isEmpty()).isTrue()
        }
    }

    @Test
    fun childrenWithGraphicsLayerAreNotIncreasingTheSavedStateSize() {
        var childCount by mutableStateOf(0)
        lateinit var view: View
        rule.setContent {
            view = LocalView.current
            Box {
                repeat(childCount) {
                    Box(Modifier.graphicsLayer())
                }
            }
        }

        val initialArray = rule.runOnIdle {
            val array = SparseArray<Parcelable>()
            view.saveHierarchyState(array)
            array
        }

        childCount = 10

        rule.runOnIdle {
            val array = SparseArray<Parcelable>()
            view.saveHierarchyState(array)
            // we are comparing the sizes here just to not make assumptions about the
            // initial array size in this particular test.
            assertWithMessage("New array $array. Initial array $initialArray")
                .that(array.size())
                .isEqualTo(initialArray.size())
        }
    }
}
