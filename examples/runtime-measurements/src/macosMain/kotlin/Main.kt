import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // try to run in debug and release modes to compare the time of execution
    addNComposableItems(1000)
}
