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

import androidx.compose.runtime.snapshots.Snapshot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AndroidSnapshotTests : BaseComposeTest() {
    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test // regression test for b/163903673
    fun testCommittingInABackgroundThread() {
        val states = Array(10000) { mutableStateOf(0) }
        var stop = false
        object : Thread() {
            override fun run() {
                while (!stop) {
                    for (state in states) {
                        state.value = state.value + 1
                    }
                    sleep(1)
                }
            }
        }.start()
        try {
            val unregister = Snapshot.registerApplyObserver { changed, _ ->
                // Try to catch a concurrent modification exception
                val iterator = changed.iterator()
                while (iterator.hasNext()) {
                    iterator.next()
                }
            }
            try {
                repeat(100) {
                    activityRule.activity.uiThread {
                        Snapshot.sendApplyNotifications()
                    }
                }
            } finally {
                unregister.dispose()
            }
        } finally {
            stop = true
        }
    }
}