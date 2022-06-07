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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@MediumTest
@RunWith(AndroidJUnit4::class)
class RememberSaveableTest {

    @get:Rule
    val rule = createComposeRule()

    private val restorationTester = StateRestorationTester(rule)

    @Test
    fun simpleRestore() {
        var array: IntArray? = null
        restorationTester.setContent {
            array = rememberSaveable {
                intArrayOf(0)
            }
        }

        assertThat(array).isEqualTo(intArrayOf(0))

        rule.runOnUiThread {
            array!![0] = 1
            // we null it to ensure recomposition happened
            array = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(array).isEqualTo(intArrayOf(1))
    }

    @Test
    fun restoreWithSaver() {
        var holder: Holder? = null
        restorationTester.setContent {
            holder = rememberSaveable(saver = HolderSaver) {
                Holder(0)
            }
        }

        assertThat(holder).isEqualTo(Holder(0))

        rule.runOnUiThread {
            holder!!.value = 1
            // we null it to ensure recomposition happened
            holder = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(holder).isEqualTo(Holder(1))
    }

    @Test
    fun canBeSavedFromRegistryIsUsed() {
        var canBeSavedCalledWith: Any? = null

        restorationTester.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun canBeSaved(value: Any): Boolean {
                            canBeSavedCalledWith = value
                            return super.canBeSaved(value)
                        }
                    }
                }
            ) {
                val v = rememberSaveable { 1 }
                assertEquals(1, v)
            }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(canBeSavedCalledWith).isEqualTo(1)
    }

    @Test
    fun providerProvidesCorrectlySavedValue() {
        var provider: () -> Any? = { error("will be overridden") }

        restorationTester.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun registerProvider(
                            key: String,
                            valueProvider: () -> Any?
                        ): SaveableStateRegistry.Entry {
                            provider = valueProvider
                            return super.registerProvider(key, valueProvider)
                        }
                    }
                }
            ) {
                val v = rememberSaveable { 2 }
                assertEquals(2, v)
            }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(provider.invoke()).isEqualTo(2)
    }

    @Test
    fun providerProvidesCorrectlySavedValueWithSaver() {
        var provider: () -> Any? = { error("will be overridden") }

        restorationTester.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun registerProvider(
                            key: String,
                            valueProvider: () -> Any?
                        ): SaveableStateRegistry.Entry {
                            provider = valueProvider
                            return super.registerProvider(key, valueProvider)
                        }
                    }
                }
            ) {
                rememberSaveable(saver = HolderSaver) { Holder(4) }
            }
        }

        restorationTester.emulateSavedInstanceStateRestore()

        assertThat(provider.invoke()).isEqualTo(4)
    }

    @Test
    fun unregistersFromPrevProviderAndRegistersToTheNewOne() {
        var unregisterCalledForKey: String? = null
        var registryFactory by mutableStateOf<(SaveableStateRegistry) -> SaveableStateRegistry>(
            value = {
                object : DelegateRegistry(it) {
                    override fun registerProvider(
                        key: String,
                        valueProvider: () -> Any?
                    ): SaveableStateRegistry.Entry {
                        val entry = super.registerProvider(key, valueProvider)
                        return object : SaveableStateRegistry.Entry {
                            override fun unregister() {
                                unregisterCalledForKey = key
                                entry.unregister()
                            }
                        }
                    }
                }
            }
        )

        rule.setContent {
            WrapRegistry(
                wrap = {
                    registryFactory(it)
                }
            ) {
                val v = rememberSaveable { 1 }
                assertEquals(1, v)
            }
        }

        var registerCalled = false

        rule.runOnUiThread {
            registryFactory = {
                object : DelegateRegistry(it) {
                    override fun registerProvider(
                        key: String,
                        valueProvider: () -> Any?
                    ): SaveableStateRegistry.Entry {
                        val result = super.registerProvider(key, valueProvider)
                        // asserts that we unregistered from the previous registry and then
                        // registered with the same key
                        assertThat(key).isEqualTo(unregisterCalledForKey)
                        registerCalled = true
                        return result
                    }
                }
            }
        }

        rule.mainClock.advanceTimeUntil { registerCalled }
    }

    @Test
    fun reregisterWhenTheKeyIsChanged() {
        var key by mutableStateOf("key1")
        val registeredKeys = mutableSetOf<String>()
        var registerCalled = 0

        rule.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun registerProvider(
                            key: String,
                            valueProvider: () -> Any?
                        ): SaveableStateRegistry.Entry {
                            val entry = super.registerProvider(key, valueProvider)
                            registeredKeys.add(key)
                            registerCalled++
                            return object : SaveableStateRegistry.Entry {
                                override fun unregister() {
                                    registeredKeys.remove(key)
                                    entry.unregister()
                                }
                            }
                        }
                    }
                }
            ) {
                val v = rememberSaveable(key = key) { 1 }
                assertEquals(1, v)
            }
        }

        rule.mainClock.advanceTimeUntil { registerCalled == 1 }
        rule.runOnUiThread {
            key = "key2"
        }

        rule.mainClock.advanceTimeUntil { registerCalled == 2 }
        assertThat(registeredKeys).isEqualTo(mutableSetOf("key2"))
    }

    @Test
    fun theLatestPassedSaverIsUsed() {
        var saver by mutableStateOf(
            Saver<Int, Int>(
                save = { 1 },
                restore = { 1 }
            )
        )

        restorationTester.setContent {
            rememberSaveable(saver = saver) { 1 }
        }

        val latch = CountDownLatch(1)

        rule.runOnIdle {
            saver = Saver(
                save = {
                    latch.countDown()
                    1
                },
                restore = { 1 }
            )
        }

        restorationTester.emulateSavedInstanceStateRestore()
        assertTrue(latch.await(1, TimeUnit.SECONDS))
    }

    @Test
    fun unregistersWhenDisposed() {
        var doEmit by mutableStateOf(true)
        var onUnregisterCalled = false

        rule.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun registerProvider(
                            key: String,
                            valueProvider: () -> Any?
                        ): SaveableStateRegistry.Entry {
                            val entry = super.registerProvider(key, valueProvider)
                            return object : SaveableStateRegistry.Entry {
                                override fun unregister() {
                                    onUnregisterCalled = true
                                    entry.unregister()
                                }
                            }
                        }
                    }
                }
            ) {
                if (doEmit) {
                    rememberSaveable { 1 }
                }
            }
        }

        rule.runOnUiThread {
            // assert that unregister is not yet called
            assertThat(onUnregisterCalled).isFalse()
            doEmit = false
        }

        rule.mainClock.advanceTimeUntil { onUnregisterCalled }
    }

    @Test
    fun customKey() {
        val passedKey = "test"
        var actualKey: String? = null
        rule.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun registerProvider(
                            key: String,
                            valueProvider: () -> Any?
                        ): SaveableStateRegistry.Entry {
                            actualKey = key
                            return super.registerProvider(key, valueProvider)
                        }
                    }
                }
            ) {
                val v = rememberSaveable(key = passedKey) { 2 }
                assertEquals(2, v)
            }
        }

        assertThat(actualKey).isEqualTo(passedKey)
    }

    @Test
    fun emptyKeyIsNotUsed() {
        var actualKey: String? = null
        rule.setContent {
            WrapRegistry(
                wrap = {
                    object : DelegateRegistry(it) {
                        override fun registerProvider(
                            key: String,
                            valueProvider: () -> Any?
                        ): SaveableStateRegistry.Entry {
                            actualKey = key
                            return super.registerProvider(key, valueProvider)
                        }
                    }
                }
            ) {
                val v = rememberSaveable(key = "") { 2 }
                assertEquals(2, v)
            }
        }

        assertThat(actualKey).isNotEmpty()
    }

    @Test
    fun restoreCorrectValueAfterInputChanges() {
        var counter = 0
        var composedValue: Int? = null
        var input by mutableStateOf(0)
        restorationTester.setContent {
            composedValue = rememberSaveable(input) {
                counter++
            }
        }

        rule.runOnIdle {
            assertThat(composedValue).isEqualTo(0)
            input = 1 // this will reset the state
        }

        rule.runOnIdle {
            assertThat(composedValue).isEqualTo(1)
            composedValue = null // clear to make sure the restoration worked
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(composedValue).isEqualTo(1)
        }
    }

    @Test
    fun changingInputIsNotAffectingOrderOfRestoration() {
        var counter = 0
        var input by mutableStateOf(0)
        var withInput: Int? = null
        var withoutInput: String? = null

        restorationTester.setContent {
            withInput = rememberSaveable(input) { counter++ }
            withoutInput = rememberSaveable { (counter++).toString() }
        }

        rule.runOnIdle {
            assertThat(withInput).isNotNull()
            withInput = null
            input++
        }

        var expectedWithInput: Int? = null
        var expectedWithoutInput: String? = null

        rule.runOnIdle {
            assertThat(withInput).isNotNull()
            assertThat(withoutInput).isNotNull()
            expectedWithInput = withInput
            expectedWithoutInput = withoutInput
            withInput = null
            withoutInput = null
        }

        restorationTester.emulateSavedInstanceStateRestore()

        rule.runOnIdle {
            assertThat(withInput).isEqualTo(expectedWithInput)
            assertThat(withoutInput).isEqualTo(expectedWithoutInput)
        }
    }
}

@Composable
private fun WrapRegistry(
    wrap: @Composable (SaveableStateRegistry) -> SaveableStateRegistry,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalSaveableStateRegistry provides wrap(LocalSaveableStateRegistry.current!!),
        content = content
    )
}

private open class DelegateRegistry(original: SaveableStateRegistry) :
    SaveableStateRegistry by original