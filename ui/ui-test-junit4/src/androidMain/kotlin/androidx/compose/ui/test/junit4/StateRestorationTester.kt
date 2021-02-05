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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue

/**
 * Helps to test the state restoration for your Composable component.
 *
 * Instead of calling [ComposeContentTestRule.setContent] you need to use [setContent] on this
 * object, then change your state so there is some change to be restored, then execute
 * [emulateSavedInstanceStateRestore] and assert your state is restored properly.
 *
 * Note that this tests only the restoration of the local state of the composable you passed to
 * [setContent] and useful for testing [androidx.compose.runtime.saveable.rememberSaveable]
 * integration. It is not testing the integration with any other life cycles or Activity callbacks.
 */
class StateRestorationTester(private val composeTestRule: ComposeContentTestRule) {

    private var registry: RestorationRegistry? = null

    /**
     * This functions is a direct replacement for [ComposeContentTestRule.setContent] if you are
     * going to use [emulateSavedInstanceStateRestore] in the test.
     *
     * @see ComposeContentTestRule.setContent
     */
    fun setContent(composable: @Composable () -> Unit) {
        composeTestRule.setContent {
            InjectRestorationRegistry { registry ->
                this.registry = registry
                composable()
            }
        }
    }

    /**
     * Saves all the state stored via [savedInstanceState] or [rememberSaveable],
     * disposes current composition, and composes again the content passed to [setContent].
     * Allows to test how your component behaves when the state restoration is happening.
     * Note that the state stored via regular state() or remember() will be lost.
     */
    fun emulateSavedInstanceStateRestore() {
        val registry = checkNotNull(registry) {
            "setContent should be called first!"
        }
        composeTestRule.runOnIdle {
            registry.saveStateAndDisposeChildren()
        }
        composeTestRule.runOnIdle {
            registry.emitChildrenWithRestoredState()
        }
        composeTestRule.runOnIdle {
            // we just wait for the children to be emitted
        }
    }

    @Composable
    private fun InjectRestorationRegistry(content: @Composable (RestorationRegistry) -> Unit) {
        val original = requireNotNull(LocalSaveableStateRegistry.current) {
            "StateRestorationTester requires composeTestRule.setContent() to provide " +
                "a SaveableStateRegistry implementation via LocalSaveableStateRegistry"
        }
        val restorationRegistry = remember { RestorationRegistry(original) }
        CompositionLocalProvider(LocalSaveableStateRegistry provides restorationRegistry) {
            if (restorationRegistry.shouldEmitChildren) {
                content(restorationRegistry)
            }
        }
    }

    private class RestorationRegistry(private val original: SaveableStateRegistry) :
        SaveableStateRegistry {

        var shouldEmitChildren by mutableStateOf(true)
            private set
        private var currentRegistry: SaveableStateRegistry = original
        private var savedMap: Map<String, List<Any?>> = emptyMap()

        fun saveStateAndDisposeChildren() {
            savedMap = currentRegistry.performSave()
            shouldEmitChildren = false
        }

        fun emitChildrenWithRestoredState() {
            currentRegistry = SaveableStateRegistry(
                restoredValues = savedMap,
                canBeSaved = { original.canBeSaved(it) }
            )
            shouldEmitChildren = true
        }

        override fun consumeRestored(key: String) = currentRegistry.consumeRestored(key)

        override fun registerProvider(key: String, valueProvider: () -> Any?) =
            currentRegistry.registerProvider(key, valueProvider)

        override fun canBeSaved(value: Any) = currentRegistry.canBeSaved(value)

        override fun performSave() = currentRegistry.performSave()
    }
}
