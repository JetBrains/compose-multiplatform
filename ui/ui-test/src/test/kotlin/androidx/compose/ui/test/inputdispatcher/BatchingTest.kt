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

package androidx.compose.ui.test.inputdispatcher

import androidx.compose.testutils.expectError
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.RobolectricMinSdk
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(minSdk = RobolectricMinSdk)
class BatchingTest : InputDispatcherTest() {

    companion object {
        private const val cannotEnqueueError = "Can't enqueue touch event \\(.*\\), " +
            "AndroidInputDispatcher has already been disposed"
        private const val cannotFlushError =
            "Can't flush events, AndroidInputDispatcher has already been disposed"
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. flush sends
     * 3. dispose doesn't send more
     */
    @Test
    fun enqueueFlushDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. flush sends
        subject.flush()
        assertThat(recorder.events).hasSize(3)

        // 3. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. flush sends
     * 3. flush again doesn't send more
     * 4. dispose doesn't send more
     */
    @Test
    fun enqueueFlushFlushDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. flush sends
        subject.flush()
        assertThat(recorder.events).hasSize(3)

        // 3. flush again doesn't send more
        subject.flush()
        assertThat(recorder.events).hasSize(3)

        // 4. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. flush sends
     * 3. enqueue can be called again
     * 4. dispose doesn't send more
     */
    @Test
    fun enqueueFlushEnqueueDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. flush sends
        subject.flush()
        assertThat(recorder.events).hasSize(3)

        // 3. enqueue can be called again
        subject.enqueueTouchMove()
        assertThat(recorder.events).hasSize(3)

        // 4. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. flush sends
     * 3. enqueue can be called again
     * 4. flush again does send more
     * 5. dispose doesn't send more
     */
    @Test
    fun enqueueFlushEnqueueFlushDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. flush sends
        subject.flush()
        assertThat(recorder.events).hasSize(3)

        // 3. enqueue can be called again
        subject.enqueueTouchMove()
        assertThat(recorder.events).hasSize(3)

        // 4. flush again does send more
        subject.flush()
        assertThat(recorder.events).hasSize(4)

        // 5. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).hasSize(4)
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. dispose doesn't send more
     * 3. subsequent enqueue fails
     * 4. subsequent dispose succeeds and doesn't send more
     */
    @Test
    fun enqueueDisposeEnqueueDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).isEmpty()

        // 3. subsequent enqueue fails
        expectError<IllegalStateException>(expectedMessage = cannotEnqueueError) {
            subject.enqueueTouchMove()
        }
        assertThat(recorder.events).isEmpty()

        // 4. subsequent dispose succeeds and doesn't send more
        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. dispose doesn't send more
     * 3. subsequent flush fails
     * 4. subsequent dispose succeeds and doesn't send more
     */
    @Test
    fun enqueueDisposeFlushDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).isEmpty()

        // 3. subsequent flush fails
        expectError<IllegalStateException>(expectedMessage = cannotFlushError) {
            subject.flush()
        }
        assertThat(recorder.events).isEmpty()

        // 4. subsequent dispose succeeds and doesn't send more
        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that:
     * 1. enqueue doesn't send
     * 2. dispose doesn't send more
     * 3. subsequent dispose succeeds and doesn't send more
     */
    @Test
    fun enqueueDisposeDispose() {
        // 1. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 2. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).isEmpty()

        // 3. subsequent dispose succeeds and doesn't send more
        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that:
     * 1. flush doesn't send
     * 2. enqueue doesn't send
     * 3. subsequent flush sends
     * 4. dispose doesn't send more
     */
    @Test
    fun flushEnqueueFlushDispose() {
        // 1. flush doesn't send
        subject.flush()
        assertThat(recorder.events).isEmpty()

        // 2. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 3. subsequent flush sends
        subject.flush()
        assertThat(recorder.events).hasSize(3)

        // 4. dispose doesn't send more
        subject.dispose()
        assertThat(recorder.events).hasSize(3)
    }

    /**
     * Tests that:
     * 1. flush doesn't send
     * 2. enqueue doesn't send
     * 3. dispose doesn't send
     */
    @Test
    fun flushEnqueueDispose() {
        // 1. flush doesn't send
        subject.flush()
        assertThat(recorder.events).isEmpty()

        // 2. enqueue doesn't send
        subject.enqueueTouchDown(0, Offset.Zero)
        subject.enqueueTouchMove()
        subject.enqueueTouchMove()
        assertThat(recorder.events).isEmpty()

        // 3. dispose doesn't send
        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that:
     * 1. flush doesn't send
     * 2. subsequent flush succeeds and doesn't send
     * 3. dispose doesn't send
     */
    @Test
    fun flushFlushDispose() {
        // 1. flush doesn't send
        subject.flush()
        assertThat(recorder.events).isEmpty()

        // 2. subsequent flush succeeds and doesn't send
        subject.flush()
        assertThat(recorder.events).isEmpty()

        // 3. dispose doesn't send
        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }

    /**
     * Tests that:
     * 1. flush doesn't send
     * 2. dispose doesn't send
     */
    @Test
    fun flushDispose() {
        // 1. flush doesn't send
        subject.flush()
        assertThat(recorder.events).isEmpty()

        // 2. dispose doesn't send
        subject.dispose()
        assertThat(recorder.events).isEmpty()
    }
}
