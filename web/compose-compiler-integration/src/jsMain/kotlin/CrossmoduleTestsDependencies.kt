import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import kotlinx.browser.document
import org.jetbrains.compose.web.dom.Text

data class DataClassTakesValComposable(val c: @Composable () -> Unit)
data class DataClassTakesValComposableTyped<T>(val c: @Composable (T) -> Unit)
data class DataClassTakesVarComposable(var c: @Composable () -> Unit)

class ClassTakesValComposable(val c: @Composable () -> Unit)
class ClassTakesValComposableTyped<T>(val c: @Composable (T) -> Unit)
class ClassTakesVarComposable(var c: @Composable () -> Unit)

class ClassTakesComposablePrivateVal(private val c: @Composable () -> Unit) {

    @Composable
    fun callPrivateComposablePassedIntoConstructor() {
        c()
    }
}


interface HasComposable {
    val composable: @Composable () -> Unit
}

class ImplementsHasComposable(override val composable: @Composable () -> Unit): HasComposable

interface HasComposableTyped<T> {
    val composable: @Composable (T) -> Unit
}

class ImplementsHasComposableTyped<T>(override val composable: @Composable (T) -> Unit): HasComposableTyped<T>

class ClassSavesComposableIntoVar(c: @Composable () -> Unit) {
    var composableVar: @Composable () -> Unit = c
}

@Suppress("UNNECESSARY_LATEINIT")
class ClassSavesComposableIntoLateinitVar(c: @Composable () -> Unit) {
    lateinit var composableVar: @Composable () -> Unit

    init {
        composableVar = c
    }
}

class ClassSavesComposableIntoNullableVar(c: @Composable () -> Unit) {
    var composableVar: (@Composable () -> Unit)? = null

    init {
        composableVar = c
    }
}

class ClassSavesTypedComposableIntoVar<T>(c: @Composable (T) -> Unit) {
    var composableVar: @Composable (T) -> Unit = c
}


@Suppress("UNNECESSARY_LATEINIT")
class ClassSavesTypedComposableIntoLateinitVar<T>(c: @Composable (T) -> Unit) {
    lateinit var composableVar: @Composable (T) -> Unit

    init {
        composableVar = c
    }
}

class ClassWithSecondaryConstructorSavesComposable(val c: @Composable () -> Unit) {
    constructor(): this({
        SideEffect {
            document.body!!.innerHTML = "Secondary constructor composable content"
        }
    })
}

data class DataClassTakesValStringAndComposable(
    val s: String, val c: @Composable DataClassTakesValStringAndComposable.() -> Unit
)

class ClassTakesValStringAndComposable(
    val s: String, val c: @Composable ClassTakesValStringAndComposable.() -> Unit
)

class ClassSavesStringAndComposableIntoVar(
    s: String, c: @Composable ClassSavesStringAndComposableIntoVar.() -> Unit
) {
    var composableVar: @Composable ClassSavesStringAndComposableIntoVar.() -> Unit = c
    var stringVar: String = s
}

val GlobalComposableLambdaToShowText: @Composable (text: () -> String) -> Unit = {
    Text(it())
}
