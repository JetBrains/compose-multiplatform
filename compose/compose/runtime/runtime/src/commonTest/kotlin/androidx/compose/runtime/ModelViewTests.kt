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

package androidx.compose.runtime

import androidx.compose.runtime.mock.MockViewValidator
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.expectNoChanges
import androidx.compose.runtime.mock.validate
import kotlin.test.Test

internal const val PRESIDENT_NAME_1 = "George Washington"
internal const val PRESIDENT_AGE_1 = 57
internal const val PRESIDENT_NAME_16 = "Abraham Lincoln"
internal const val PRESIDENT_AGE_16 = 52

internal class Person(name: String, age: Int) {
    var name by mutableStateOf(name)
    var age by mutableStateOf(age)
}

class ModelViewTests {

    @Test
    fun testModelView_Simple() = compositionTest {
        compose {
            Text("Hello world!")
        }

        validate {
            Text("Hello world!")
        }
    }

    @Test
    fun testModelView_Simple_Recompose() = compositionTest {
        compose {
            Text(value = "Hello world!")
        }

        validate {
            Text(value = "Hello world!")
        }

        expectNoChanges()

        validate {
            Text(value = "Hello world!")
        }
    }

    @Test
    fun testModelView_PersonModel() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        compose {
            Text(president.name)
            Text("${president.age}")
        }

        fun validate() {
            validate {
                Text(president.name)
                Text("${president.age}")
            }
        }
        validate()

        president.name = PRESIDENT_NAME_16
        president.age = PRESIDENT_AGE_16

        expectChanges()
        validate()
    }

    @Test
    fun testModelView_RecomposeScopeCleanup() = compositionTest {
        val washington = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )
        val lincoln = Person(
            PRESIDENT_NAME_16,
            PRESIDENT_AGE_16
        )

        val displayLincoln = mutableStateOf(true)

        @Composable fun Display(person: Person) {
            Text(person.name)
            Text(person.age.toString())
        }

        compose {
            Display(washington)
            if (displayLincoln.value) {
                Display(lincoln)
            }
        }

        fun MockViewValidator.Display(person: Person) {
            Text(person.name)
            Text(person.age.toString())
        }

        fun validate() {
            validate {
                this.Display(washington)
                if (displayLincoln.value) {
                    this.Display(lincoln)
                }
            }
        }
        validate()

        displayLincoln.value = false

        expectChanges()
        validate()
    }

    // b/122548164
    @Test
    fun testObserverEntering() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        @Composable fun Display(person: Person) {
            Text(person.name)
            Text("${person.age}")
            if (person.name == PRESIDENT_NAME_16) {
                Text(person.name)
                Text("${person.age}")
            }
        }

        fun MockViewValidator.Display(person: Person) {
            Text(person.name)
            Text("${person.age}")
            if (person.name == PRESIDENT_NAME_16) {
                Text(person.name)
                Text("${person.age}")
            }
        }

        compose {
            Display(president)
        }

        fun validate() {
            validate {
                this.Display(president)
            }
        }
        validate()

        president.name = PRESIDENT_NAME_16
        expectChanges()
        validate()
    }

    @Test
    fun testModelUpdatesNextFrameVisibility() = compositionTest {
        val president = Person(
            PRESIDENT_NAME_1,
            PRESIDENT_AGE_1
        )

        @Composable fun Display(person: Person) {
            Text(person.name)
            Text("${person.age}")
            if (person.name == PRESIDENT_NAME_16) {
                Text(person.name)
                Text("${person.age}")
            }
        }

        fun MockViewValidator.Display(person: Person, nameOverride: String?) {
            val name = nameOverride ?: person.name
            Text(name)
            Text("${person.age}")
            if (name == PRESIDENT_NAME_16) {
                Text(name)
                Text("${person.age}")
            }
        }

        compose {
            Display(president)
        }

        fun validate(nameOverride: String? = null) {
            validate {
                this.Display(president, nameOverride)
            }
        }
        validate()

        // schedule commit and recompose by this change, all for next frame
        president.name = PRESIDENT_NAME_16

        // check that changes aren't there yet
        validate(PRESIDENT_NAME_1)

        expectChanges()
        validate()
    }
}