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
import androidx.compose.ui.inspection.util.GetParameterDetailsCommand
import androidx.compose.ui.inspection.util.GetParametersCommand
import androidx.compose.ui.inspection.util.flatten
import androidx.compose.ui.inspection.util.toMap
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetParametersResponse
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Parameter
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.StringEntry
import org.junit.Rule
import org.junit.Test

@LargeTest
class ParametersTest {
    @get:Rule
    val rule = ComposeInspectionRule(ParametersTestActivity::class)

    @Test
    fun resource(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        val text = composables.filter("Text").first()
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, text.id))
            .getParametersResponse

        val resourceValue = params.find("fontFamily").resourceValue
        assertThat(resourceValue.type.resolve(params)).isEqualTo("font")
        assertThat(resourceValue.namespace.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.test")
        assertThat(resourceValue.name.resolve(params)).isEqualTo("samplefont")
    }

    @Test
    fun lambda(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        val buttons = composables.filter("Button")
        val buttonId = buttons.first().id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, buttonId))
            .getParametersResponse

        val lambdaValue = params.find("onClick").lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(52)
        assertThat(lambdaValue.endLineNumber).isEqualTo(52)
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun contentLambda(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        val buttons = composables.filter("SomeContent")
        val someId = buttons.single().id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, someId))
            .getParametersResponse

        val lambdaValue = params.find("content").lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(61)
        assertThat(lambdaValue.endLineNumber).isEqualTo(64)
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun functionType(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        val buttons = composables.filter("Button")
        val buttonId = buttons.last().id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, buttonId))
            .getParametersResponse

        val lambdaValue = params.find("onClick").lambdaValue
        assertThat(lambdaValue.fileName.resolve(params)).isEqualTo("ParametersTestActivity.kt")
        assertThat(lambdaValue.startLineNumber).isEqualTo(55)
        assertThat(lambdaValue.endLineNumber).isEqualTo(55)
        assertThat(lambdaValue.functionName.resolve(params)).isEqualTo("testClickHandler")
        assertThat(lambdaValue.packageName.resolve(params))
            .isEqualTo("androidx.compose.ui.inspection.testdata")
    }

    @Test
    fun intArray(): Unit = runBlocking {
        val tester = rule.inspectorTester
        val nodes = tester.sendCommand(GetComposablesCommand(rule.rootId)).getComposablesResponse

        val function = nodes.filter("FunctionWithIntArray").single()
        val params = tester.sendCommand(GetParametersCommand(rule.rootId, function.id))
            .getParametersResponse

        val intArray = params.find("intArray")
        var strings = params.stringsList

        checkStringParam(strings, intArray, "intArray", "IntArray[8]", 0)
        assertThat(intArray.elementsCount).isEqualTo(5)
        checkIntParam(strings, intArray.elementsList[0], "[0]", 10, 0)
        checkIntParam(strings, intArray.elementsList[1], "[1]", 11, 1)
        checkIntParam(strings, intArray.elementsList[2], "[2]", 12, 2)
        checkIntParam(strings, intArray.elementsList[3], "[3]", 13, 3)
        checkIntParam(strings, intArray.elementsList[4], "[4]", 14, 4)

        val expanded =
            tester.sendCommand(
                GetParameterDetailsCommand(
                    rule.rootId,
                    intArray.reference,
                    startIndex = 5,
                    maxElements = 5
                )
            ).getParameterDetailsResponse
        val intArray2 = expanded.parameter
        strings = expanded.stringsList
        checkStringParam(strings, intArray, "intArray", "IntArray[8]", 0)
        assertThat(intArray2.elementsCount).isEqualTo(3)
        checkIntParam(strings, intArray2.elementsList[0], "[5]", 15, 5)
        checkIntParam(strings, intArray2.elementsList[1], "[6]", 16, 6)
        checkIntParam(strings, intArray2.elementsList[2], "[7]", 17, 7)
    }

    @Test
    fun unmergedSemantics(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        val texts = composables.filter("Text")
        val textOne = texts.first().id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, textOne))
            .getParametersResponse

        val text = params.findUnmerged("Text")
        val strings = params.stringsList
        checkStringParam(strings, text, "Text", "one", 0)
    }

    @Test
    fun mergedSemantics(): Unit = runBlocking {
        val composables = rule.inspectorTester.sendCommand(GetComposablesCommand(rule.rootId))
            .getComposablesResponse

        val texts = composables.filter("Column")
        val id = texts.first().id
        val params = rule.inspectorTester.sendCommand(GetParametersCommand(rule.rootId, id))
            .getParametersResponse

        val text = params.findMerged("Text")
        val strings = params.stringsList
        checkStringParam(strings, text, "Text", "three, four, five", 0)
    }
}

private fun Int.resolve(response: GetParametersResponse): String? {
    return response.stringsList.toMap()[this]
}

private fun GetParametersResponse.find(name: String): Parameter {
    val strings = stringsList.toMap()
    val params = parameterGroup.parameterList.associateBy { strings[it.name] }
    return params[name]
        ?: error("$name not found in parameters. Found: ${params.keys.joinToString()}")
}

private fun GetParametersResponse.findUnmerged(name: String): Parameter {
    val strings = stringsList.toMap()
    val semantics = parameterGroup.unmergedSemanticsList.associateBy { strings[it.name] }
    return semantics[name]
        ?: error("$name not found in unmerged semantics. Found: ${semantics.keys.joinToString()}")
}

private fun GetParametersResponse.findMerged(name: String): Parameter {
    val strings = stringsList.toMap()
    val semantics = parameterGroup.mergedSemanticsList.associateBy { strings[it.name] }
    return semantics[name]
        ?: error("$name not found in merged semantics. Found: ${semantics.keys.joinToString()}")
}

private fun GetComposablesResponse.filter(name: String): List<ComposableNode> {
    val strings = stringsList.toMap()
    return rootsList.flatMap { it.nodesList }.flatMap { it.flatten() }.filter {
        strings[it.name] == name
    }
}

@Suppress("SameParameterValue")
private fun checkStringParam(
    stringList: List<StringEntry>,
    param: Parameter,
    name: String,
    value: String,
    index: Int = 0
) {
    assertThat(stringList.toMap()[param.name]).isEqualTo(name)
    assertThat(stringList.toMap()[param.int32Value]).isEqualTo(value)
    assertThat(param.index).isEqualTo(index)
}

private fun checkIntParam(
    stringList: List<StringEntry>,
    param: Parameter,
    name: String,
    value: Int,
    index: Int = 0
) {
    assertThat(stringList.toMap()[param.name]).isEqualTo(name)
    assertThat(param.int32Value).isEqualTo(value)
    assertThat(param.index).isEqualTo(index)
}
