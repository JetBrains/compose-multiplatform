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

import androidx.compose.testutils.expectError
import androidx.compose.ui.platform.AndroidOwner
import androidx.compose.ui.test.junit4.android.AndroidOwnerRegistry
import androidx.compose.ui.test.onNodeWithTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@MediumTest
@RunWith(AndroidJUnit4::class)
class SynchronizationMethodsTest {

    val rule = createComposeRule()

    @Test
    fun runOnUiThread() {
        val result = rule.runOnUiThread { "Hello" }
        assertThat(result).isEqualTo("Hello")
    }

    @Test
    fun runOnUiThread_void() {
        var called = false
        rule.runOnUiThread { called = true }
        assertThat(called).isTrue()
    }

    @Test
    fun runOnUiThread_nullable() {
        val result: String? = rule.runOnUiThread { null }
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun runOnIdle() {
        withAndroidOwnerRegistry {
            val result = rule.runOnIdle { "Hello" }
            assertThat(result).isEqualTo("Hello")
        }
    }

    @Test
    fun runOnIdle_void() {
        withAndroidOwnerRegistry {
            var called = false
            rule.runOnIdle { called = true }
            assertThat(called).isTrue()
        }
    }

    @Test
    fun runOnIdle_nullable() {
        withAndroidOwnerRegistry {
            val result: String? = rule.runOnIdle { null }
            assertThat(result).isEqualTo(null)
        }
    }

    @Test
    fun runOnIdle_assert_fails() {
        withAndroidOwnerRegistry {
            rule.runOnIdle {
                expectError<IllegalStateException> {
                    rule.onNodeWithTag("dummy").assertExists()
                }
            }
        }
    }

    @Test
    fun runOnIdle_waitForIdle_fails() {
        withAndroidOwnerRegistry {
            rule.runOnIdle {
                expectError<IllegalStateException> {
                    rule.waitForIdle()
                }
            }
        }
    }

    @Test
    fun runOnIdle_runOnIdle_fails() {
        withAndroidOwnerRegistry {
            rule.runOnIdle {
                expectError<IllegalStateException> {
                    rule.runOnIdle {}
                }
            }
        }
    }

    private fun withAndroidOwnerRegistry(block: () -> Unit) {
        try {
            AndroidOwnerRegistry.setupRegistry()
            AndroidOwnerRegistry.registerOwner(mockResumedAndroidOwner())
            block()
        } finally {
            AndroidOwnerRegistry.tearDownRegistry()
        }
    }

    private fun mockResumedAndroidOwner(): AndroidOwner {
        val lifecycle = mock<Lifecycle>()
        doReturn(Lifecycle.State.RESUMED).whenever(lifecycle).currentState

        val lifecycleOwner = mock<LifecycleOwner>()
        doReturn(lifecycle).whenever(lifecycleOwner).lifecycle

        val viewTreeOwners = AndroidOwner.ViewTreeOwners(
            lifecycleOwner = lifecycleOwner,
            viewModelStoreOwner = mock(),
            savedStateRegistryOwner = mock()
        )
        val owner = mock<AndroidOwner>()
        doReturn(viewTreeOwners).whenever(owner).viewTreeOwners

        return owner
    }
}