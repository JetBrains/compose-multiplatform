import androidx.compose.runtime.Composable

class ImplementsHasComposable(
    override val composable: @Composable () -> Unit
): HasComposable

class ImplementsHasComposableTyped<T>(
    override val composable: @Composable (T) -> Unit
): HasComposableTyped<T>
