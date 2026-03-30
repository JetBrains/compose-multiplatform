// @Module:Main

// https://youtrack.jetbrains.com/issue/CMP-6374

import kotlin.reflect.KProperty
import androidx.compose.runtime.Composable

class Router {
    @Composable
    operator fun getValue(ref: Any?, property: KProperty<*>): Router {
        return Router()
    }
}

fun main() {
    callComposable {
        val router by Router()
    }
}
