/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.runtime.ExperimentalComposeApi
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

/**
 * Return a [SnapshotContextElement] that will [enter][Snapshot.enter] this [Snapshot] whenever
 * the associated coroutine is resumed and leave this snapshot when it suspends.
 * The snapshot still must be [disposed][Snapshot.dispose] separately when it will no longer
 * be used.
 *
 * @sample androidx.compose.runtime.samples.snapshotAsContextElementSample
 */
@ExperimentalComposeApi
fun Snapshot.asContextElement(): SnapshotContextElement = SnapshotContextElementImpl(this)

/**
 * A [CoroutineContext] element that [enters][Snapshot.enter] an associated snapshot
 * whenever a coroutine associated with this context is resumed.
 */
@ExperimentalComposeApi
interface SnapshotContextElement : CoroutineContext.Element {
    companion object Key : CoroutineContext.Key<SnapshotContextElement>
}

/**
 * Implementation of [SnapshotContextElement] that enters a single given snapshot when updating
 * the thread context of a resumed coroutine.
 */
@ExperimentalComposeApi
private class SnapshotContextElementImpl(
    private val snapshot: Snapshot
) : SnapshotContextElement, ThreadContextElement<Snapshot?> {
    override val key: CoroutineContext.Key<*>
        get() = SnapshotContextElement

    override fun updateThreadContext(context: CoroutineContext): Snapshot? =
        snapshot.unsafeEnter()

    override fun restoreThreadContext(context: CoroutineContext, oldState: Snapshot?) {
        snapshot.unsafeLeave(oldState)
    }
}
