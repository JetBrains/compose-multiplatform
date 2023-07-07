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

import android.view.View
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.tooling.CompositionData
import androidx.compose.ui.Modifier
import androidx.compose.ui.R
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.ViewRootForTest
import androidx.inspection.testing.InspectorTester
import androidx.test.core.app.ActivityScenario
import kotlinx.coroutines.runBlocking
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Command
import layoutinspector.compose.inspection.LayoutInspectorComposeProtocol.Response
import org.junit.rules.ExternalResource
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

/**
 * Test rule with common setup for compose inspector's tests:
 * - it enables JVMTI;
 * - starts a [clazz] activity;
 * - enables inspection mode in compose in this activity;
 * - starts a compose inspector itself if [useInspector] is `true`
 *
 * @param clazz an activity to start for a test
 * @param useInspector parameter to enable / disable creation of inspector itself. By default,
 * it is true. However a test may not need an inspector because it works with underlying infra,
 * in such cases `false` can be passed to speed up test a bit.
 */
class ComposeInspectionRule(
    val clazz: KClass<out ComponentActivity>,
    private val useInspector: Boolean = true
) : ExternalResource() {
    val rootsForTest = mutableListOf<ViewRootForTest>()
    val roots = mutableListOf<View>()
    val rootId: Long
        get() = roots.single().uniqueDrawingId
    lateinit var inspectorTester: InspectorTester
        private set

    @Suppress("UNCHECKED_CAST")
    private val compositionDataSet: Collection<CompositionData>
        get() = rootsForTest.single().view.getTag(R.id.inspection_slot_table_set)
            as Collection<CompositionData>

    val compositionData: CompositionData
        get() = compositionDataSet.first()

    private lateinit var activityScenario: ActivityScenario<out ComponentActivity>

    override fun before() {
        JvmtiRule.ensureInitialised()
        // need to set this special tag on the root view to enable inspection
        ViewRootForTest.onViewCreatedCallback = {
            rootsForTest.add(it)
            it.view.setTag(
                R.id.inspection_slot_table_set,
                Collections.newSetFromMap(WeakHashMap<CompositionData, Boolean>())
            )
        }

        activityScenario = ActivityScenario.launch(clazz.java)
        activityScenario.onActivity {
            roots.addAll(WindowInspector.getGlobalWindowViews())
        }
        if (!useInspector) return
        runBlocking {
            inspectorTester = InspectorTester("layoutinspector.compose.inspection")
        }
    }

    fun show(composable: @Composable () -> Unit) = activityScenario.show(composable)

    override fun after() {
        if (useInspector) inspectorTester.dispose()
        ViewRootForTest.onViewCreatedCallback = null
    }
}

fun ActivityScenario<out ComponentActivity>.show(composable: @Composable () -> Unit) {
    val positionedLatch = CountDownLatch(1)
    onActivity {
        it.setContent {
            Box(
                Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { positionedLatch.countDown() }
            ) {
                composable()
            }
        }
    }
    // Wait for the layout to be performed
    positionedLatch.await(1, TimeUnit.SECONDS)

    // Wait for the UI thread to complete its current work so we know that layout is done.
    onActivity { }
}

suspend fun InspectorTester.sendCommand(command: Command): Response {
    return Response.parseFrom(sendCommand(command.toByteArray()))
}