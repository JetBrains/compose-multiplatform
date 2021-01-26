/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.runtime.snapshots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

/**
 * A mutable value holder where reads to the [value] property during the execution of a [Composable]
 * function, the current [RecomposeScope] will be subscribed to changes of that value. When the
 * [value] property is written to and changed, a recomposition of any subscribed [RecomposeScope]s
 * will be scheduled. Writes to it are transacted as part of the [Snapshot] system.
 *
 * @see [State]
 * @see [MutableState]
 * @see [mutableStateOf]
 */
interface SnapshotMutableState<T> : MutableState<T> {
    /**
     * A policy to control how changes are handled in a mutable snapshot.
     */
    val policy: SnapshotMutationPolicy<T>
}
