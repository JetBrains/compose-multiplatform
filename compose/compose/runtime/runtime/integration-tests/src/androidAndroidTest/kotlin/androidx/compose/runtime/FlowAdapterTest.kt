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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertNotNull

@LargeTest
@RunWith(AndroidJUnit4::class)
class FlowAdapterTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun weReceiveSubmittedValue() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream.tryEmit("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Test
    fun weReceiveSecondValue() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream.tryEmit("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
            stream.tryEmit("value2")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value2")
        }
    }

    @Test
    fun noUpdatesAfterDispose() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var emit by mutableStateOf(true)
        var realValue: String? = "to-be-updated"
        rule.setContent {
            if (emit) {
                realValue = stream.collectAsState(initial = null).value
            }
        }

        rule.runOnIdle { emit = false }

        rule.runOnIdle {
            stream.tryEmit("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isNull()
        }
    }

    @Test
    fun testCollectionWithInitialValue() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var realValue = "to-be-updated"
        rule.setContent {
            realValue = stream.collectAsState("value").value
        }

        assertThat(realValue).isEqualTo("value")
    }

    @Test
    fun testOverridingInitialValue() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var realValue = "to-be-updated"
        rule.setContent {
            realValue = stream.collectAsState("value").value
        }

        rule.runOnIdle {
            stream.tryEmit("value2")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value2")
        }
    }

    @Test
    fun theCurrentValueIsNotLostWhenWeUpdatedInitial() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var initial by mutableStateOf("initial1")

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(initial).value
        }

        rule.runOnIdle {
            initial = "initial2"
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("initial1")
        }
    }

    @Test
    fun replacingStreams() {
        val stream1 = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val stream2 = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var stream by mutableStateOf(stream1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream = stream2
        }

        rule.runOnIdle {
            stream2.tryEmit("stream2")
        }

        rule.runOnIdle {
            stream1.tryEmit("stream1")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("stream2")
        }
    }

    @Test
    fun theCurrentValueIsNotLostWhenWeReplacedStreams() {
        val stream1 = MutableSharedFlow<String>(extraBufferCapacity = 1)
        val stream2 = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var stream by mutableStateOf(stream1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream1.tryEmit("value")
        }

        rule.runOnIdle {
            stream = stream2
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Ignore("b/177256608")
    @Test
    fun observingOnCustomContext() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(null, Dispatchers.Default).value
        }

        rule.runOnUiThread {
            stream.tryEmit("value")
        }

        rule.waitUntil { realValue != null }
        assertThat(realValue).isEqualTo("value")
    }

    @Test
    fun theCurrentValueIsNotLostWhenWeReplacedContext() {
        val stream = MutableSharedFlow<String>(extraBufferCapacity = 1)
        var context by mutableStateOf<CoroutineContext>(Dispatchers.Main)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.collectAsState(null, context).value
        }

        rule.runOnIdle {
            stream.tryEmit("value")
        }

        rule.runOnIdle {
            context = Dispatchers.IO
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Test
    fun testInitialValueOfStateFlow() {
        val flow = MutableStateFlow("initial")
        var realValue = "to-be-updated"
        rule.setContent {
            realValue = flow.collectAsState().value
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("initial")
        }
    }

    @Test
    fun stateFlowHandlesNullValue() {
        val flow = MutableStateFlow<String?>(null)
        var realValue: String? = "to-be-updated"
        rule.setContent {
            realValue = flow.collectAsState().value
        }

        rule.runOnIdle {
            assertThat(realValue).isNull()
        }
    }

    @Test
    fun updatingValueOfStateFlow() {
        val flow = MutableStateFlow("initial")
        var realValue = "to-be-updated"
        rule.setContent {
            realValue = flow.collectAsState().value
        }

        rule.runOnIdle {
            flow.value = "updated"
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("updated")
        }
    }

    @Test // Regression test for 232007227
    fun updatingTheInstanceOfAStateFlow() {
        class Car(val model: String)
        class Person(val name: String, val car: MutableStateFlow<Car>)

        val people = mutableListOf<MutableStateFlow<Person?>>(
            MutableStateFlow(Person("Ford", MutableStateFlow(Car("Model T")))),
            MutableStateFlow(Person("Musk", MutableStateFlow(Car("Model 3"))))
        )

        var carValue: Any? = null

        rule.setContent {
            for (index in (0 until people.size)) {
                val person = people[index].collectAsState().value
                if (person != null) {
                    val car = person.car.collectAsState().value
                    carValue = car
                } else {
                    carValue = null
                }
            }
        }

        rule.runOnIdle {
            people[0].value = null
        }

        rule.runOnIdle {
            assertNotNull(carValue)
        }
    }
}
