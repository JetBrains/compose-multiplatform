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

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.blinkingCursorEnabled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Recomposer
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.Owner
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.test.ExperimentalTesting
import androidx.compose.ui.test.InternalTestingApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.TestOwner
import androidx.compose.ui.test.createTestContext
import androidx.compose.ui.test.junit4.android.AndroidOwnerRegistry
import androidx.compose.ui.test.junit4.android.FirstDrawRegistry
import androidx.compose.ui.test.junit4.android.SynchronizedTreeCollector
import androidx.compose.ui.test.junit4.android.registerComposeWithEspresso
import androidx.compose.ui.test.junit4.android.unregisterComposeFromEspresso
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.text.input.EditOperation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.textInputServiceFactory
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.rules.RuleChain
import org.junit.runner.Description
import org.junit.runners.model.Statement

actual fun createComposeRule(): ComposeTestRule = createAndroidComposeRule<ComponentActivity>()

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * activity class type [T].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * app tests. Make sure that you add the provided activity into your app's manifest file (usually
 * in main/AndroidManifest.xml).
 *
 * If you don't care about specific activity and just want to test composables in general, see
 * [createComposeRule].
 */
inline fun <reified T : ComponentActivity> createAndroidComposeRule(): AndroidComposeTestRule<T> {
    // TODO(b/138993381): By launching custom activities we are losing control over what content is
    //  already there. This is issue in case the user already set some compose content and decides
    //  to set it again via our API. In such case we won't be able to dispose the old composition.
    //  Other option would be to provide a smaller interface that does not expose these methods.
    return AndroidComposeTestRule(ActivityScenarioRule(T::class.java))
}

/**
 * Factory method to provide android specific implementation of [createComposeRule], for a given
 * [activityClass].
 *
 * This method is useful for tests that require a custom Activity. This is usually the case for
 * app tests. Make sure that you add the provided activity into your app's manifest file (usually
 * in main/AndroidManifest.xml).
 *
 * If you don't care about specific activity and just want to test composables in general, see
 * [createComposeRule].
 */
fun <T : ComponentActivity> createAndroidComposeRule(
    activityClass: Class<T>
): AndroidComposeTestRule<T> = AndroidComposeTestRule(
    ActivityScenarioRule(activityClass)
)

/**
 * Android specific implementation of [ComposeTestRule].
 */
