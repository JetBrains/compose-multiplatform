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

package androidx.compose.ui.platform

import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.input.pointer.EmptyPointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
class WindowInfoTest {

    @After
    fun reset() {
        WindowInfoImpl.GlobalKeyboardModifiers.value = EmptyPointerKeyboardModifiers()
    }

    @Test
    fun `keyboardModifiers is the same across different instances`() {
        val wi1 = WindowInfoImpl()
        val wi2 = WindowInfoImpl()

        assertThat(wi1).isNotSameInstanceAs(wi2)

        val initial = EmptyPointerKeyboardModifiers()
        assertThat(wi1.keyboardModifiers).isEqualTo(initial)
        assertThat(wi2.keyboardModifiers).isEqualTo(initial)

        val update10 = PointerKeyboardModifiers(10)
        val update20 = PointerKeyboardModifiers(20)
        assertThat(update10).isNotEqualTo(update20)

        wi1.keyboardModifiers = update10
        assertThat(wi1.keyboardModifiers).isEqualTo(update10)
        assertThat(wi2.keyboardModifiers).isEqualTo(update10)

        wi2.keyboardModifiers = update20
        assertThat(wi1.keyboardModifiers).isEqualTo(update20)
        assertThat(wi2.keyboardModifiers).isEqualTo(update20)
    }

    @Test
    fun `keyboardModifiers is observable via snapshotFlow`() = runTest {
        val wi = WindowInfoImpl()

        var last: PointerKeyboardModifiers? = null
        launch(Job()) {
            snapshotFlow { wi.keyboardModifiers }.collect { last = it }
        }
        testScheduler.runCurrent()
        assertThat(last).isEqualTo(EmptyPointerKeyboardModifiers())

        WindowInfoImpl.GlobalKeyboardModifiers.value = PointerKeyboardModifiers(11)
        Snapshot.sendApplyNotifications()
        testScheduler.runCurrent()
        assertThat(last).isEqualTo(PointerKeyboardModifiers(11))

        WindowInfoImpl.GlobalKeyboardModifiers.value = PointerKeyboardModifiers(22)
        Snapshot.sendApplyNotifications()
        testScheduler.runCurrent()
        assertThat(last).isEqualTo(PointerKeyboardModifiers(22))
    }
}