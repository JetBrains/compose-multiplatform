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
import kotlin.test.Test
import kotlin.test.assertSame
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

@OptIn(ExperimentalComposeApi::class)
class SnapshotContextElementTests {
    @Test
    fun coroutineEntersExpectedSnapshot() = runBlocking {
        val snapshot = Snapshot.takeSnapshot()
        try {
            withContext(snapshot.asContextElement()) {
                assertSame(snapshot, Snapshot.current, "expected snapshot")
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun snapshotRestoredAfterResume() {
        val snapshotOne = Snapshot.takeSnapshot()
        val snapshotTwo = Snapshot.takeSnapshot()
        try {
            runBlocking {
                val stopA = Job()
                val jobA = launch(snapshotOne.asContextElement()) {
                    assertSame(snapshotOne, Snapshot.current, "expected snapshotOne, A")
                    stopA.join()
                    assertSame(snapshotOne, Snapshot.current, "expected snapshotOne, B")
                }
                launch(snapshotTwo.asContextElement()) {
                    assertSame(snapshotTwo, Snapshot.current, "expected snapshotTwo, A")
                    stopA.complete()
                    jobA.join()
                    assertSame(snapshotTwo, Snapshot.current, "expected snapshotTwo, B")
                }
            }
        } finally {
            snapshotOne.dispose()
            snapshotTwo.dispose()
        }
    }
}