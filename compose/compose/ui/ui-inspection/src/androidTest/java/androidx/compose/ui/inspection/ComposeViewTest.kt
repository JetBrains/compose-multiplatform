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

import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.ComposeViewTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.GetParametersByIdCommand
import androidx.compose.ui.inspection.util.flatten
import androidx.compose.ui.inspection.util.toMap
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import org.junit.Rule
import org.junit.Test

@LargeTest
class ComposeViewTest {
    @get:Rule
    val rule = ComposeInspectionRule(ComposeViewTestActivity::class)

    @Test
    fun composeView(): Unit = runBlocking {
        val response = rule.inspectorTester.sendCommand(
            GetComposablesCommand(rule.rootId, skipSystemComposables = false)
        ).getComposablesResponse
        val strings = response.stringsList.toMap()
        val roots = response.rootsList
        assertThat(roots).hasSize(3)
        val firstText = roots[0].nodesList.findNode("Text", strings)
        val secondText = roots[1].nodesList.findNode("Text", strings)
        val thirdText = roots[2].nodesList.findNode("Text", strings)
        assertThat(firstText?.textParameter).isEqualTo("one")
        assertThat(secondText?.textParameter).isEqualTo("two")
        assertThat(thirdText?.textParameter).isEqualTo("three")
    }

    private fun Iterable<ComposableNode>.findNode(
        name: String,
        strings: Map<Int, String>
    ): ComposableNode? = flatMap { it.flatten() }.singleOrNull { strings[it.name] == name }

    private val ComposableNode.textParameter: String?
        get() = runBlocking {
            val params = rule.inspectorTester.sendCommand(
                GetParametersByIdCommand(
                    rule.rootId,
                    skipSystemComposables = false,
                    composableId = id
                )
            ).getParametersResponse
            val strings = params.stringsList.toMap()
            val param = params.parameterGroup.parameterList.single { strings[it.name] == "text" }
            strings[param.int32Value]
        }
}
