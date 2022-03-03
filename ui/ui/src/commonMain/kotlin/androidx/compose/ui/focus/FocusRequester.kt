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

package androidx.compose.ui.focus

import androidx.compose.runtime.collection.MutableVector
import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.ExperimentalComposeUiApi

private const val focusRequesterNotInitialized = """
   FocusRequester is not initialized. Here are some possible fixes:

   1. Remember the FocusRequester: val focusRequester = remember { FocusRequester() }
   2. Did you forget to add a Modifier.focusRequester() ?
   3. Are you attempting to request focus during composition? Focus requests should be made in
   response to some event. Eg Modifier.clickable { focusRequester.requestFocus() }
"""

/**
 * The [FocusRequester] is used in conjunction with
 * [Modifier.focusRequester][androidx.compose.ui.focus.focusRequester] to send requests to
 * change focus.
 *
 * @sample androidx.compose.ui.samples.RequestFocusSample
 *
 * @see androidx.compose.ui.focus.focusRequester
 */
class FocusRequester {

    internal val focusRequesterModifierLocals: MutableVector<FocusRequesterModifierLocal> =
        mutableVectorOf()

    /**
     * Use this function to request focus. If the system grants focus to a component associated
     * with this [FocusRequester], its [onFocusChanged] modifiers will receive a [FocusState] object
     * where [FocusState.isFocused] is true.
     *
     * @sample androidx.compose.ui.samples.RequestFocusSample
     */
    fun requestFocus() {
        check(focusRequesterModifierLocals.isNotEmpty()) { focusRequesterNotInitialized }
        focusRequesterModifierLocals.forEach { it.findFocusNode()?.requestFocus() }
    }

    /**
     * Deny requests to clear focus.
     *
     * Use this function to send a request to capture focus. If a component captures focus,
     * it will send a [FocusState] object to its associated [onFocusChanged]
     * modifiers where [FocusState.isCaptured]() == true.
     *
     * When a component is in a Captured state, all focus requests from other components are
     * declined.
     *
     * @return true if the focus was successfully captured by one of the
     * [focus][focusTarget] modifiers associated with this [FocusRequester]. False otherwise.
     *
     * @sample androidx.compose.ui.samples.CaptureFocusSample
     */
    fun captureFocus(): Boolean {
        check(focusRequesterModifierLocals.isNotEmpty()) { focusRequesterNotInitialized }
        var success = false
        focusRequesterModifierLocals.forEach {
            it.findFocusNode()?.apply {
                if (captureFocus()) {
                    success = true
                }
            }
        }
        return success
    }

    /**
     * Use this function to send a request to free focus when one of the components associated
     * with this [FocusRequester] is in a Captured state. If a component frees focus,
     * it will send a [FocusState] object to its associated [onFocusChanged]
     * modifiers where [FocusState.isCaptured]() == false.
     *
     * When a component is in a Captured state, all focus requests from other components are
     * declined.
     *.
     * @return true if the captured focus was successfully released. i.e. At the end of this
     * operation, one of the components associated with this [focusRequester] freed focus.
     *
     * @sample androidx.compose.ui.samples.CaptureFocusSample
     */
    fun freeFocus(): Boolean {
        check(focusRequesterModifierLocals.isNotEmpty()) { focusRequesterNotInitialized }
        var success = false
        focusRequesterModifierLocals.forEach {
            it.findFocusNode()?.apply {
                if (freeFocus()) {
                    success = true
                }
            }
        }
        return success
    }

    companion object {
        /**
         * Default [focusRequester], which when used in [Modifier.focusProperties][focusProperties]
         * implies that we want to use the default system focus order, that is based on the
         * position of the items on the screen.
         */
        val Default = FocusRequester()

        /**
         * Convenient way to create multiple [FocusRequester] instances.
         *
         * @sample androidx.compose.ui.samples.CreateFocusRequesterRefsSample
         */
        @ExperimentalComposeUiApi
        object FocusRequesterFactory {
            operator fun component1() = FocusRequester()
            operator fun component2() = FocusRequester()
            operator fun component3() = FocusRequester()
            operator fun component4() = FocusRequester()
            operator fun component5() = FocusRequester()
            operator fun component6() = FocusRequester()
            operator fun component7() = FocusRequester()
            operator fun component8() = FocusRequester()
            operator fun component9() = FocusRequester()
            operator fun component10() = FocusRequester()
            operator fun component11() = FocusRequester()
            operator fun component12() = FocusRequester()
            operator fun component13() = FocusRequester()
            operator fun component14() = FocusRequester()
            operator fun component15() = FocusRequester()
            operator fun component16() = FocusRequester()
        }

        /**
         * Convenient way to create multiple [FocusRequester]s, which can to be used to request
         * focus, or to specify a focus traversal order.
         *
         * @sample androidx.compose.ui.samples.CreateFocusRequesterRefsSample
         */
        @ExperimentalComposeUiApi
        fun createRefs() = FocusRequesterFactory
    }
}
