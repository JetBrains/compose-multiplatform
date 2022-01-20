import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

fun main() {
    MainScope().launch {
        addNComposableItems(1000)
        //removeAllItems(preAddedItemsCount = 1000)
        //updateEveryXth(3000, xth = 2, repeatUpdate = 20)
    }
}
