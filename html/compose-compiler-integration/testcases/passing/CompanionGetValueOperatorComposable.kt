// @Module:Main

// https://youtrack.jetbrains.com/issue/CMP-6374

import kotlin.reflect.KProperty
import androidx.compose.runtime.Composable

interface Router {
    companion object {
        @Composable
        operator fun getValue(ref: Any?, property: KProperty<*>): Router {
            return object : Router {}
        }
    }
}

fun main() {
    callComposable {
        val router by Router
    }
}
