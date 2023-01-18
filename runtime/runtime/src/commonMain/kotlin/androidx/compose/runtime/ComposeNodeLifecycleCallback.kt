/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.runtime

/**
 * Observes lifecycle of the node emitted with [ReusableComposeNode] or [ComposeNode] inside
 * [ReusableContentHost] and [ReusableContent].
 *
 * The [ReusableContentHost] introduces the concept of reusing (or recycling) nodes, as well as
 * deactivating parts of composition, while keeping the nodes around to reuse common structures
 * in the next iteration. In this state, [RememberObserver] is not sufficient to track lifetime
 * of data associated with reused node, as deactivated or reused parts of composition is disposed.
 *
 * These callbacks track intermediate states of the node in reusable groups for managing
 * data contained inside reusable nodes or associated with them (e.g. subcomposition).
 *
 * Important: the runtime only supports node implementation of this interface.
 */
interface ComposeNodeLifecycleCallback {
    /**
     * Invoked when the node was reused in the composition.
     * Consumers might use this callback to reset data associated with the previous content, as
     * it is no longer valid.
     */
    fun onReuse()

    /**
     * Invoked when the group containing the node was deactivated.
     * This happens when the content of [ReusableContentHost] is deactivated.
     *
     * The node will not be reused in this recompose cycle, but might be reused or released in
     * the future. Consumers might use this callback to release expensive resources or stop
     * continuous process that was dependent on the node being used in composition.
     *
     * If the node is reused, [onReuse] will be called again to prepare the node for reuse.
     * Similarly, [onRelease] will indicate that deactivated node will never be reused again.
     */
    fun onDeactivate()

    /**
     * Invoked when the node exits the composition entirely and won't be reused again.
     * All intermediate data related to the node can be safely disposed.
     */
    fun onRelease()
}
