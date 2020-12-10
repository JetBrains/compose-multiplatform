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

import androidx.compose.ui.ExperimentalComposeUiApi

/**
 * The autofill tree is a temporary data structure that is used before the Semantics Tree is
 * implemented. This data structure is used by compose components to set autofill
 * hints (via [AutofillNode]s). It is also used  by the autofill framework to communicate with
 * Compose components (by calling [performAutofill]).
 *
 * The [AutofillTree] will be replaced by Autofill Semantics (b/138604305).
 *
 * Since this is a temporary implementation, it is implemented as a list of [children], which is
 * essentially a tree of height = 1
 */
@ExperimentalComposeUiApi
class AutofillTree {
    /**
     * A map which contains [AutofillNode]s, where every node represents an autofillable field.
     */
    val children: MutableMap<Int, AutofillNode> = mutableMapOf()

    /**
     * Add the specified [AutofillNode] to the [AutofillTree].
     */
    operator fun plusAssign(autofillNode: AutofillNode) {
        children[autofillNode.id] = autofillNode
    }

    /**
     * The autofill framework uses this function to 'fill' the [AutofillNode] represented by
     * [id] with the specified [value].
     */
    fun performAutofill(id: Int, value: String) = children[id]?.onFill?.invoke(value)
}