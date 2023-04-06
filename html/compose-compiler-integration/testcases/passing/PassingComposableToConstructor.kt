// @Module:Main

// https://youtrack.jetbrains.com/issue/KT-46880
import androidx.compose.runtime.Composable

fun main() {
    var set = mutableSetOf<Int>()

    val instance = testCase {
        set.add(1)
    }
    val instance2 = TestCase2()

    callComposable {
        instance.composable()
        instance2.composable()
        set.add(2)
    }
    require(setOf(1, 2) == set) { "Failed when running composables" }
}

// @Module:Lib
import androidx.compose.runtime.Composable
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class TestCase(val composable: @Composable () -> Unit) {
    operator fun provideDelegate(
        thisRef: Any,
        property: KProperty<*>
    ): ReadOnlyProperty<Any?, String> {
        return ReadOnlyProperty { _, _ -> property.name }
    }
}

class TestCase2(val composable: @Composable () -> Unit = {}) {
    operator fun provideDelegate(
        thisRef: Any,
        property: KProperty<*>
    ): ReadOnlyProperty<Any?, String> {
        return ReadOnlyProperty { _, _ -> property.name }
    }
}

fun testCase(composable: @Composable () -> Unit): TestCase {
    return TestCase(composable)
}
