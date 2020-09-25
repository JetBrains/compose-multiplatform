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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.ui.test.createComposeRule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

@LargeTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@FlowPreview
class FlowAdapterTest {

    private class FlowChannel<T> {
        val channel = BroadcastChannel<T>(1)
        val flow = channel.asFlow()
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun weReceiveSubmittedValue() {
        val stream = FlowChannel<String>()

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream.channel.offer("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Test
    fun weReceiveSecondValue() {
        val stream = FlowChannel<String>()

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream.channel.offer("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
            stream.channel.offer("value2")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value2")
        }
    }

    @Test
    fun noUpdatesAfterDispose() {
        val stream = FlowChannel<String>()
        var emit by mutableStateOf(true)
        var realValue: String? = "to-be-updated"
        rule.setContent {
            if (emit) {
                realValue = stream.flow.collectAsState(initial = null).value
            }
        }

        rule.runOnIdle { emit = false }

        rule.runOnIdle {
            stream.channel.offer("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isNull()
        }
    }

    @Test
    fun testCollectionWithInitialValue() {
        val stream = FlowChannel<String>()
        var realValue = "to-be-updated"
        rule.setContent {
            realValue = stream.flow.collectAsState("value").value
        }

        assertThat(realValue).isEqualTo("value")
    }

    @Test
    fun testOverridingInitialValue() {
        val stream = FlowChannel<String>()
        var realValue = "to-be-updated"
        rule.setContent {
            realValue = stream.flow.collectAsState("value").value
        }

        rule.runOnIdle {
            stream.channel.offer("value2")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value2")
        }
    }

    @Test
    fun theCurrentValueIsNotLostWhenWeUpdatedInitial() {
        val stream = FlowChannel<String>()
        var initial by mutableStateOf("initial1")

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(initial).value
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
        val stream1 = FlowChannel<String>()
        val stream2 = FlowChannel<String>()
        var stream by mutableStateOf(stream1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream = stream2
        }

        rule.runOnIdle {
            stream2.channel.offer("stream2")
        }

        rule.runOnIdle {
            stream1.channel.offer("stream1")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("stream2")
        }
    }

    @Test
    fun theCurrentValueIsNotLostWhenWeReplacedStreams() {
        val stream1 = FlowChannel<String>()
        val stream2 = FlowChannel<String>()
        var stream by mutableStateOf(stream1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(initial = null).value
        }

        rule.runOnIdle {
            stream1.channel.offer("value")
        }

        rule.runOnIdle {
            stream = stream2
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Test
    fun observingOnCustomContext() {
        val stream = FlowChannel<String>()
        val latch = CountDownLatch(1)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(null, Dispatchers.Default).value
            if (realValue != null) {
                latch.countDown()
            }
        }

        rule.runOnIdle {
            stream.channel.offer("value")
        }

        assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue()

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Test
    fun theCurrentValueIsNotLostWhenWeReplacedContext() {
        val stream = FlowChannel<String>()
        var context by mutableStateOf<CoroutineContext>(Dispatchers.Main)

        var realValue: String? = null
        rule.setContent {
            realValue = stream.flow.collectAsState(null, context).value
        }

        rule.runOnIdle {
            stream.channel.offer("value")
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
}
