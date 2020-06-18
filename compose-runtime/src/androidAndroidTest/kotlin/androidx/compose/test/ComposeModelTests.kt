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
@file:Suppress("PLUGIN_ERROR")
package androidx.compose.test

import android.widget.TextView
import androidx.compose.Composable
import androidx.compose.clearRoots
import androidx.compose.frames.AbstractRecord
import androidx.compose.frames.Framed
import androidx.compose.frames.Record
import androidx.compose.frames._created
import androidx.compose.frames._readable
import androidx.compose.frames._writable
import androidx.compose.frames.commit
import androidx.compose.frames.currentFrame
import androidx.compose.frames.inFrame
import androidx.compose.frames.open
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.After
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

@Composable fun Observe(body: @Composable () -> Unit) = body()

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

@MediumTest
@RunWith(AndroidJUnit4::class)
class ModelViewTests : BaseComposeTest() {

    @After
    fun teardown() {
        clearRoots()
    }

    @get:Rule
    override val activityRule = makeTestActivityRule()

    @Test
    fun testModelView_Simple() {
        val tvId = 67
        compose {
            TextView(text = "Hello world!", id = tvId)
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }
    }

    @Test
    fun testModelView_Simple_Recompose() {
        val tvId = 71
        compose {
            TextView(text = "Hello world!", id = tvId)
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }.then { activity ->
            val tv = activity.root.findViewById(tvId) as TextView
            assertEquals("Hello world!", tv.text)
        }
    }

    @Test
    fun testModelView_PersonModel() {
        val tvIdName = 90
        val tvIdAge = 91
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }

        compose {
            TextView(id = tvIdName, text = president.name)
            TextView(id = tvIdAge, text = "${president.age}")
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
    fun testModelView_RecomposeScopeCleanup() {
        val washington = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }
        val lincoln = frame {
            Person(
                PRESIDENT_NAME_16,
                PRESIDENT_AGE_16
            )
        }
        val displayLincoln = frame { TestState(true) }

        @Composable fun display(person: Person) {
            TextView(text = person.name)
            TextView(text = "${person.age}")
        }

        compose {
            Observe {
                display(washington)
                if (displayLincoln.value)
                    display(lincoln)
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
    fun testObserverEntering() {
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }
        val tvName = 204

        @Composable fun display(person: Person) {
            TextView(id = tvName, text = person.name)
            TextView(text = "${person.age}")
            if (person.name == PRESIDENT_NAME_16) {
                TextView(text = person.name)
                TextView(text = "${person.age}")
            }
        }

        compose {
            display(president)
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_1, (activity.findViewById(tvName) as TextView).text)
            president.name = PRESIDENT_NAME_16
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_16, (activity.findViewById(tvName) as TextView).text)
        }
    }

    @Test
    fun testModelUpdatesNextFrameVisibility() {
        val president = frame {
            Person(
                PRESIDENT_NAME_1,
                PRESIDENT_AGE_1
            )
        }
        val tvName = 204

        @Composable fun display(person: Person) {
            TextView(id = tvName, text = person.name)
            TextView(text = "${person.age}")
            if (person.name == PRESIDENT_NAME_16) {
                TextView(text = person.name)
                TextView(text = "${person.age}")
            }
        }

        compose {
            display(president)
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_1, (activity.findViewById(tvName) as TextView).text)
            // schedule commit and recompose by this change, all for next frame
            president.name = PRESIDENT_NAME_16
            // check that changes aren't there yet
            assertEquals(PRESIDENT_NAME_1, (activity.findViewById(tvName) as TextView).text)
        }.then { activity ->
            assertEquals(PRESIDENT_NAME_16, (activity.findViewById(tvName) as TextView).text)
        }
    }
}

fun <T> frame(block: () -> T): T {
    val wasInFrame = inFrame
    if (!wasInFrame) open()
    return block().also { if (!wasInFrame) commit() }
}