package org.jetbrains.compose.web.sample.tests

import androidx.compose.runtime.Composable
import androidx.compose.web.sample.tests.ControlledInputsTests
import androidx.compose.web.sample.tests.RadioGroupTestCases
import androidx.compose.web.sample.tests.SelectElementTests
import androidx.compose.web.sample.tests.UncontrolledInputsTests
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.renderComposableInBody
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal class TestCase {
    lateinit var composable: @Composable () -> Unit

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
    return TestCase().apply {
        this.composable = composable
    }
}

internal val testCases = mutableMapOf<String, TestCase>()

fun launchTestCase(testCaseId: String) {
    // this makes test cases get initialised:
    listOf<Any>(
        TestCases1(), InputsTests(), EventsTests(),
        SelectElementTests(), ControlledInputsTests(), UncontrolledInputsTests(),
        RadioGroupTestCases()
    )

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
