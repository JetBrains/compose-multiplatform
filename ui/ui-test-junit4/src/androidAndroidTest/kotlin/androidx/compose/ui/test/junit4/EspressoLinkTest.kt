/*
 * Copyright 2021 The Android Open Source Project
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

import androidx.compose.ui.test.InternalTestApi
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Test

class EspressoLinkTest {
    @OptIn(InternalTestApi::class, ExperimentalCoroutinesApi::class)
    private val espressoLink = EspressoLink(
        IdlingResourceRegistry(TestScope(UnconfinedTestDispatcher()))
    )

    @After
    fun tearDown() {
        // Unregister EspressoLink with public API in case our implementation
        // that relies on deprecated API doesn't work anymore.
        IdlingRegistry.getInstance().unregister(espressoLink)
        // onIdle() should remove the espressoLink from all lists of idling resources
        Espresso.onIdle()
    }

    /**
     * Tests that EspressoLink registers and unregisters itself synchronously to both the public
     * registry (IdlingRegistry) and the private registry (IdlingResourceRegistry). When the
     * unregistration doesn't unregister itself synchronously anymore, we might have a memory
     * leak (see b/202190483).
     *
     * Also see b/205550018 for context.
     */
    @Test
    fun registerAndUnregister() {
        // Check the public registry:
        assertThat(IdlingRegistry.getInstance().resources).hasSize(0)
        // Check the private registry:
        assertThat(@Suppress("DEPRECATION") Espresso.getIdlingResources()).hasSize(0)

        espressoLink.withStrategy {
            assertThat(IdlingRegistry.getInstance().resources)
                .containsExactlyElementsIn(listOf(espressoLink))
            assertThat(@Suppress("DEPRECATION") Espresso.getIdlingResources())
                .containsExactlyElementsIn(listOf(espressoLink))
        }

        // Check if espressoLink is removed from both places:
        assertThat(IdlingRegistry.getInstance().resources).hasSize(0)
        assertThat(@Suppress("DEPRECATION") Espresso.getIdlingResources()).hasSize(0)
    }
}
