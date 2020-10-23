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
package androidx.compose.runtime

import android.widget.TextView
import androidx.compose.runtime.snapshots.StateObject
import androidx.compose.runtime.snapshots.StateRecord
import androidx.compose.runtime.snapshots.readable
import androidx.compose.runtime.snapshots.writable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val PRESIDENT_NAME_1 = "George Washington"
const val PRESIDENT_AGE_1 = 57
const val PRESIDENT_NAME_16 = "Abraham Lincoln"
const val PRESIDENT_AGE_16 = 52

@OptIn(ExperimentalComposeApi::class)
class Person(name: String, age: Int) : StateObject {
    var name
        get() = first.readable(this).name
        set(value) {
            first.writable(this) { name = value }
        }

    var age
        get() = first.readable(this).age
        set(value) {
            first.writable(this) { age = value }
        }

    private var first: CustomerRecord = CustomerRecord().apply {
        this.name = name
        this.age = age
    }

    override val firstStateRecord: StateRecord get() = first

    override fun prependStateRecord(value: StateRecord) {
        first = value as CustomerRecord
    }

    class CustomerRecord : StateRecord() {
        var name: String = ""
        var age: Int = 0

        override fun assign(value: StateRecord) {
            (value as? CustomerRecord)?.let {
                this.name = it.name
                this.age = it.age
            }
        }

        override fun create() = CustomerRecord()
    }
}

@Composable fun Observe(body: @Composable () -> Unit) = body()

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
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

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
        val washington = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
        val lincoln = Person(
            PRESIDENT_NAME_16,
            PRESIDENT_AGE_16
        )

        val displayLincoln = mutableStateOf(true)

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
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
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
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
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
