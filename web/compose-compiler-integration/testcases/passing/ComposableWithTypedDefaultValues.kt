// @Module:Main
// fixed in https://github.com/JetBrains/androidx/pull/118

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.Composer

fun main() {
    var called = false

    callComposable {
        ComposableWithTypedDefaultValue<String>({ it ->
            check(it.value == null)
            called = true
        })
    }

    require(called) { "Failed when running composables" }
}

// @Module:Lib
import androidx.compose.runtime.Composable

sealed class NullableWrapper<T> {
    companion object {
        fun <T> of(value: T?): NullableWrapper<T> =
            if (value != null) {
                NonNullWrapper(value)
            } else {
                NullWrapper()
            }
    }

    abstract val value: T?
}

class NonNullWrapper<T>(override val value: T) : NullableWrapper<T>()

class NullWrapper<T> : NullableWrapper<T>() {
    override val value: T? get() = null
}

@Composable // https://github.com/JetBrains/compose-jb/issues/1226
fun <T> ComposableWithTypedDefaultValue(
    onChange: (NullableWrapper<T>) -> Unit,
    valueWrapper: NullableWrapper<T> = NullWrapper()
) {
    onChange(valueWrapper)
}

