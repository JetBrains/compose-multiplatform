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

import androidx.compose.ui.inspection.inspector.InspectorNode
import androidx.compose.ui.inspection.inspector.MutableInspectorNode
import androidx.compose.ui.inspection.rules.ComposeInspectionRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.testdata.DialogTestActivity
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.toMap
import androidx.compose.ui.unit.IntRect
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.ComposableNode
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.GetComposablesResponse
import org.junit.Rule
import org.junit.Test

@LargeTest
class DialogTest {
    @get:Rule
    val rule = ComposeInspectionRule(DialogTestActivity::class)

    @Test
    fun dialogLocation(): Unit = runBlocking {
        assertThat(rule.roots).hasSize(2)
        val appViewId = rule.roots.first().uniqueDrawingId
        val dialogViewId = rule.roots.last().uniqueDrawingId
        val app = rule.inspectorTester.sendCommand(GetComposablesCommand(appViewId))
            .getComposablesResponse
        val dialog = rule.inspectorTester.sendCommand(GetComposablesCommand(dialogViewId))
            .getComposablesResponse
        val appRoots = app.roots()
        val dialogRoots = dialog.roots()
        val dialogViewRoot = rule.roots.last()
        assertThat(appRoots).hasSize(1)
        assertThat(dialogRoots).hasSize(1)
        assertThat(appRoots.single().name).isEqualTo("Column")
        assertThat(dialogRoots.single().name).isEqualTo("AlertDialog")
        val location = IntArray(2)
        dialogViewRoot.getLocationOnScreen(location)
        assertThat(dialogRoots.single().left).isEqualTo(location[0])
        assertThat(dialogRoots.single().top).isEqualTo(location[1])
        assertThat(dialogRoots.single().width).isEqualTo(dialogViewRoot.width)
        assertThat(dialogRoots.single().height).isEqualTo(dialogViewRoot.height)
    }

    private fun GetComposablesResponse.roots(): List<InspectorNode> {
        val strings = stringsList.toMap()
        return rootsList.flatMap { it.nodesList.convert(strings) }
    }

    private fun List<ComposableNode>.convert(strings: Map<Int, String>): List<InspectorNode> =
        map {
            val node = MutableInspectorNode()
            node.name = strings[it.name] ?: ""
            node.box = IntRect(
                it.bounds.layout.x,
                it.bounds.layout.y,
                it.bounds.layout.x + it.bounds.layout.w,
                it.bounds.layout.y + it.bounds.layout.h
            )
            node.children.addAll(it.childrenList.convert(strings))
            node.build()
        }
}
