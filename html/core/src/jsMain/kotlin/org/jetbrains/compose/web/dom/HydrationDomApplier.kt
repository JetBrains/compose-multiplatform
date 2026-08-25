package org.jetbrains.compose.web.dom

import androidx.compose.runtime.AbstractApplier
import kotlinx.dom.clear
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.Text

/**
 * Claims the existing DOM during initial composition, then switches to normal DOM mutations.
 * Works by maintaining stack of frames.
 */
internal class HydrationDomApplier(
    root: DomNodeWrapper,
) : AbstractApplier<DomNodeWrapper>(root) {
    private enum class State {
        Hydrating,
        Complete,
        Aborted,
    }

    // cursor for parent whose children are being visited
    private class Frame(val node: Node) {
        var nextNode: Node? = node.firstChild // used for traversal
        var nextChildIndex: Int = 0           // used for diagnostics
    }

    private val rootNode = root.node
    private val frames = mutableListOf(Frame(rootNode))
    private val claimedNodes = mutableSetOf<Node>()  // claimed nodes that still need to be called by insertBottomUp
    private var state = State.Hydrating

    val isHydrating: Boolean
        get() = state == State.Hydrating

    private val currentFrame: Frame
        get() = frames.last()

    fun claimElement(tagName: String): Element {
        ensureHydrating()

        val expectedTagName = tagName.lowercase()
        val frame = currentFrame
        val index = frame.nextChildIndex++
        val candidate = frame.nextNode
        frame.nextNode = candidate?.nextSibling
        val element = candidate as? Element
            ?: mismatchAtChild(
                expectedTagName,
                index,
                "expected <$expectedTagName>, found ${candidate.describe()}",
            )

        if (element.tagName.lowercase() != expectedTagName) {
            mismatchAtChild(
                expectedTagName,
                index,
                "expected <$expectedTagName>, found ${element.describe()}",
            )
        }

        claimedNodes += element
        return element
    }

    fun claimText(value: String): Text {
        ensureHydrating()

        val frame = currentFrame
        val index = frame.nextChildIndex++
        val candidate = frame.nextNode
        frame.nextNode = candidate?.nextSibling
        val text = candidate as? Text
            ?: mismatchAtChild(
                "text()",
                index,
                "expected text ${value.quoted()}, found ${candidate.describe()}",
            )

        if (text.data != value) {
            mismatchAtChild(
                "text()",
                index,
                "expected text ${value.quoted()}, found text ${text.data.quoted()}",
            )
        }

        claimedNodes += text
        return text
    }

    override fun down(node: DomNodeWrapper) {
        if (isHydrating) {
            if (node.node !in claimedNodes) {
                mismatchAtCurrentNode("entered a node that was not claimed")
            }
            frames += Frame(node.node)
        }
        super.down(node)
    }

    override fun up() {
        if (isHydrating) {
            val frame = currentFrame
            // The frame stack has to stay in sync with the applier stack.
            if (frame.node !== current.node) {
                mismatchAtCurrentNode("left an unexpected node")
            }
            verifyComplete(frame)
            frames.removeAt(frames.lastIndex)
        }
        super.up()
    }

    override fun insertTopDown(index: Int, instance: DomNodeWrapper) = Unit

    override fun insertBottomUp(index: Int, instance: DomNodeWrapper) {
        when (state) {
            State.Hydrating -> verifyClaimedChild(index, instance.node)
            State.Complete -> current.insert(index, instance)
            State.Aborted -> Unit
        }
    }

    override fun remove(index: Int, count: Int) {
        if (state == State.Complete) current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        if (state == State.Complete) current.move(from, to, count)
    }

    override fun onClear() {
        if (state == State.Complete) rootNode.clear()
    }

    fun finishHydration() {
        ensureHydrating()
        if (frames.size != 1) {
            mismatchAtCurrentNode("composition ended before leaving the current node")
        }
        verifyComplete(currentFrame)
        if (claimedNodes.isNotEmpty()) {
            mismatchAtCurrentNode("composition ended before all claimed nodes were reconciled")
        }

        state = State.Complete
        frames.clear()
    }

    fun abortHydration() {
        if (!isHydrating) return
        state = State.Aborted
        claimedNodes.clear()
        frames.clear()
    }

    private fun verifyClaimedChild(index: Int, child: Node) {
        if (!claimedNodes.remove(child)) {
            mismatchAtChild(
                child.pathName(),
                index,
                "attempted to reconcile a node that was not claimed",
            )
        }
    }

    private fun verifyComplete(frame: Frame) {
        val extra = frame.nextNode ?: return
        mismatchAtCurrentNode("expected end of node, found extra ${extra.describe()}")
    }

    private fun ensureHydrating() {
        check(isHydrating) { "Hydration is no longer active" }
    }

    private fun mismatchAtChild(name: String, index: Int, detail: String): Nothing {
        throw HydrationMismatchException(
            "Hydration mismatch at ${currentPath()}/$name[$index]: $detail",
        )
    }

    private fun mismatchAtCurrentNode(detail: String): Nothing {
        throw HydrationMismatchException("Hydration mismatch at ${currentPath()}: $detail")
    }

    private fun currentPath(): String = buildString {
        append("root")
        for (frameIndex in 1 until frames.size) {
            append('/')
            append(frames[frameIndex].node.pathName())
            append('[')
            // The parent cursor advanced exactly once when this frame's node was claimed.
            append(frames[frameIndex - 1].nextChildIndex - 1)
            append(']')
        }
    }
}

private fun Node.pathName(): String = when (this) {
    is Element -> tagName.lowercase()
    is Text -> "text()"
    else -> nodeName
}

private fun Node?.describe(): String = when (this) {
    null -> "the end of the children"
    is Element -> "<${tagName.lowercase()}>"
    is Text -> "text ${data.quoted()}"
    else -> nodeName
}

private fun String.quoted(): String = buildString {
    append('"')
    this@quoted.forEach { character ->
        when (character) {
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            '"' -> append("\\\"")
            else -> append(character)
        }
    }
    append('"')
}
