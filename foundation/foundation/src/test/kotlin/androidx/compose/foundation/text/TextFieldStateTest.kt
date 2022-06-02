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

package androidx.compose.foundation.text

import androidx.compose.runtime.snapshots.Snapshot
import com.google.common.truth.Truth
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class TextFieldStateTest {

    @OptIn(InternalFoundationTextApi::class)
    @Test
    fun layoutResult_isSnapshotState() {
        // this is really a test about references, so just using mocks
        val subject = TextFieldState(
            mock(), // not needed for test
            mock(), // not needed for test
        )
        val result1 = TextLayoutResultProxy(mock())
        val snapshot = Snapshot.takeMutableSnapshot()
        snapshot.enter {
            subject.layoutResult = result1
            Truth.assertThat(subject.layoutResult).isSameInstanceAs(result1)
        }
        Truth.assertThat(subject.layoutResult).isSameInstanceAs(null)
    }
}