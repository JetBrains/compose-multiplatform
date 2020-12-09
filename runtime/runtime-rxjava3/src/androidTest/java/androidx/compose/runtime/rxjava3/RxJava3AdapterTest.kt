/*
 * Copyright 2020 The Android Open stream Project
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

package androidx.compose.runtime.rxjava3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.filters.MediumTest
import com.google.common.truth.Truth.assertThat
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.MaybeEmitter
import io.reactivex.rxjava3.core.MaybeOnSubscribe
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.core.SingleOnSubscribe
import io.reactivex.rxjava3.subjects.BehaviorSubject
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@MediumTest
@RunWith(Parameterized::class)
class RxJava3AdapterTest(private val factory: () -> Stream) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<() -> Stream> = arrayOf(
            ObservableStream(),
            FlowableStream(),
            SingleStream(),
            MaybeStream()
        )
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun whenValueIsNotSetWeGotNull() {
        val stream = factory()
        var realValue: String? = "to-be-updated"
        rule.setContent {
            realValue = stream.subscribeAsState(null).value
        }

        assertThat(realValue).isNull()
    }

    @Test
    fun weGotInitialValue() {
        val stream = factory()
        stream.onNext("value")
        var realValue: String? = null
        rule.setContent {
            realValue = stream.subscribeAsState(null).value
        }

        assertThat(realValue).isEqualTo("value")
    }

    @Test
    fun weReceiveValueSubmittedLater() {
        val stream = factory()

        var realValue: String? = null
        rule.setContent {
            realValue = stream.subscribeAsState(null).value
        }

        rule.runOnIdle {
            stream.onNext("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value")
        }
    }

    @Test
    fun weReceiveSecondValue() {
        val stream = factory()
        if (!stream.supportMultipleValues()) return

        stream.onNext("value")
        var realValue: String? = null
        rule.setContent {
            realValue = stream.subscribeAsState(null).value
        }

        rule.runOnIdle {
            stream.onNext("value2")
        }

        rule.runOnIdle {
            assertThat(realValue).isEqualTo("value2")
        }
    }

    @Test
    fun noUpdatesAfterDispose() {
        val stream = factory()
        var emit by mutableStateOf(true)
        var realValue: String? = "to-be-updated"
        rule.setContent {
            if (emit) {
                realValue = stream.subscribeAsState(null).value
            }
        }

        rule.runOnIdle { emit = false }

        rule.runOnIdle {
            stream.onNext("value")
        }

        rule.runOnIdle {
            assertThat(realValue).isNull()
        }
    }

    @Test
    fun testObservableWithInitialValue() {
        val stream = factory()
        var realValue: String? = "to-be-updated"
        rule.setContent {
            realValue = stream.subscribeAsState("value").value
        }

        assertThat(realValue).isEqualTo("value")
    }

    @Test
    fun weReceiveValueSubmittedOnBackgroundThread() {
        val stream = factory()

        var realValue: String? = null
        rule.setContent {
            realValue = stream.subscribeAsState(null).value
        }

        Thread(
            Runnable {
                stream.onNext("value")
            }
        ).start()

        rule.waitUntil(5_000) { realValue == "value" }
    }
}

interface Stream {
    fun onNext(value: String)

    @Composable
    fun subscribeAsState(initial: String?): State<String?>

    fun supportMultipleValues(): Boolean
}

private class ObservableStream : () -> Stream {

    override fun invoke(): Stream {
        return object : Stream {
            val subject = BehaviorSubject.create<String>()

            override fun onNext(value: String) = subject.onNext(value)

            @Composable
            override fun subscribeAsState(initial: String?) = subject.subscribeAsState(initial)

            override fun supportMultipleValues(): Boolean = true
        }
    }

    override fun toString() = "Observable"
}

private class FlowableStream : () -> Stream {
    override fun invoke(): Stream {
        return object : Stream {
            val subject = BehaviorSubject.create<String>()
            val flowable = subject.toFlowable(BackpressureStrategy.LATEST)

            override fun onNext(value: String) = subject.onNext(value)

            @Composable
            override fun subscribeAsState(initial: String?) = flowable.subscribeAsState(initial)

            override fun supportMultipleValues(): Boolean = true
        }
    }

    override fun toString() = "Flowable"
}

private class SingleStream : () -> Stream {
    override fun invoke(): Stream {
        return object : Stream, SingleOnSubscribe<String> {
            var value: String? = null
            val emmiters = mutableListOf<SingleEmitter<String>>()
            val single = Single.create(this)

            override fun onNext(value: String) {
                require(this.value == null)
                this.value = value
                emitAll()
            }

            private fun emitAll() {
                if (value != null) {
                    emmiters.forEach {
                        if (!it.isDisposed) {
                            it.onSuccess(value!!)
                        }
                    }
                }
            }

            override fun subscribe(p0: SingleEmitter<String>) {
                emmiters.add(p0)
                emitAll()
            }

            @Composable
            override fun subscribeAsState(initial: String?) = single.subscribeAsState(initial)

            override fun supportMultipleValues(): Boolean = false
        }
    }

    override fun toString() = "Single"
}

private class MaybeStream : () -> Stream {
    override fun invoke(): Stream {
        return object : Stream, MaybeOnSubscribe<String> {
            val emmiters = mutableListOf<MaybeEmitter<String>>()
            var value: String? = null
            val maybe = Maybe.create<String>(this)

            override fun onNext(value: String) {
                require(this.value == null)
                this.value = value
                emitAll()
            }

            private fun emitAll() {
                if (value != null) {
                    emmiters.forEach {
                        if (!it.isDisposed) {
                            it.onSuccess(value!!)
                        }
                    }
                }
            }

            override fun subscribe(p0: MaybeEmitter<String>) {
                emmiters.add(p0)
                emitAll()
            }

            @Composable
            override fun subscribeAsState(initial: String?) = maybe.subscribeAsState(initial)

            override fun supportMultipleValues(): Boolean = false
        }
    }

    override fun toString() = "Maybe"
}