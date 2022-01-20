import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Composable
private fun AddItems(count: Int) {
    repeat(count) {
        PlainText("A") {
            PlainText("A1") {
                PlainText("A1_1") {
                    PlainText("a1") {
                        PlainText("a1_1")
                    }
                }
            }
            PlainText("A2")
        }
    }
}

@OptIn(ExperimentalTime::class)
suspend fun addNComposableItems(count: Int) {
    println("\nStart addNComposableItems($count)\n")
    val rootNode = Node().apply { text = "__ROOT__" }
    val applier = ListApplier(rootNode)

    val repeatCount = mutableStateOf(0)

    callComposable(applier) {
        AddItems(repeatCount.value)
    }

    println("Initial content size = ${rootNode.list.size}\n")

    val duration = measureTime {
        repeatCount.value = count
        while (applier.changesAppliedCountState.value != 2) {
            yield()
        }
    }

    println("After change content size = ${rootNode.list.size}\n")

    println("Duration (add count=$count) = ${duration.toLong(DurationUnit.MILLISECONDS)}\n")
}

//
//fun oneNode(): Node {
//    val nA = Node("A")
//
//    val nA1 = Node("A1")
//    val nA1_1 = Node("A1_1")
//    val na1 = Node("a1")
//    val na1_1 = Node("a1_1")
//
//    na1.list.add(na1_1)
//    nA1_1.list.add(na1)
//    nA1.list.add(nA1_1)
//
//    val nA2 = Node("A2")
//
//    nA.list.add(nA1)
//    nA.list.add(nA2)
//
//    return nA
//}
//
//fun addNNodes(n: Int = 1000): Node {
//    val rootNode = Node().apply { text = "__ROOT__" }
//
//
//    repeat(n) {
//        rootNode.list.add(it, oneNode())
//    }
//
//    return rootNode
//}
//
//@OptIn(ExperimentalTime::class)
//fun main_1() {
//    val duration = measureTime {
//        val ints = Array(5) { 0 }
//        repeat(1000_000) {
//            arrayOf(10, 11, 12, 13, 14).copyInto(ints)
//        }
//    }
//
//    println("Duration = ${duration.toLong(DurationUnit.MILLISECONDS)}")
//}
