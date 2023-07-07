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
import android.view.View
import android.view.ViewStructure
import androidx.autofill.HintConstants.AUTOFILL_HINT_PERSON_NAME
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 26
)
class AndroidPopulateViewStructureTest {
    private val autofillTree = AutofillTree()
    private lateinit var androidAutofill: AndroidAutofill
    private lateinit var currentPackage: String

    @Before
    fun setup() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        val view = View(activity)
        activity.setContentView(view)

        androidAutofill = AndroidAutofill(view, autofillTree)
        currentPackage = activity.packageName
    }

    @Test
    fun populateViewStructure_emptyAutofillTree() {
        // Arrange.
        val viewStructure: ViewStructure = FakeAndroidViewStructure()

        // Act.
        androidAutofill.populateViewStructure(viewStructure)

        // Assert.
        assertThat(viewStructure.childCount).isEqualTo(0)
    }

    @Test
    fun populateViewStructure_oneChild() {
        // Arrange.
        val autofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        autofillTree += autofillNode

        // Act.
        val viewStructure = FakeAndroidViewStructure()
        androidAutofill.populateViewStructure(viewStructure)

        // Assert.
        assertThat(viewStructure).isEqualTo(
            FakeAndroidViewStructure().apply {
                children.add(
                    FakeAndroidViewStructure().apply {
                        virtualId = autofillNode.id
                        packageName = currentPackage
                        setAutofillType(View.AUTOFILL_TYPE_TEXT)
                        setAutofillHints(arrayOf(AUTOFILL_HINT_PERSON_NAME))
                        setDimens(0, 0, 0, 0, 0, 0)
                    }
                )
            }
        )
    }

    @Test
    fun populateViewStructure_twoChildren() {
        // Arrange.
        val nameAutofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.PersonFullName),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        autofillTree += nameAutofillNode

        val emailAutofillNode = AutofillNode(
            onFill = {},
            autofillTypes = listOf(AutofillType.EmailAddress),
            boundingBox = Rect(0f, 0f, 0f, 0f)
        )
        autofillTree += emailAutofillNode

        // Act.
        val viewStructure: ViewStructure = FakeAndroidViewStructure()
        androidAutofill.populateViewStructure(viewStructure)

        // Assert.
        assertThat(viewStructure).isEqualTo(
            FakeAndroidViewStructure().apply {
                children.add(
                    FakeAndroidViewStructure().apply {
                        virtualId = nameAutofillNode.id
                        packageName = currentPackage
                        setAutofillType(View.AUTOFILL_TYPE_TEXT)
                        setAutofillHints(arrayOf(AUTOFILL_HINT_PERSON_NAME))
                        setDimens(0, 0, 0, 0, 0, 0)
                    }
                )
                children.add(
                    FakeAndroidViewStructure().apply {
                        virtualId = emailAutofillNode.id
                        packageName = currentPackage
                        setAutofillType(View.AUTOFILL_TYPE_TEXT)
                        setAutofillHints(arrayOf(View.AUTOFILL_HINT_EMAIL_ADDRESS))
                        setDimens(0, 0, 0, 0, 0, 0)
                    }
                )
            }
        )
    }
}
