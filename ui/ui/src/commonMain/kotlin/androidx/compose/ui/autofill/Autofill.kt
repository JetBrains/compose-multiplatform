/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose.ui.autofill

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.util.annotation.GuardedBy

/**
 * Autofill API.
 *
 * This interface is available to all composables via an ambient. The composable can then request
 * or cancel autofill as required. For instance, the [TextField] can call [requestAutofillForNode]
 * when it gains focus, and [cancelAutofillForNode] when it loses focus.
 */
interface Autofill {

    /**
     * Request autofill for the specified node.
     *
     * @param autofillNode The node that needs to be autofilled.
     *
     * This function is usually called when an autofillable component gains focus.
     */
    fun requestAutofillForNode(autofillNode: AutofillNode)

    /**
     * Cancel a previously supplied autofill request.
     *
     * @param autofillNode The node that needs to be autofilled.
     *
     * This function is usually called when an autofillable component loses focus.
     */
    fun cancelAutofillForNode(autofillNode: AutofillNode)
}

/**
 * Every autofillable composable will have an [AutofillNode]. (An autofill node will be created
 * for every semantics node that adds autofill properties). This node is used to request/cancel
 * autofill, and it holds the [onFill] lambda which is called by the autofill framework.
 *
 * @property autofillTypes A list of autofill types for this node. These types are conveyed to the
 * autofill framework and it is used to call [onFill] with the appropriate value. If you don't set
 * this property, the autofill framework will use heuristics to guess the type. This property is a
 * list because some fields can have multiple types. For instance, userid in a login form can
 * either be a username or an email address. TODO(b/138731416): Check with the autofill service
 * team if the order matters, and how duplicate types are handled.
 *
 * @property boundingBox The screen coordinates of the composable being autofilled.
 * This data is used by the autofill framework to decide where to show the autofill popup.
 *
 * @property onFill The callback that is called by the autofill framework to perform autofill.
 *
 * @property id A virtual id that is automatically generated for each node.
 */
data class AutofillNode(
    val autofillTypes: List<AutofillType> = listOf(),
    var boundingBox: Rect? = null,
    val onFill: ((String) -> Unit)?
) {
    internal companion object {
        @GuardedBy("this")
        private var previousId = 0

        @Suppress("DEPRECATION_ERROR")
        private fun generateId() =
            synchronized(this) { ++previousId }
    }

    val id: Int = generateId()
}