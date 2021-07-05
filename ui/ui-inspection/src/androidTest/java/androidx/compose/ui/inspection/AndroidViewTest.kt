/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.AndroidViewTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.flatten
import androidx.compose.ui.inspection.util.toMap
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@LargeTest
class AndroidViewTest {
    @get:Rule
    val rule = ComposeInspectionRule(AndroidViewTestActivity::class)

    @Test
    fun androidView(): Unit = runBlocking {
        val app = rule.inspectorTester.sendCommand(
            GetComposablesCommand(rule.rootId, skipSystemComposables = false)
        ).getComposablesResponse
        val strings = app.stringsList.toMap()
        val composeNode = app.rootsList.flatMap { it.nodesList }.flatMap { it.flatten() }.filter {
            it.viewId != 0L
        }.single()
        assertThat(strings[composeNode.name]).isEqualTo("ComposeNode")
        val androidViewsHandler = rule.rootsForTest.single().view.childAt(0)
        val viewFactoryHolder = androidViewsHandler.childAt(0)
        assertThat(composeNode.viewId).isEqualTo(viewFactoryHolder.uniqueDrawingId)
    }

    private fun View.childAt(index: Int): View =
        (this as ViewGroup).getChildAt(index)
}
