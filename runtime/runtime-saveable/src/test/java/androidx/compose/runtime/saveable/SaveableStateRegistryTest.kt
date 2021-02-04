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

package androidx.compose.runtime.saveable

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class SaveableStateRegistryTest {

    @Test
    fun saveSimpleValue() {
        val registry = createRegistry()

        registry.registerProvider("key") { 10 }

        registry.performSave().apply {
            assertThat(get("key")).isEqualTo(listOf(10))
        }
    }

    @Test
    fun unregisteredValuesAreNotSaved() {
        val registry = createRegistry()

        val provider = { 10 }
        val entry = registry.registerProvider("key", provider)
        entry.unregister()

        registry.performSave().apply {
            assertThat(containsKey("key")).isFalse()
        }
    }

    @Test
    fun registerAgainAfterUnregister() {
        val registry = createRegistry()

        val provider1 = { "value1" }
        val entry = registry.registerProvider("key", provider1)
        entry.unregister()
        registry.registerProvider("key") { "value2" }

        registry.performSave().apply {
            assertThat(get("key")).isEqualTo(listOf("value2"))
        }
    }

    @Test
    fun registerMultipleValues() {
        val registry = createRegistry()

        registry.registerProvider("key1") { 100L }
        val provider2 = { 100 }
        val entry = registry.registerProvider("key2", provider2)
        registry.registerProvider("key3") { "value" }
        registry.registerProvider("key4") { listOf("item") }
        entry.unregister()

        registry.performSave().apply {
            assertThat(get("key1")).isEqualTo(listOf(100L))
            assertThat(containsKey("key2")).isFalse()
            assertThat(get("key3")).isEqualTo(listOf("value"))
            assertThat(get("key4")).isEqualTo(listOf(listOf("item")))
        }
    }

    @Test
    fun nullValuesAreNotSaved() {
        val registry = createRegistry()

        registry.registerProvider("key") { null }

        registry.performSave().apply {
            assertThat(containsKey("key")).isFalse()
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyKeysAreNotAllowed() {
        val registry = createRegistry()

        registry.registerProvider("") { null }
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankKeysAreNotAllowed() {
        val registry = createRegistry()

        registry.registerProvider("     ") { null }
    }

    @Test
    fun restoreSimpleValues() {
        val restored = mapOf("key1" to listOf("value"), "key2" to listOf(2f))
        val registry = createRegistry(restored)

        assertThat(registry.consumeRestored("key1")).isEqualTo("value")
        assertThat(registry.consumeRestored("key2")).isEqualTo(2f)
    }

    @Test
    fun restoreClearsTheStoredValue() {
        val restored = mapOf("key" to listOf("value"))
        val registry = createRegistry(restored)

        assertThat(registry.consumeRestored("key")).isEqualTo("value")
        assertThat(registry.consumeRestored("key")).isNull()
    }

    @Test
    fun unusedRestoredValueSavedAgain() {
        val restored = mapOf("key1" to listOf("value"))
        val registry = createRegistry(restored)

        registry.registerProvider("key2") { 1 }

        registry.performSave().apply {
            assertThat(get("key1")).isEqualTo(listOf("value"))
            assertThat(get("key2")).isEqualTo(listOf(1))
        }
    }

    @Test
    fun canBeSavedIsCallingOurCallback() {
        var canBeSavedCalled = false
        val registry = createRegistry {
            canBeSavedCalled = true
            it is String
        }

        assertThat(registry.canBeSaved(1)).isFalse()
        assertThat(canBeSavedCalled).isTrue()

        canBeSavedCalled = false
        assertThat(registry.canBeSaved("test")).isTrue()
        assertThat(canBeSavedCalled).isTrue()
    }

    @Test(expected = IllegalStateException::class)
    fun valueWhichCantBeSavedIsNotAllowed() {
        val registry = createRegistry { false }

        registry.registerProvider("key") { 1 }

        registry.performSave()
    }

    @Test
    fun registeringTheSameKeysTwice() {
        val registry = createRegistry()

        registry.registerProvider("key") { 100L }
        registry.registerProvider("key") { 200L }

        val restoredRegistry = createRegistry(registry.performSave())
        assertThat(restoredRegistry.consumeRestored("key")).isEqualTo(100L)
        assertThat(restoredRegistry.consumeRestored("key")).isEqualTo(200L)
        assertThat(restoredRegistry.consumeRestored("key")).isNull()
    }

    @Test
    fun registeringAndUnregisteringTheSameKeys() {
        val registry = createRegistry()

        registry.registerProvider("key") { 1L }
        val entry = registry.registerProvider("key") { 2L }
        registry.registerProvider("key") { 3L }
        entry.unregister()

        val restoredRegistry = createRegistry(registry.performSave())
        assertThat(restoredRegistry.consumeRestored("key")).isEqualTo(1L)
        assertThat(restoredRegistry.consumeRestored("key")).isEqualTo(3L)
    }

    private fun createRegistry(
        restored: Map<String, List<Any?>>? = null,
        canBeSaved: (Any) -> Boolean = { true }
    ) = SaveableStateRegistry(restored, canBeSaved)
}
