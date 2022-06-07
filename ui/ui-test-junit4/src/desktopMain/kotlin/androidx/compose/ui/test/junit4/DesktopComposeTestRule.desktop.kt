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

package androidx.compose.ui.test.junit4

import androidx.compose.runtime.Composable
import androidx.compose.ui.ComposeScene
import androidx.compose.ui.test.DesktopComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.InternalTestApi
import androidx.compose.ui.test.MainTestClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.unit.Density
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(InternalTestApi::class)
actual fun createComposeRule(): ComposeContentTestRule = DesktopComposeTestRule()

@InternalTestApi
@OptIn(ExperimentalTestApi::class)
class DesktopComposeTestRule private constructor(
    private val composeTest: DesktopComposeUiTest
) : ComposeContentTestRule {

    constructor() : this(DesktopComposeUiTest())

    var scene: ComposeScene
        get() = composeTest.scene
        set(value) {
            composeTest.scene = value
        }

    override fun apply(base: Statement, description: Description?): Statement {
        return object : Statement() {
            override fun evaluate() {
                composeTest.runTest {
                    base.evaluate()
                }
            }
        }
    }

    /*
     * WHEN THE NAME AND SHAPE OF THE NEW COMMON INTERFACES HAS BEEN DECIDED,
     * REPLACE ALL OVERRIDES BELOW WITH DELEGATION: ComposeTest by composeTest
     */

    override val density: Density = composeTest.density

    override val mainClock: MainTestClock = composeTest.mainClock

    override fun <T> runOnUiThread(action: () -> T): T = composeTest.runOnUiThread(action)

    override fun <T> runOnIdle(action: () -> T): T = composeTest.runOnIdle(action)

    override fun waitForIdle() = composeTest.waitForIdle()

    override suspend fun awaitIdle() = composeTest.awaitIdle()

    override fun waitUntil(timeoutMillis: Long, condition: () -> Boolean) =
        composeTest.waitUntil(timeoutMillis, condition)

    override fun registerIdlingResource(idlingResource: IdlingResource) =
        composeTest.registerIdlingResource(idlingResource)

    override fun unregisterIdlingResource(idlingResource: IdlingResource) =
        composeTest.unregisterIdlingResource(idlingResource)

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction = composeTest.onNode(matcher, useUnmergedTree)

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection = composeTest.onAllNodes(matcher, useUnmergedTree)

    override fun setContent(composable: @Composable () -> Unit) = composeTest.setContent(composable)
}
