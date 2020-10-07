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

package androidx.compose.runtime

/**
 * Schedule [effect] to run when the current composition completes successfully and applies
 * changes. [SideEffect] can be used to apply side effects to objects managed by the
 * composition that are not backed by [snapshots][androidx.compose.runtime.snapshots.Snapshot] so
 * as not to leave those objects in an inconsistent state if the current composition operation
 * fails.
 *
 * [effect] will always be run on the composition's apply dispatcher and appliers are never run
 * concurrent with themselves, one another, applying changes to the composition tree, or running
 * [CompositionLifecycleObserver] event callbacks. [SideEffect]s are always run after
 * [CompositionLifecycleObserver] event callbacks.
 */
@Composable
@ComposableContract(restartable = false)
fun SideEffect(
    effect: () -> Unit
) {
    currentComposer.recordSideEffect(effect)
}
