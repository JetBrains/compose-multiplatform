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
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.mock
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class TextStateTest {

    @OptIn(InternalFoundationTextApi::class)
    @Test
    fun layoutResult_writes_areReadObservableinDraw() {
        // this is really a test about references, so just using mocks
        val subject = TextState(
            mock(), // not needed for test
            17L,
        )
        var observed: Any? = null
        var written: Any? = null
        Snapshot.observe(readObserver = { any ->
            observed = any
        }, writeObserver = { any ->
            written = any
        }
        ) {
            subject.drawScopeInvalidation
            subject.layoutResult = mock()
        }
        assertThat(observed).isNotNull()
        assertThat(written).isNotNull()
    }
}