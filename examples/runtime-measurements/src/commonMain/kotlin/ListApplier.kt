import androidx.compose.runtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class Node(val list: MutableList<Node> = mutableListOf()) {

    constructor(text: String, list: MutableList<Node> = mutableListOf()): this(list) {
        this.text = text
    }
    var text: String = ""
}

class ListApplier(node: Node) : AbstractApplier<Node>(node) {

    private var _changesAppliedCountState = MutableStateFlow(0)

    val changesAppliedCountState: StateFlow<Int> = _changesAppliedCountState

    private var endApplyCounter = 0

    override fun onEndChanges() {
        //println("onEndChanges")
        super.onEndChanges()
        _changesAppliedCountState.tryEmit(++endApplyCounter)
    }

    override fun onBeginChanges() {
        //println("onBeginChanges")
        super.onBeginChanges()
    }

    override fun insertTopDown(index: Int, instance: Node) {
        // ignored. Building tree bottom-up
    }

    override fun insertBottomUp(index: Int, instance: Node) {
        current.list.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        repeat(count) {
            current.list.removeAt(index)
        }
    }
    override fun move(from: Int, to: Int, count: Int) {
        if (from == to) {
            return // nothing to do
        }

        for (i in 0 until count) {
            // if "from" is after "to," the from index moves because we're inserting before it
            val fromIndex = if (from > to) from + i else from
            val toIndex = if (from > to) to + i else to + count - 2

            val child = current.list.removeAt(fromIndex)
            current.list.add(toIndex, child)
        }
    }

    override fun onClear() {
        root.list.clear()
    }
}


fun nodeToStr(node: Node, padding: Int = 0): String {
    return node.text + " " + node.list.joinToString(prefix = "", postfix = "", separator = "") {
        "\n${" ".repeat(padding)}|--- " + nodeToStr(it, padding + 5)
    }
}


fun createRecomposer(): Recomposer {
    GlobalSnapshotManager.ensureStarted()

    val mainScope = CoroutineScope(Dispatchers.Default + MClock)

    return Recomposer(mainScope.coroutineContext).also {
        mainScope.launch {
            it.runRecomposeAndApplyChanges()
        }
    }
}

fun callComposable(applier: ListApplier, content: @Composable () -> Unit) {
    val c = ControlledComposition(
        applier = applier,
        parent = createRecomposer()
    )
    c.setContent(content)
}
