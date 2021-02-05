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

package androidx.compose.ui.inspection.rules

import android.app.Activity
import android.view.inspector.WindowInspector
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.R
import androidx.compose.ui.platform.ViewRootForTest
import androidx.inspection.testing.InspectorTester
import androidx.test.core.app.ActivityScenario
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response
import org.junit.rules.ExternalResource
import java.util.Collections
import java.util.WeakHashMap
import kotlin.reflect.KClass

class ComposeInspectionRule(val clazz: KClass<out Activity>) : ExternalResource() {
    var rootId: Long = 0
        private set
    lateinit var inspectorTester: InspectorTester
        private set

    override fun before() {
        JvmtiRule.ensureInitialised()
        // need to set this special tag on the root view to enable inspection
        ViewRootForTest.onViewCreatedCallback = {
            it.view.setTag(
                R.id.inspection_slot_table_set,
                Collections.newSetFromMap(WeakHashMap<CompositionData, Boolean>())
            )
            ViewRootForTest.onViewCreatedCallback = null
        }

        ActivityScenario.launch(clazz.java).onActivity {
            val roots = WindowInspector.getGlobalWindowViews().map { it.uniqueDrawingId }
            Truth.assertThat(roots).hasSize(1)
            rootId = roots[0]
        }
        runBlocking {
            inspectorTester = InspectorTester("layoutinspector.compose.inspection")
        }
    }

    override fun after() {
        inspectorTester.dispose()
    }
}

suspend fun InspectorTester.sendCommand(command: Command): Response {
    return Response.parseFrom(sendCommand(command.toByteArray()))
}