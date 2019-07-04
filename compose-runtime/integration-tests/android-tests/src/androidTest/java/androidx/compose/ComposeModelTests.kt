/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

import android.app.Activity
import android.view.ViewGroup
import android.widget.TextView
import androidx.compose.frames.AbstractRecord
import androidx.compose.frames.Framed
import androidx.compose.frames.Record
import androidx.compose.frames._created
import androidx.compose.frames._readable
import androidx.compose.frames._writable
import androidx.compose.frames.currentFrame
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import androidx.test.rule.ActivityTestRule
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

val PRESIDENT_NAME_1 = "George Washington"
val PRESIDENT_AGE_1 = 57
val PRESIDENT_NAME_16 = "Abraham Lincoln"
val PRESIDENT_AGE_16 = 52

class Person(name: String, age: Int) : Framed {
    var name
        get() = (_readable(_first, this) as CustomerRecord).name
        set(value) {
            (_writable(_first, this) as CustomerRecord).name = value
        }

    var age
        get() = (_readable(_first, this) as CustomerRecord).age
        set(value) {
            (_writable(_first, this) as CustomerRecord).age = value
        }

    private var _first: Record = CustomerRecord().apply {
        this.name = name
        this.age = age
        this.frameId = currentFrame().id
        _created(this@Person)
    }

    override val firstFrameRecord: Record get() = _first

    override fun prependFrameRecord(value: Record) {
        value.next = _first
        _first = value
    }

    class CustomerRecord : AbstractRecord() {
        @JvmField
        var name: String = ""
        @JvmField
        var age: Int = 0

        override fun assign(value: Record) {
            (value as? CustomerRecord)?.let {
                this.name = it.name
                this.age = it.age
            }
        }

        override fun create() = CustomerRecord()
    }
}

class TestState<T>(value: T) : Framed {
    @Suppress("UNCHECKED_CAST")
    var value: T
        get() = (_readable(myFirst, this) as StateRecord<T>).value
        set(value) {
            (_writable(myFirst, this) as StateRecord<T>).value = value
        }

    private var myFirst: Record

    init {
        myFirst = StateRecord(value)
    }

    override val firstFrameRecord: Record
        get() = myFirst

    override fun prependFrameRecord(value: Record) {
        value.next = myFirst
        myFirst = value
    }

    private class StateRecord<T>(myValue: T) : AbstractRecord() {
        override fun assign(value: Record) {
            @Suppress("UNCHECKED_CAST")
            this.value = (value as StateRecord<T>).value
        }

        override fun create(): Record = StateRecord(value)

        var value: T = myValue
    }
}

@SmallTest
@RunWith(AndroidJUnit4::class)
class ModelViewTests {

    @get:Rule
    val activityRule = ActivityTestRule(TestActivity::class.java)

    @Test
    @UiThreadTest
    fun testModelView_Simple(): Unit = FrameManager.isolated {
        val tvId = 67
        compose {
            emit(62, { context ->
                TextView(context).apply {
                    text = "Hello world!"; id = tvId
                }
            }) { }
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }
    }

    @Test
    @UiThreadTest
    fun testModelView_Simple_Recompose(): Unit = FrameManager.isolated {
        val tvId = 71
        compose {
            emit(73, { context ->
                TextView(context).apply {
                    text = "Hello world!"; id = tvId
                }
            }) { }
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }
    }

