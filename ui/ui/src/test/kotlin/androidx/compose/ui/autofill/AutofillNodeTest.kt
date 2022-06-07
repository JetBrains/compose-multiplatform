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
import android.view.autofill.AutofillManager
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow

@OptIn(ExperimentalComposeUiApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(
    shadows = [ShadowAutofillManager::class],
    minSdk = 26
)
class AutofillNodeTest {
    private lateinit var androidAutofill: AndroidAutofill
    private lateinit var autofillManager: ShadowAutofillManager
    private lateinit var view: View
    private val autofillTree = AutofillTree()

    @Before
    fun setup() {
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        view = View(activity)
        activity.setContentView(view)

        autofillManager = Shadow.extract<ShadowAutofillManager>(
            activity.getSystemService(AutofillManager::class.java)
        )

        androidAutofill = AndroidAutofill(view, autofillTree)
    }

    @Test
    fun eachInstanceHasUniqueId() {
        assertThat(listOf(AutofillNode {}.id, AutofillNode {}.id, AutofillNode {}.id))
            .containsNoDuplicates()
    }

    @Test
    fun importantForAutofill_is_yes() {
        assertThat(view.importantForAutofill).isEqualTo(View.IMPORTANT_FOR_AUTOFILL_YES)
    }

    @Test
    fun requestAutofillForNode_calls_notifyViewEntered() {
        // Arrange.
        val bounds = Rect(0f, 0f, 0f, 0f)
        val autofillNode = AutofillNode(onFill = {}, boundingBox = bounds)

        // Act.
        androidAutofill.requestAutofillForNode(autofillNode)

        // Assert.
        assertThat(autofillManager.viewEnteredStats).containsExactly(
            ShadowAutofillManager.NotifyViewEntered(view, autofillNode.id, bounds)
        )
    }

    @Test
    fun requestAutofillForNode_beforeComposableIsPositioned_throwsError() {
        // Arrange - Before the composable is positioned, the boundingBox is null.
        val autofillNode = AutofillNode(onFill = {})

        // Act and assert.
        val exception = Assert.assertThrows(IllegalStateException::class.java) {
            androidAutofill.requestAutofillForNode(autofillNode)
        }

        // Assert some more.
        assertThat(exception).hasMessageThat().isEqualTo(
            "requestAutofill called before onChildPositioned()"
        )
    }

    @Test
    fun cancelAutofillForNode_calls_notifyViewExited() {
        // Arrange.
        val autofillNode = AutofillNode(onFill = {})

        // Act.
        androidAutofill.cancelAutofillForNode(autofillNode)

        // Assert.
        assertThat(autofillManager.viewExitedStats).containsExactly(
            ShadowAutofillManager.NotifyViewExited(view, autofillNode.id)
        )
    }
}

@Implements(value = AutofillManager::class, minSdk = 26)
internal class ShadowAutofillManager {
    data class NotifyViewEntered(val view: View, val virtualId: Int, val rect: Rect)
    data class NotifyViewExited(val view: View, val virtualId: Int)

    val viewEnteredStats = mutableListOf<NotifyViewEntered>()
    val viewExitedStats = mutableListOf<NotifyViewExited>()

    @Implementation
    fun notifyViewEntered(view: View, virtualId: Int, rect: android.graphics.Rect) {
        viewEnteredStats += NotifyViewEntered(view, virtualId, rect.toComposeRect())
    }

    @Implementation
    fun notifyViewExited(view: View, virtualId: Int) {
        viewExitedStats += NotifyViewExited(view, virtualId)
    }
}
