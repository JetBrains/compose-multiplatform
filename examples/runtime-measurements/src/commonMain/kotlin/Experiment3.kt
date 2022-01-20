import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.yield
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Composable
private fun RemoveAllItems(count: Int) {
    repeat(count) {
        key(it) {
            PlainText("A") {
                PlainText("A1") {
                    PlainText("A1_1") {
                        PlainText("a1") {
                            PlainText("text")
                        }
                    }
                }
                PlainText("A2")
            }
        }
    }
}
@OptIn(ExperimentalTime::class)
suspend fun removeAllItems(preAddedItemsCount: Int) {
    println("\nStart removeAllItems(preAddedItemsCount=$preAddedItemsCount)\n")

    val rootNode = Node().apply { text = "__ROOT__" }
    val applier = ListApplier(rootNode)

    val count = mutableStateOf(preAddedItemsCount)

    callComposable(applier) {
        RemoveAllItems(count.value)
    }

    println("Initial content size = ${rootNode.list.size}\n")

    val duration = measureTime {
        count.value = 0
        while (applier.changesAppliedCountState.value != 2) {
            yield()
        }
    }

    println("After change content size = ${rootNode.list.size}\n")

    println("Duration (preAddedItemsCount=$preAddedItemsCount) = ${duration.toLong(DurationUnit.MILLISECONDS)}\n")

}
