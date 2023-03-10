import androidx.compose.runtime.Composable

fun interface FunInterfaceWithComposable {
    @Composable
    fun content()
}


fun interface FunInterfaceReturnComposable {
    fun getContent(): @Composable () -> Unit
}
