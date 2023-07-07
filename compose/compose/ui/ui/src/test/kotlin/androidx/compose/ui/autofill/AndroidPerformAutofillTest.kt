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

import android.app.Activity
import android.util.SparseArray
import android.view.View
import android.view.autofill.AutofillValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(minSdk = 26)
class AndroidPerformAutofillTest {
    private val autofillTree = AutofillTree()
    private lateinit var androidAutofill: AndroidAutofill

    @Before
    fun setup() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val view = View(activity)
        activity.setContentView(view)

        androidAutofill = AndroidAutofill(view, autofillTree)
    }

    @Test
    fun performAutofill_name() {
        // Arrange.
        val expectedValue = "Name"
        var autofilledValue = ""
        val autofillNode = AutofillNode(
            onFill = { autofilledValue = it },
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        autofillTree += autofillNode

        val autofillValues = SparseArray<AutofillValue>()
            .apply { append(autofillNode.id, AutofillValue.forText(expectedValue)) }

        // Act.
        androidAutofill.performAutofill(autofillValues)

        // Assert.
        Truth.assertThat(autofilledValue).isEqualTo(expectedValue)
    }

    @Test
    fun performAutofill_email() {
        // Arrange.
        val expectedValue = "email@google.com"
        var autofilledValue = ""
        val autofillNode = AutofillNode(
            onFill = { autofilledValue = it },
            autofillTypes = listOf(AutofillType.EmailAddress),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        autofillTree += autofillNode

        val autofillValues = SparseArray<AutofillValue>()
            .apply { append(autofillNode.id, AutofillValue.forText(expectedValue)) }

        // Act.
        androidAutofill.performAutofill(autofillValues)

        // Assert.
        Truth.assertThat(autofilledValue).isEqualTo(expectedValue)
    }
}
