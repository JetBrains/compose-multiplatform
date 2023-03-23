package example.imageviewer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.painter.Painter


typealias NodeComponentX<T> = @Composable BonsaiScopeX<T>.(NodeX<T>) -> Unit

sealed interface NodeX<T> {
    val iconComponent: NodeComponentX<T>
}

typealias NodeIconX<T> = @Composable (NodeX<T>) -> Painter?

data class BonsaiScopeX<T> internal constructor(
    internal val style: BonsaiStyleX<T>,
)

data class BonsaiStyleX<T>(
    val nodeCollapsedIcon: NodeIconX<T> = { null },
)

val abc: @Composable () -> Unit = {
    Text("Abc")
}

fun abcFoo(l: @Composable () -> Unit) {

}

fun abcFoo2(): @Composable () -> Unit {
    return abc
}

val LocalUnit: ProvidableCompositionLocal<Unit> = staticCompositionLocalOf { }

val text: String @Composable get() = "Some text"

@Composable
inline fun <reified C> rememberRouter(
    stack: List<C>,
    handleBackButton: Boolean = true
): C {
    error("a")
}