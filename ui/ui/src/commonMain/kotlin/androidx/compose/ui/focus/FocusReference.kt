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
import androidx.compose.ui.node.ModifiedFocusReferenceNode

private val focusReferenceNotInitialized = "FocusReference is not initialized. One reason for " +
    "this is that you requesting focus changes during composition. Focus references should " +
    "not be made during composition, but should be made in response to some event."

@Deprecated(
    message = "Use FocusReference instead",
    replaceWith = ReplaceWith("FocusReference", "androidx.compose.ui.focus.FocusReference"),
    level = DeprecationLevel.ERROR
)
class FocusRequester {
    fun requestFocus() {}
    fun captureFocus(): Boolean = false
    fun freeFocus(): Boolean = false
}

/**
 * The [FocusReference] is used in conjunction with
 * [Modifier.focusReference][androidx.compose.ui.focus.focusReference] to send requests for focus
 * state change.
 *
 * @see androidx.compose.ui.focus.focusReference
 */
class FocusReference {

    internal val focusReferenceNodes: MutableVector<ModifiedFocusReferenceNode> = mutableVectorOf()

    /**
     * Use this function to request focus. If the system grants focus to a component associated
     * with this [FocusReference], its [state][FocusState] will be set to
     * [Active][FocusState.Active].
     */
    fun requestFocus() {
        check(focusReferenceNodes.isNotEmpty()) { focusReferenceNotInitialized }
        focusReferenceNodes.forEach { it.findFocusNode()?.requestFocus(propagateFocus = false) }
    }

    /**
     * Deny requests to clear focus.
     *
     * Use this function to send a request to capture the focus. If a component is captured,
     * its [state][FocusState] will be set to [Captured][FocusState.Captured]. When a
     * component is in this state, it holds onto focus until [freeFocus] is called. When a
     * component is in the [Captured][FocusState.Captured] state, all focus requests from
     * other components are declined.
     *
     * @return true if the focus was successfully captured by one of the
     * [focus][androidx.compose.ui.focus] modifiers associated with this [FocusReference].
     * false otherwise.
     */
    fun captureFocus(): Boolean {
        check(focusReferenceNodes.isNotEmpty()) { focusReferenceNotInitialized }
        var success = false
        focusReferenceNodes.forEach {
            it.findFocusNode()?.apply {
                if (captureFocus()) {
                    success = true
                }
            }
        }
        return success
    }

    /**
     * Use this function to send a request to release focus when one of the components associated
     * with this [FocusReference] is in a [Captured][FocusState.Captured] state.
     *
     * When the node is in the [Captured][FocusState.Captured] state, it rejects all requests to
     * clear focus. Calling
     * [freeFocus] puts the node in the [Active][FocusState.Active] state, where it is no longer
     * preventing other
     * nodes from requesting focus.
     *
     * @return true if the focus was successfully released. i.e. At the end of this operation,
     * one of the components associated with this
     * [focusReference][androidx.compose.ui.focus.focusReference] is in the
     * [Active][FocusState.Active] state. false otherwise.
     */
    fun freeFocus(): Boolean {
        check(focusReferenceNodes.isNotEmpty()) { focusReferenceNotInitialized }
        var success = false
        focusReferenceNodes.forEach {
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
         * Convenient way to create multiple [FocusReference] instances.
         */
        object FocusReferenceFactory {
            operator fun component1() = FocusReference()
            operator fun component2() = FocusReference()
            operator fun component3() = FocusReference()
            operator fun component4() = FocusReference()
            operator fun component5() = FocusReference()
            operator fun component6() = FocusReference()
            operator fun component7() = FocusReference()
            operator fun component8() = FocusReference()
            operator fun component9() = FocusReference()
            operator fun component10() = FocusReference()
            operator fun component11() = FocusReference()
            operator fun component12() = FocusReference()
            operator fun component13() = FocusReference()
            operator fun component14() = FocusReference()
            operator fun component15() = FocusReference()
            operator fun component16() = FocusReference()
        }

        /**
         * Convenient way to create multiple [FocusReference]s, which can to be used to request
         * focus, or to specify a focus traversal order.
         */
        fun createRefs() = FocusReferenceFactory
    }
}
