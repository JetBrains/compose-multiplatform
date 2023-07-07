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

package androidx.compose.ui.test.junit4

import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters
import org.robolectric.annotation.Config

@RunWith(ParameterizedRobolectricTestRunner::class)
@Config(minSdk = 21)
@OptIn(ExperimentalTestApi::class)
class ViewVisibilityRobolectricTest(private val visibility: Int) {
    companion object {
        @JvmStatic
        @Parameters(name = "visibility={0}")
        fun params() = listOf(
            View.VISIBLE,
            View.INVISIBLE,
            View.GONE
        )
    }

    private var offset by mutableStateOf(0)

    private fun toggleState() {
        offset = 10
    }

    @Test
    fun noTimeout_hostView_visibility() {
        runComposeUiTest {
            setContent {
                val hostView = LocalView.current
                SideEffect {
                    hostView.visibility = visibility
                }
                TestContent()
            }

            val expectDisplayed = visibility == View.VISIBLE
            checkUi(expectDisplayed)
            toggleState()
            checkUi(expectDisplayed)
        }
    }

    @Test
    fun noTimeout_composeView_visibility() {
        runAndroidComposeUiTest<ComponentActivity> {
            runOnUiThread {
                val activity = activity!!
                val composeView = ComposeView(activity)
                composeView.layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                composeView.visibility = visibility
                composeView.setContent {
                    TestContent()
                }
                activity.setContentView(composeView)
            }

            val expectDisplayed = visibility == View.VISIBLE
            checkUi(expectDisplayed)
            toggleState()
            checkUi(expectDisplayed)
        }
    }

    @Composable
    private fun TestContent() {
        // Read state in layout and not in measure, so a state change will trigger layout, but
        // not measure. Because measure is not affected, the containing View doesn't need to be
        // remeasured and Compose will do its measure/layout pass in the draw pass, meaning the
        // View will be invalidated but no layout will be requested.
        Layout({
            Box(Modifier.size(10.dp))
        }, Modifier.fillMaxSize().testTag("box")) { measurables, constraints ->
            val placeable = measurables.first().measure(constraints)
            layout(constraints.maxWidth, constraints.maxHeight) {
                placeable.place(offset, 0)
            }
        }
    }

    private fun SemanticsNodeInteractionsProvider.checkUi(expectDisplayed: Boolean) {
        // It should always exist
        onNodeWithTag("box").assertExists()
        // But only be displayed in tests where visibility = View.VISIBLE
        if (expectDisplayed) {
            onNodeWithTag("box").assertIsDisplayed()
        } else {
            onNodeWithTag("box").assertIsNotDisplayed()
        }
    }
}
