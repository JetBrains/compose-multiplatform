// @Module:Main

// https://github.com/JetBrains/compose-jb/issues/1436

import kotlin.reflect.KProperty
import androidx.compose.runtime.*


fun main() {
    var (foo, setFoo) =  mutableStateOf(0)
    setFoo(123) // set
    require(foo == 0) { "If this failed, it probably means the issue was fixed" }
    require(foo == 123) { "Expected Failure (wrong behaviour)! foo was expected to get updated, but it's acutal value is $foo" }
}
