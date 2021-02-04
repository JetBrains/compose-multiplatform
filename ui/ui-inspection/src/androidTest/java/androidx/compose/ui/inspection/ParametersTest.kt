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
import androidx.compose.ui.inspection.testdata.ParametersTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.GetParametersCommand
import androidx.compose.ui.inspection.util.toMap
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import org.junit.Rule
import org.junit.Test

@MediumTest
class ParametersTest {
    @get:Rule
    val rule = ComposeInspectionRule(ParametersTestActivity::class)

    @Test
    fun lambda(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse
        // first button's id
        val buttonId = composables.rootsList[0]!!.nodesList[0]!!.childrenList[0]!!.id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, buttonId))
            .getParametersResponse

        val lambdaValue = params.find("onClick")!!.lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(29)
        assertThat(lambdaValue.endLineNumber).isEqualTo(29)
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun functionType(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        // second's button id
        val buttonId = composables.rootsList[0]!!.nodesList[0]!!.childrenList[1]!!.id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, buttonId))
            .getParametersResponse

        val lambdaValue = params.find("onClick")!!.lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(32)
        assertThat(lambdaValue.endLineNumber).isEqualTo(32)
        assertThat(lambdaValue.functionName.resolve(params)).isEqualTo("testClickHandler")
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }
}

private fun Int.resolve(response: GetParametersResponse): String? {
    return response.stringsList.toMap()[this]
}

private fun GetParametersResponse.find(name: String): Parameter? {
    val strings = stringsList.toMap()
    return parameterGroup.parameterList.find {
        strings[it.name] == name
    }
}