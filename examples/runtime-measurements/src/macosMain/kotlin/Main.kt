import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // try to run in debug and release modes to compare the time of execution
    addNComposableItems(1000)
    //removeAllItems(preAddedItemsCount = 1000)
    //updateEveryXth(3000, xth = 2, repeatUpdate = 20)
}
