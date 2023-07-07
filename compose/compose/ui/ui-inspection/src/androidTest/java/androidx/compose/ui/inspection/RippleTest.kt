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

import android.view.ViewGroup
import androidx.compose.ui.inspection.framework.getChildren
import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.RippleTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.ThreadUtils
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

@LargeTest
class RippleTest {
    @get:Rule
    val rule = ComposeInspectionRule(RippleTestActivity::class)

    @Test
    fun rippleViewsAreMarked(): Unit = runBlocking {
        val app = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse
        val composeViewChildren = ThreadUtils.runOnMainThread {
            val composeView = rule.rootsForTest.single() as ViewGroup
            composeView.getChildren().map { it.uniqueDrawingId }
        }.get()
        val toSkip = app.rootsList.single().viewsToSkipList
        assertThat(composeViewChildren).containsExactlyElementsIn(toSkip)
        assertThat(toSkip).hasSize(1)
    }
}
