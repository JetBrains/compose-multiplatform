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

package androidx.compose.ui.benchmark.autofill

import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillTree
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@OptIn(ExperimentalComposeUiApi::class)
@RunWith(AndroidJUnit4::class)
class AndroidAutofillBenchmark {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var autofillTree: AutofillTree
    private lateinit var composeView: View

    @Before
    fun setup() {
        composeTestRule.setContent {
            autofillTree = LocalAutofillTree.current
            composeView = LocalView.current
        }
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Test
    @UiThreadTest
    @SdkSuppress(minSdkVersion = 26)
    fun provideAutofillVirtualStructure_performAutofill() {

        // Arrange.
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        val autofillValues = SparseArray<AutofillValue>().apply {
            append(autofillNode.id, AutofillValue.forText("Name"))
        }
        autofillTree += autofillNode

        // Assess.
        benchmarkRule.measureRepeated {
            composeView.autofill(autofillValues)
        }
    }
}