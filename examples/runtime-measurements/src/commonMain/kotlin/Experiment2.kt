import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.yield
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Composable
private fun UpdateItems(count: Int, prefixForEveryXth: String, updateEveryXth: Int = 10) {
    repeat(count) {
        key(it) {
            PlainText("A") {
                PlainText("A1") {
                    PlainText("A1_1") {
                        PlainText("a1") {
                            val text = if (it % updateEveryXth == 0) {
                                "$prefixForEveryXth = $it"
                            } else {
                                "Number = $it"
                            }
                            PlainText(text)
                        }
                    }
                }
                PlainText("A2")
            }
        }
    }
}
@OptIn(ExperimentalTime::class)
suspend fun updateEveryXth(preAddedItemsCount: Int, xth: Int = 10, repeatUpdate: Int = 1) {
    println("\nStart updateEvery10th(preAddedItemsCount=$preAddedItemsCount)\n")

    val rootNode = Node().apply { text = "__ROOT__" }
    val applier = ListApplier(rootNode)

    val prefix = mutableStateOf("I")

    callComposable(applier) {
        UpdateItems(preAddedItemsCount, prefix.value, xth)
    }

    println("Initial content size = ${rootNode.list.size}\n")

    val duration = measureTime {
        repeat(repeatUpdate) { iter ->
            prefix.value = "K$iter"
            while (applier.changesAppliedCountState.value != 2 + iter) {
                yield()
            }
        }
    }

    println("After change content size = ${rootNode.list.size}\n")

    println("Duration (preAddedItemsCount=$preAddedItemsCount) = ${duration.toLong(DurationUnit.MILLISECONDS)}\n")

}
