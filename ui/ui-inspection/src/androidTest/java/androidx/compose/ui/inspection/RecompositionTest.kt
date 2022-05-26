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

package androidx.compose.ui.inspection

import android.view.inspector.WindowInspector
import androidx.compose.ui.inspection.rules.DebugViewAttributeRule
import androidx.compose.ui.inspection.rules.JvmtiRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.RecompositionTestActivity
import androidx.compose.ui.inspection.util.GetAllParametersCommand
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.GetUpdateSettingsCommand
import androidx.compose.ui.inspection.util.flatten
import androidx.compose.ui.inspection.util.toMap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.inspection.testing.InspectorTester
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetAllParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

@LargeTest
class RecompositionTest {
    private val rule = createAndroidComposeRule<RecompositionTestActivity>()

    @get:Rule
    val chain = RuleChain.outerRule(JvmtiRule()).around(DebugViewAttributeRule()).around(rule)!!

    private lateinit var inspectorTester: InspectorTester

    @Before
    fun before() {
        JvmtiRule.ensureInitialised()
        runBlocking {
            inspectorTester = InspectorTester(inspectorId = "layoutinspector.compose.inspection")
        }
    }

    @After
    fun after() {
        inspectorTester.dispose()
    }

    @Test
    fun recomposing(): Unit = runBlocking {
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true)
        ).updateSettingsResponse

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        val rootId =
            WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }.single().toLong()
        var composables = inspectorTester.sendCommand(
            GetComposablesCommand(rootId, skipSystemComposables = false)
        ).getComposablesResponse

        val parameters = inspectorTester.sendCommand(
            GetAllParametersCommand(rootId, skipSystemComposables = false)
        ).getAllParametersResponse

        // Buttons have double recompose counts, as they are
        // recomposed on the down event for the press indication
        var nodes = Nodes(composables, parameters)
        assertThat(nodes.button1.recomposeCount).isEqualTo(6)
        assertThat(nodes.text1.recomposeCount).isEqualTo(3)
        assertThat(nodes.button2.recomposeCount).isEqualTo(2)
        assertThat(nodes.text2.recomposeCount).isEqualTo(1)

        // Stop counting but keep the current counts:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = false, keepRecomposeCounts = true)
        ).updateSettingsResponse

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        composables = inspectorTester.sendCommand(
            GetComposablesCommand(rootId, skipSystemComposables = false)
        ).getComposablesResponse
        nodes = Nodes(composables, parameters)

        assertThat(nodes.button1.recomposeCount).isEqualTo(6)
        assertThat(nodes.text1.recomposeCount).isEqualTo(3)
        assertThat(nodes.button2.recomposeCount).isEqualTo(2)
        assertThat(nodes.text2.recomposeCount).isEqualTo(1)

        // Continue counting:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true, keepRecomposeCounts = true)
        ).updateSettingsResponse

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        composables = inspectorTester.sendCommand(
            GetComposablesCommand(rootId, skipSystemComposables = false)
        ).getComposablesResponse
        nodes = Nodes(composables, parameters)

        // Buttons have double recompose counts, as they are
        // recomposed on the down event for the press indication
        assertThat(nodes.button1.recomposeCount).isEqualTo(8)
        assertThat(nodes.text1.recomposeCount).isEqualTo(4)
        assertThat(nodes.button2.recomposeCount).isEqualTo(4)
        assertThat(nodes.text2.recomposeCount).isEqualTo(2)

        // Continue counting but reset the counts:
        inspectorTester.sendCommand(
            GetUpdateSettingsCommand(includeRecomposeCounts = true, keepRecomposeCounts = false)
        ).updateSettingsResponse

        rule.onNodeWithText("Click row 1").performClick()
        rule.onNodeWithText("Click row 2").performClick()
        rule.waitForIdle()

        composables = inspectorTester.sendCommand(
            GetComposablesCommand(rootId, skipSystemComposables = false)
        ).getComposablesResponse
        nodes = Nodes(composables, parameters)

        // Buttons have double recompose counts, as they are
        // recomposed on the down event for the press indication
        assertThat(nodes.button1.recomposeCount).isEqualTo(2)
        assertThat(nodes.text1.recomposeCount).isEqualTo(1)
        assertThat(nodes.button2.recomposeCount).isEqualTo(2)
        assertThat(nodes.text2.recomposeCount).isEqualTo(1)
    }

    private class Nodes(composables: GetComposablesResponse, parameters: GetAllParametersResponse) {
        val button1 = nodeWithText("Button", composables, parameters) { it == "Click row 1" }
        val button2 = nodeWithText("Button", composables, parameters) { it == "Click row 2" }
        val text1 =
            nodeWithText("Text", composables, parameters) { it.startsWith("Row 1 click count: ") }
        val text2 =
            nodeWithText("Text", composables, parameters) { it.startsWith("Row 2 click count: ") }

        private fun nodeWithText(
            name: String,
            composables: GetComposablesResponse,
            parameters: GetAllParametersResponse,
            predicate: (String) -> Boolean
        ): ComposableNode {
            val strings = composables.stringsList.toMap()
            return composables.rootsList.single().nodesList
                .flatMap { it.flatten() }
                .single { strings[it.name] == name && hasText(it, parameters, predicate) }
        }

        private fun hasText(
            node: ComposableNode,
            parameters: GetAllParametersResponse,
            predicate: (String) -> Boolean
        ): Boolean {
            val strings = parameters.stringsList.toMap()
            val group = parameters.parameterGroupsList.single { it.composableId == node.id }
            if (group.parameterList.any {
                    strings[it.name] == "text" &&
                        it.type == Parameter.Type.STRING &&
                        predicate(strings[it.int32Value]!!)
                }) {
                return true
            }
            return node.childrenList.any { hasText(it, parameters, predicate) }
        }
    }
}