@OptIn(InternalTestingApi::class)
class AndroidComposeTestRule<T : ComponentActivity>(
    // TODO(b/153623653): Remove activityRule from arguments when AndroidComposeTestRule can
    //  work with any kind of Activity launcher.
    val activityRule: ActivityScenarioRule<T>
) : ComposeTestRule {

    private fun getActivity(): T {
        var activity: T? = null
        if (activity == null) {
            activityRule.scenario.onActivity { activity = it }
            if (activity == null) {
                throw IllegalStateException("Activity was not set in the ActivityScenarioRule!")
            }
        }
        return activity!!
    }

    override val clockTestRule: AnimationClockTestRule = AndroidAnimationClockTestRule()

    internal var disposeContentHook: (() -> Unit)? = null

    private val testOwner = AndroidTestOwner()
    private val testContext = createTestContext(testOwner)

    override val density: Density
        get() =
            Density(getActivity().resources.displayMetrics.density)

    override val displaySize by lazy {
        getActivity().resources.displayMetrics.let {
            IntSize(it.widthPixels, it.heightPixels)
        }
    }

    override fun apply(base: Statement, description: Description?): Statement {
        @Suppress("NAME_SHADOWING")
        @OptIn(InternalTestingApi::class)
        return RuleChain
            .outerRule(clockTestRule)
            .around { base, _ -> AndroidComposeStatement(base) }
            .around(activityRule)
            .apply(base, description)
    }

    /**
     * @throws IllegalStateException if called more than once per test.
     */
    @SuppressWarnings("SyntheticAccessor")
    override fun setContent(composable: @Composable () -> Unit) {
        check(disposeContentHook == null) {
            "Cannot call setContent twice per test!"
        }

        lateinit var activity: T
        activityRule.scenario.onActivity { activity = it }

        runOnUiThread {
            val composition = activity.setContent(
                Recomposer.current(),
                composable
            )
            disposeContentHook = {
                composition.dispose()
            }
        }

        if (!isOnUiThread()) {
            // Only wait for idleness if not on the UI thread. If we are on the UI thread, the
            // caller clearly wants to keep tight control over execution order, so don't go
            // executing future tasks on the main thread.
            waitForIdle()
        }
    }

    override fun waitForIdle() {
        SynchronizedTreeCollector.waitForIdle()
    }

    @ExperimentalTesting
    override suspend fun awaitIdle() {
        SynchronizedTreeCollector.awaitIdle()
    }

    override fun <T> runOnUiThread(action: () -> T): T {
        return testOwner.runOnUiThread(action)
    }

    override fun <T> runOnIdle(action: () -> T): T {
        // Method below make sure that compose is idle.
        waitForIdle()
        // Execute the action on ui thread in a blocking way.
        return runOnUiThread(action)
    }

    inner class AndroidComposeStatement(
        private val base: Statement
    ) : Statement() {
        @OptIn(InternalTextApi::class)
        override fun evaluate() {
            @Suppress("DEPRECATION_ERROR")
            val oldTextInputFactory = textInputServiceFactory
            beforeEvaluate()
            try {
                base.evaluate()
            } finally {
                afterEvaluate()
                @Suppress("DEPRECATION_ERROR")
                textInputServiceFactory = oldTextInputFactory
            }
        }

        @OptIn(InternalTextApi::class)
        private fun beforeEvaluate() {
            @Suppress("DEPRECATION_ERROR")
            blinkingCursorEnabled = false
            AndroidOwnerRegistry.setupRegistry()
            FirstDrawRegistry.setupRegistry()
            registerComposeWithEspresso()
            @Suppress("DEPRECATION_ERROR")
            textInputServiceFactory = {
                TextInputServiceForTests(it)
            }
        }

        @OptIn(InternalTextApi::class)
        private fun afterEvaluate() {
            @Suppress("DEPRECATION_ERROR")
            blinkingCursorEnabled = true
            AndroidOwnerRegistry.tearDownRegistry()
            FirstDrawRegistry.tearDownRegistry()
            unregisterComposeFromEspresso()
            // Dispose the content
            if (disposeContentHook != null) {
                runOnUiThread {
                    // NOTE: currently, calling dispose after an exception that happened during
                    // composition is not a safe call. Compose runtime should fix this, and then
                    // this call will be okay. At the moment, however, calling this could
                    // itself produce an exception which will then obscure the original
                    // exception. To fix this, we will just wrap this call in a try/catch of
                    // its own
                    try {
                        disposeContentHook!!()
                    } catch (e: Exception) {
                        // ignore
                    }
                    disposeContentHook = null
                }
            }
        }
    }

    override fun onNode(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteraction {
        return SemanticsNodeInteraction(testContext, useUnmergedTree, matcher)
    }

    override fun onAllNodes(
        matcher: SemanticsMatcher,
        useUnmergedTree: Boolean
    ): SemanticsNodeInteractionCollection {
        return SemanticsNodeInteractionCollection(testContext, useUnmergedTree, matcher)
    }

    private class AndroidTestOwner : TestOwner {
        @SuppressLint("DocumentExceptions")
        override fun sendTextInputCommand(node: SemanticsNode, command: List<EditOperation>) {
            @OptIn(ExperimentalLayoutNodeApi::class)
            val owner = node.componentNode.owner as AndroidOwner

            @Suppress("DEPRECATION")
            runOnUiThread {
                val textInputService = owner.getTextInputServiceOrDie()

                val onEditCommand = textInputService.onEditCommand
                    ?: throw IllegalStateException("No input session started. Missing a focus?")

                onEditCommand(command)
            }
        }

        @SuppressLint("DocumentExceptions")
        override fun sendImeAction(node: SemanticsNode, actionSpecified: ImeAction) {
            @OptIn(ExperimentalLayoutNodeApi::class)
            val owner = node.componentNode.owner as AndroidOwner

            @Suppress("DEPRECATION")
            runOnUiThread {
                val textInputService = owner.getTextInputServiceOrDie()

                val onImeActionPerformed = textInputService.onImeActionPerformed
                    ?: throw IllegalStateException("No input session started. Missing a focus?")

                onImeActionPerformed.invoke(actionSpecified)
            }
        }

        @SuppressLint("DocumentExceptions")
        override fun <T> runOnUiThread(action: () -> T): T {
            return androidx.compose.ui.test.junit4.runOnUiThread(action)
        }

        override fun getOwners(): Set<Owner> {
            return SynchronizedTreeCollector.getOwners()
        }

        private fun AndroidOwner.getTextInputServiceOrDie(): TextInputServiceForTests {
            return this.textInputService as TextInputServiceForTests?
                ?: throw IllegalStateException (
                    "Text input service wrapper not set up! Did you use " +
                        "ComposeTestRule?"
                )
        }
    }
}