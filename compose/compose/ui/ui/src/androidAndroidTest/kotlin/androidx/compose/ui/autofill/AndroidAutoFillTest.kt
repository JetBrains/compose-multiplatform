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

package androidx.compose.ui.autofill

import android.util.SparseArray
import android.view.View
import android.view.ViewStructure
import android.view.autofill.AutofillValue
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalComposeUiApi::class)
@SmallTest
@RunWith(AndroidJUnit4::class)
class AndroidAutoFillTest {
    @get:Rule
    val rule = createComposeRule()

    private var autofill: Autofill? = null
    private lateinit var autofillTree: AutofillTree
    private lateinit var ownerView: View

    @Before
    fun setup() {
        rule.setContent {
            ownerView = LocalView.current
            autofill = LocalAutofill.current
            autofillTree = LocalAutofillTree.current
        }
    }

    @SdkSuppress(maxSdkVersion = 25)
    @Test
    fun autofillCompositionLocal_belowApi26_isNull() {
        assertThat(autofill).isNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofillCompositionLocal_isNotNull() {
        assertThat(autofill).isNotNull()
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofillCompositionLocal_returnsAnInstanceOfAndroidAutofill() {
        assertThat(autofill).isInstanceOf(AndroidAutofill::class.java)
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun onProvideAutofillVirtualStructure_populatesViewStructure() {
        // Arrange.
        val viewStructure: ViewStructure = FakeViewStructure()
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        autofillTree += autofillNode

        // Act.
        ownerView.onProvideAutofillVirtualStructure(viewStructure, 0)
        val currentPackageName = ownerView.context.applicationInfo.packageName

        // Assert.
        assertThat(viewStructure).isEqualTo(
            FakeViewStructure().apply {
                children.add(
                    FakeViewStructure().apply {
                        virtualId = autofillNode.id
                        packageName = currentPackageName
                        setAutofillType(View.AUTOFILL_TYPE_TEXT)
                        setAutofillHints(arrayOf(AUTOFILL_HINT_PERSON_NAME))
                        setDimens(0, 0, 0, 0, 0, 0)
                    }
                )
            }
        )
    }

    @SdkSuppress(minSdkVersion = 26)
    @Test
    fun autofill_triggersOnFill() {
        // Arrange.
        val expectedValue = "PersonName"
        var autofilledValue = ""
        val autofillNode = AutofillNode(
            onFill = { autofilledValue = it },
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        val autofillValues = SparseArray<AutofillValue>().apply {
            append(autofillNode.id, AutofillValue.forText(expectedValue))
        }
        autofillTree += autofillNode

        // Act.
        ownerView.autofill(autofillValues)

        // Assert.
        assertThat(autofilledValue).isEqualTo(expectedValue)
    }
}