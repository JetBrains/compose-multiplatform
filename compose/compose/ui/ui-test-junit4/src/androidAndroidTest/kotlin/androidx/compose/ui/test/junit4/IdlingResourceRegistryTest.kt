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

package androidx.compose.ui.test.junit4

import androidx.compose.ui.test.IdlingResource
import androidx.compose.ui.test.InternalTestApi
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

/*
 * This class *could* be moved to the test source set, but that makes it more likely to be
 * skipped if only connectedCheck is run.
 */
@SmallTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class IdlingResourceRegistryTest {

    private var onIdleCalled = false
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scope = TestScope(UnconfinedTestDispatcher())
    @OptIn(InternalTestApi::class)
    private val registry = IdlingResourceRegistry(scope).apply {
        setOnIdleCallback { onIdleCalled = true }
    }

    @After
    fun verifyRegistryStoppedPolling() {
        // to replace deprecated scope.cleanupTestCoroutines() we use runTest {}
        scope.runTest {}
    }

    @Test
    @UiThreadTest
    fun isIdleNow_0_IdlingResources() {
        assertThat(registry.isIdleNow).isTrue()
        assertNotPolling()
    }

    @Test
    @UiThreadTest
    fun isIdleNow_1_IdlingResource() {
        val resource = TestIdlingResource(true)
        registry.registerIdlingResource(resource)
        assertThat(registry.isIdleNow).isTrue()

        resource.isIdleNow = false
        assertThat(registry.isIdleNow).isFalse()

        assertThatPollingStartsAndEnds {
            resource.isIdleNow = true
        }

        resource.isIdleNow = false
        assertThat(registry.isIdleNow).isFalse()

        assertThatPollingStartsAndEnds {
            resource.isIdleNow = true
        }
    }

    @Test
    @UiThreadTest
    fun isIdleNow_2_IdlingResources() {
        val resource1 = TestIdlingResource(true)
        registry.registerIdlingResource(resource1)
        val resource2 = TestIdlingResource(true)
        registry.registerIdlingResource(resource2)

        resource1.isIdleNow = true
        resource2.isIdleNow = true
        assertThat(registry.isIdleNow).isTrue()

        resource1.isIdleNow = false
        resource2.isIdleNow = true
        assertThat(registry.isIdleNow).isFalse()

        resource1.isIdleNow = true
        resource2.isIdleNow = false
        assertThat(registry.isIdleNow).isFalse()

        resource1.isIdleNow = false
        resource2.isIdleNow = false
        assertThat(registry.isIdleNow).isFalse()

        assertThatPollingStartsAndEnds {
            resource1.isIdleNow = true
            resource2.isIdleNow = true
        }
    }

    @Test
    @UiThreadTest
    fun isIdleNow_true_doesNotStartPolling() {
        registry.registerIdlingResource(TestIdlingResource(true))
        assertThat(registry.isIdleNow).isTrue()
        assertNotPolling()
    }

    @Test
    @UiThreadTest
    fun isIdleNow_false_doesNotStartPolling() {
        registry.registerIdlingResource(TestIdlingResource(false))
        assertThat(registry.isIdleNow).isFalse()
        assertNotPolling()
    }

    @Test
    @UiThreadTest
    fun isIdleOrStartPolling_emptyRegister_doesNotStartPolling() {
        assertThat(registry.isIdleNow).isTrue()
        assertThat(registry.isIdleOrEnsurePolling()).isTrue()
        assertNotPolling()
    }

    @Test
    @UiThreadTest
    fun isIdleOrStartPolling_idleRegister_doesNotStartPolling() {
        registry.registerIdlingResource(TestIdlingResource(true))
        assertThat(registry.isIdleOrEnsurePolling()).isTrue()
        assertNotPolling()
    }

    @Test
    @UiThreadTest
    fun isIdleOrStartPolling_busyRegister_doesStartPolling() {
        val resource = TestIdlingResource(false)
        registry.registerIdlingResource(resource)

        assertThatPollingStartsAndEnds {
            resource.isIdleNow = true
        }
    }

    private fun assertThatPollingStartsAndEnds(makeIdle: () -> Unit) {
        // Check that we're not polling already ..
        val beforeTime = scope.currentTime
        scope.advanceUntilIdle()
        assertThat(scope.currentTime - beforeTime).isEqualTo(0L)

        // .. and that we're not idle
        assertThat(registry.isIdleNow).isFalse()

        // Start the polling
        onIdleCalled = false
        assertThat(registry.isIdleOrEnsurePolling()).isFalse()

        // Make the registry idle
        makeIdle.invoke()

        // Verify that it has polled ..
        val beforeTime2 = scope.currentTime
        scope.advanceUntilIdle()
        assertThat(scope.currentTime - beforeTime2).isGreaterThan(0L)
        // .. the registry is now idle
        assertThat(registry.isIdleNow).isTrue()
        // .. and the onIdle callback was called
        assertThat(onIdleCalled).isTrue()
    }

    private fun assertNotPolling() {
        // Check that no poll job is running ..
        val beforeTime = scope.currentTime
        scope.advanceUntilIdle()
        assertThat(scope.currentTime - beforeTime).isEqualTo(0L)
        // .. and that the onIdle callback was not called
        assertThat(onIdleCalled).isFalse()
    }

    private class TestIdlingResource(initialIdleness: Boolean) : IdlingResource {
        override var isIdleNow: Boolean = initialIdleness
    }
}
