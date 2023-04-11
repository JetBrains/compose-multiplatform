// @Module:Main

// https://github.com/JetBrains/compose-jb/issues/827

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
