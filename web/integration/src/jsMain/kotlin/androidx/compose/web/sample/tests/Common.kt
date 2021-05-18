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

package androidx.compose.web.sample.tests

import androidx.compose.runtime.Composable
import androidx.compose.web.elements.Span
import androidx.compose.web.elements.Text
import androidx.compose.web.renderComposableInBody
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class TestCase(val composable: @Composable () -> Unit) {
    operator fun provideDelegate(
        thisRef: Any,
        property: KProperty<*>
    ): ReadOnlyProperty<Any?, String> {
        if (property.name in testCases) {
            error("${property.name} already exists! Choose a unique name")
        }
        testCases[property.name] = this
        return ReadOnlyProperty { _, _ -> property.name }
    }
}

internal fun testCase(composable: @Composable () -> Unit): TestCase {
    return TestCase(composable)
}

internal val testCases = mutableMapOf<String, TestCase>()

fun launchTestCase(testCaseId: String) {
    // this makes test cases get initialised:
    listOf<Any>(TestCases1(), InputsTests())

    if (testCaseId !in testCases) error("Test Case '$testCaseId' not found")

    renderComposableInBody {
        testCases[testCaseId]!!.composable.invoke()
    }
}

const val TEST_TEXT_DEFAULT_ID = "txt"
@Composable
fun TestText(value: String, id: String = TEST_TEXT_DEFAULT_ID) {
    Span(
        attrs = { id(id) }
    ) {
        Text(value)
    }
}