    @Test
    @Ignore("TODO(b/138720405): Investigate synchronisation issues in tests")
    @UiThreadTest
    fun testModelView_PersonModel(): Unit = FrameManager.isolated {
        val tvIdName = 90
        val tvIdAge = 91
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
        compose {
            call(147, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    emit(93, { context -> TextView(context).apply { id = tvIdName } }) {
                        set(president.name) { text = it }
                    }
                    emit(94, { context -> TextView(context).apply { id = tvIdAge } }) {
                        set(president.age) { text = it.toString() }
                    }
                })
            }
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            president.name = PRESIDENT_NAME_16
            president.age = PRESIDENT_AGE_16
        }.then {
            val tvName = it.findViewById(tvIdName) as TextView
            val tvAge = it.findViewById(tvIdAge) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test
    @UiThreadTest
    fun testModelView_RecomposeScopeCleanup(): Unit = FrameManager.isolated {
        val washington = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
        val lincoln = Person(
            PRESIDENT_NAME_16,
            PRESIDENT_AGE_16
        )
        val displayLincoln = TestState(true)

        fun ViewComposition.display(person: Person) {
            call(167, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    emit(93, { context -> TextView(context) }) {
                        set(person.name) { text = it }
                    }
                    emit(94, { context -> TextView(context) }) {
                        set(person.age) { text = it.toString() }
                    }
                })
            }
        }

        compose {
            call(185, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    display(washington)
                    if (displayLincoln.value)
                        display(lincoln)
                })
            }
        }.then {
            displayLincoln.value = false
        }.then {
            assertFalse(displayLincoln.value)
        }.then {
            assertFalse(displayLincoln.value)
        }
    }

    // b/122548164
    @Test
    @Ignore("TODO(b/138720405): Investigate synchronisation issues in tests")
    fun testObserverEntering(): Unit = FrameManager.isolated {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
        val tvName = 204

        fun ViewComposition.display(person: Person) {
            call(167, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    emit(93, { context -> TextView(context).apply { id = tvName } }) {
                        set(person.name) { text = it }
                    }
                    emit(94, { context -> TextView(context) }) {
                        set(person.age) { text = it.toString() }
                    }
                })
                if (person.name == PRESIDENT_NAME_16) {
                    @Suppress("PLUGIN_ERROR")
                    (Observe {
                        emit(211, { context -> TextView(context) }) {
                            set(person.name) { text = it }
                        }
                        emit(211, { context -> TextView(context) }) {
                            set(person.age) { text = it.toString() }
                        }
                    })
                }
            }
        }

        compose {
            call(219, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    display(president)
                })
            }
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_1, (activity.findViewById(tvName) as TextView).text)
            president.name = PRESIDENT_NAME_16
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_16, (activity.findViewById(tvName) as TextView).text)
        }
    }

    @Test
    @Ignore("TODO(b/138720405): Investigate synchronisation issues in tests")
    fun testModelUpdatesNextFrameVisibility(): Unit = FrameManager.isolated {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
        val tvName = 204

        fun ViewComposition.display(person: Person) {
            call(167, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    emit(93, { context -> TextView(context).apply { id = tvName } }) {
                        set(person.name) { text = it }
                    }
                    emit(94, { context -> TextView(context) }) {
                        set(person.age) {
                            text = it.toString()
                        }
                    }
                })
                if (person.name == PRESIDENT_NAME_16) {
                    @Suppress("PLUGIN_ERROR")
                    (Observe {
                        emit(211, { context -> TextView(context) }) {
                            set(person.name) { text = it }
                        }
                        emit(211, { context -> TextView(context) }) {
                            set(person.age) { text = it.toString() }
                        }
                    })
                }
            }
        }

        compose {
            call(219, { true }) {
                @Suppress("PLUGIN_ERROR")
                (Observe {
                    display(president)
                })
            }
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_1, (activity.findViewById(tvName) as TextView).text)
            // schedule commit and recompose by this change, all for next frame
            president.name = PRESIDENT_NAME_16
            // check that changes aren't there yet
            assertEquals(PRESIDENT_NAME_1, (activity.findViewById(tvName) as TextView).text)
            Choreographer.postFrameCallback {
                // after one frame we should see changes
                assertEquals(PRESIDENT_NAME_16, (activity.findViewById(tvName) as TextView).text)
            }
        }
    }

    private class Root(val block: ViewComposition.() -> Unit) : Component() {
        override fun compose() {
            ViewComposition(composer.composer).block()
        }
    }

    fun compose(block: ViewComposition.() -> Unit) =
        CompositionModelTest(block, activityRule.activity)

    class CompositionModelTest(val composable: ViewComposition.() -> Unit, val activity: Activity) {
        var savedContext: CompositionContext? = null
        inner class ActiveTest(val activity: Activity) {
            private var firstCompose = true
            private fun compose() {
                if (firstCompose) {
                    val composer = composer.composer
                    composer.startRoot()
                    val instance = composer.remember {
                        Root(
                            composable
                        )
                    }
                    composer.startGroup(invocation)
                    instance()
                    composer.endGroup()
                    composer.endRoot()
                    firstCompose = false
                    composer.applyChanges()
                }
            }

            fun then(block: (activity: Activity) -> Unit): ActiveTest {
                compose()
                block(activity)
                return this
            }
        }

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            val cc = Compose.createCompositionContext(
                activity,
                activity.root,
                Root(composable),
                null
            )

            // Ensure the context is not collected until the test completes.
            savedContext = cc

            return cc.composer.runWithCurrent {
                ActiveTest(activity).then(block)
            }
        }
    }
}

private val Activity.root get() = findViewById(ComposerComposeTestCase.ROOT_ID) as ViewGroup
