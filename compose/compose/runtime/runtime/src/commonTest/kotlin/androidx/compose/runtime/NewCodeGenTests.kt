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

import androidx.compose.runtime.mock.Linear
import androidx.compose.runtime.mock.Text
import androidx.compose.runtime.mock.compositionTest
import androidx.compose.runtime.mock.expectChanges
import androidx.compose.runtime.mock.validate
import kotlin.test.Test
import kotlin.test.assertEquals

class NewCodeGenTests {
    @Test
    fun testStaticComposition() = compositionTest {
        compose {
            Text("Hello world!")
            Linear {
                Text("Yellow world")
            }
        }

        validate {
            Text("Hello world!")
            Linear {
                Text("Yellow world")
            }
        }
    }

    @Test
    fun testUpdatedComposition() = compositionTest {
        var text1 by mutableStateOf("Hello world!")
        var text2 by mutableStateOf("Yellow world")

        compose {
            Text(text1)
            Linear {
                Text(text2)
            }
        }

        fun validate() {
            validate {
                Text(text1)
                Linear {
                    Text(text2)
                }
            }
        }
        validate()

        text1 = "$text1 (changed)"
        text2 = "$text2 (changed)"

        expectChanges()
        validate()
    }

    @Test
    fun testSingleView() = compositionTest {
        var text by mutableStateOf("Hello world")

        compose {
            Text(text)
        }

        fun validate() {
            validate {
                Text(text)
            }
        }
        validate()

        text = "Salutations!"
        expectChanges()

        validate()
    }

    @Test
    fun testViewGroup() = compositionTest {
        var text by mutableStateOf("Hello world")

        compose {
            Linear {
                Text(text)
            }
        }

        fun validate() {
            validate {
                Linear {
                    Text(text)
                }
            }
        }
        validate()

        text = "Salutations!"
        expectChanges()

        validate()
    }

    @Composable
    fun PhoneView(phone: Phone, phoneCalled: StableCounter) {
        phoneCalled.count++
        Text(
            "${if (phone.area.isBlank()) "" else "(${phone.area}) "}${
            phone.prefix}-${phone.number}"
        )
    }

    @Test
    fun testComposableFunctionInvocationOneParameter() = compositionTest {
        var phone by mutableStateOf(Phone("123", "456", "7890"))
        val phoneCalled = StableCounter()
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            PhoneView(phone, phoneCalled)
        }

        assertEquals(1, phoneCalled.count)
        scope?.invalidate()
        advance()
        assertEquals(1, phoneCalled.count)

        phone = Phone("124", "456", "7890")
        advance()
        assertEquals(2, phoneCalled.count)
    }

    @Composable
    fun AddView(left: Int, right: Int, addCalled: StableCounter) {
        addCalled.count++
        Text("$left + $right = ${left + right}")
    }

    @Test
    fun testComposableFunctionInvocationTwoParameters() = compositionTest {
        var left by mutableStateOf(0)
        var right by mutableStateOf(1)
        val addCalled = StableCounter()
        var scope: RecomposeScope? = null
        compose {
            scope = currentRecomposeScope
            AddView(left, right, addCalled)
        }

        fun validate() {
            validate {
                Text("$left + $right = ${left + right}")
            }
        }
        validate()
        assertEquals(1, addCalled.count)

        scope?.invalidate()
        advance()
        validate()
        assertEquals(1, addCalled.count)

        left = 1
        advance()
        validate()
        assertEquals(2, addCalled.count)

        scope?.invalidate()
        advance()
        validate()
        assertEquals(2, addCalled.count)

        right = 41
        advance()
        validate()
        assertEquals(3, addCalled.count)
    }

    @Test
    fun testMoveComponents() = compositionTest {
        var data by mutableStateOf(listOf(1, 2, 3, 4, 5))
        compose {
            Linear {
                for (item in data) {
                    key(item) {
                        Text("$item View")
                    }
                }
            }
        }

        fun validate() {
            validate {
                Linear {
                    for (item in data) {
                        Text("$item View")
                    }
                }
            }
        }
        validate()
        data = data.toMutableList().also { it.add(it.removeAt(0)) }
        advance()
        validate()
    }
}

@Stable
class StableCounter(var count: Int = 0)

data class Phone(val area: String, val prefix: String, val number: String)