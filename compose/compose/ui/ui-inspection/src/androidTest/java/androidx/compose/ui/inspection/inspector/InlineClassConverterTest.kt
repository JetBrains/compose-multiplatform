/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.inspection.inspector

import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.testdata.TestActivity
import androidx.compose.ui.tooling.data.Group
import androidx.compose.ui.tooling.data.UiToolingDataApi
import androidx.compose.ui.tooling.data.asTree
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(UiToolingDataApi::class)
class InlineClassConverterTest {
    @get:Rule
    val rule = ComposeInspectionRule(TestActivity::class, false)

    @Test
    fun parameterValueTest() {
        rule.show {
            Surface {
                Text(text = "OK", fontSize = 12.sp)
            }
        }

        val tree = rule.compositionData.asTree()
        val groups = flatten(tree)
        val surface = find(groups, "Surface")
        val text = find(groups, "Text")

        val mapper = InlineClassConverter()

        fun validate(caller: Group, parameterName: String, valueType: Class<*>) {
            val parameter = caller.parameters.single { it.name == parameterName }
            val value = mapper.castParameterValue(parameter.inlineClass, parameter.value)
            assertThat(value).isInstanceOf(valueType)
        }

        validate(surface, "color", Color::class.java)
        validate(surface, "elevation", Dp::class.java)
        validate(text, "color", Color::class.java)
        validate(text, "fontSize", TextUnit::class.java)
    }

    private fun flatten(group: Group): Sequence<Group> =
        sequenceOf(group).plus(group.children.asSequence().flatMap { flatten(it) })

    private fun find(groups: Sequence<Group>, calleeName: String) =
        groups.first {
            it.parameters.isNotEmpty() && it.name == calleeName
        }
}
