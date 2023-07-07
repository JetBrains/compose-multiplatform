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

package androidx.compose.ui.test

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
 * Instead of calling [ComposeUiTest.setContent] you need to use [setContent] on this
 * object, then change your state so there is some change to be restored, then execute
 * [emulateSaveAndRestore] and assert your state is restored properly.
 *
 * Note that this only tests the restoration of the local state of the composable you passed to
 * [setContent] and it is useful for testing uses of
 * [rememberSaveable][androidx.compose.runtime.saveable.rememberSaveable]. It is not testing the
 * integration with app and/or platform specific lifecycles.
 */
@ExperimentalTestApi
class StateRestorationTester(private val composeTest: ComposeUiTest) {

    private var registry: RestorationRegistry? = null

    /**
     * This functions is a direct replacement for [ComposeUiTest.setContent] if you are
     * going to use [emulateSaveAndRestore] in the test.
     *
     * @see ComposeUiTest.setContent
     */
    fun setContent(composable: @Composable () -> Unit) {
        composeTest.setContent {
            InjectRestorationRegistry { registry ->
                this.registry = registry
                composable()
            }
        }
    }

    /**
     * Emulates a save and restore cycle of the current composition. First all state that is
     * remembered with [rememberSaveable][androidx.compose.runtime.saveable.rememberSaveable]
     * is stored, then the current composition is disposed, and finally the composition is
     * composed again. This allows you to test how your component behaves when state
     * restoration is happening. Note that state stored via [remember] will be lost.
     */
    fun emulateSaveAndRestore() {
        val registry = checkNotNull(registry) {
            "setContent should be called first!"
        }
        composeTest.runOnIdle {
            registry.saveStateAndDisposeChildren()
        }
        composeTest.runOnIdle {
            registry.emitChildrenWithRestoredState()
        }
        composeTest.runOnIdle {
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
