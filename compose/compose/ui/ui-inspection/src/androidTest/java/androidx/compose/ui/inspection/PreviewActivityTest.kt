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

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.inspection.framework.ancestors
import androidx.compose.ui.inspection.inspector.RESERVED_FOR_GENERATED_IDS
import androidx.compose.ui.inspection.rules.DebugViewAttributeRule
import androidx.compose.ui.inspection.rules.sendCommand
import androidx.compose.ui.inspection.util.GetComposablesCommand
import androidx.compose.ui.inspection.util.toMap
import androidx.compose.ui.tooling.PreviewActivity
import androidx.inspection.testing.InspectorTester
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

/**
 * Test composable from a Preview.
 *
 * A [PreviewActivity] loads the composable content via reflection.
 * Make sure that the top composable correspond to a CallGroup with an anchor,
 * such that the Layout Inspector can identify the composable by its id after
 * a recomposition.
 */
class PreviewActivityTest {

    @Suppress("DEPRECATION")
    private val activityTestRule = androidx.test.rule.ActivityTestRule(PreviewActivity::class.java)

    @get:Rule
    val chain = RuleChain.outerRule(DebugViewAttributeRule()).around(activityTestRule)!!

    private lateinit var intent: Intent

    @Before
    fun setup() {
        intent = Intent(activityTestRule.activity, PreviewActivity::class.java)
    }

    @Test
    fun testPreviewTopComposableHasAnAchor(): Unit = runBlocking() {
        intent.putExtra(
            "composable",
            "androidx.compose.ui.inspection.PreviewActivityTestKt.MainBlock"
        )
        activityTestRule.launchActivity(intent)
        val mainContent: View =
            activityTestRule.activity.findViewById<ViewGroup>(android.R.id.content)
        val root = mainContent.ancestors().lastOrNull()
        val inspectorTester = InspectorTester("layoutinspector.compose.inspection")
        val rootId = root!!.uniqueDrawingId
        val composables = inspectorTester.sendCommand(
            GetComposablesCommand(rootId, skipSystemComposables = false)
        ).getComposablesResponse

        assertThat(composables.rootsList).hasSize(1)
        val strings = composables.stringsList.toMap()
        val node = composables.rootsList.single().nodesList.first()
        assertThat(strings[node.name]).isEqualTo("MainBlock")
        assertThat(node.id).isLessThan(RESERVED_FOR_GENERATED_IDS)
    }

    private fun View.isAndroidComposeView(): Boolean {
        return javaClass.canonicalName == "androidx.compose.ui.platform.AndroidComposeView"
    }
}

@Suppress("unused")
@Composable
fun MainBlock() {
    Button(onClick = {}) {
        Text("Hello")
    }
}
