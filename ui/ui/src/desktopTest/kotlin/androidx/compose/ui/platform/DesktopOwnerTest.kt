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

package androidx.compose.ui.platform

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.node.ExperimentalLayoutNodeApi
import androidx.compose.ui.node.LayoutNode
import org.jetbrains.skiko.Library
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalLayoutNodeApi::class, ExperimentalComposeApi::class)
class DesktopOwnerTest {
    @Test
    fun `single invalidate with multiple observers and single state change`() {
        Library.load("/", "skiko")

        var invalidateCount = 0

        val owners = DesktopOwners(
            invalidate = {
                invalidateCount++
            }
        )
        val owner = DesktopOwner(owners)
        val node = LayoutNode()
        val state = mutableStateOf(2)

        owner.observeLayoutModelReads(node) {
            state.value
        }

        owner.observeLayoutModelReads(node) {
            state.value
        }

        val oldInvalidateCount = invalidateCount

        Snapshot.notifyObjectsInitialized()
        state.value++
        Snapshot.sendApplyNotifications()

        assertEquals(1, invalidateCount - oldInvalidateCount)
    }
}