package org.jetbrains.compose.web.dom

import androidx.compose.runtime.AbstractApplier
import kotlinx.browser.document
import kotlinx.dom.clear
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.w3c.dom.Comment
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
    private class Frame(
        val node: Node,
        // set by AttrsScope.allowHydrationMismatch() on the element owning these children
        val allowsContentMismatch: Boolean = false,
    ) {
        var nextNode: Node? = node.firstChild // used for traversal
        var nextChildIndex: Int = 0           // used for diagnostics
    }

    private data class PendingText(
        val parent: Node,
        val anchor: Node?,
        val text: Text,
    )

    private val rootNode = root.node
    private val frames = mutableListOf(Frame(rootNode))
    private val claimedNodes = mutableSetOf<Node>()  // claimed nodes that still need to be called by insertBottomUp
    private val nodesWithClaimedRawChildren = mutableSetOf<Node>()
    // Boundary markers and formatting-only root text must remain in place until hydration succeeds.
    private val nodesToRemoveAfterHydration = mutableListOf<Node>()
    private val pendingTextsToInsert = mutableListOf<PendingText>()
    private val pendingDomMutations = mutableListOf<() -> Unit>()
    private var state = State.Hydrating

    val isHydrating: Boolean
        get() = state == State.Hydrating

    private val currentFrame: Frame
        get() = frames.last()

    /** Holds back properties and listeners, which have no complete server HTML representation. */
    fun applyOrDeferDomMutation(mutation: () -> Unit) {
        when (state) {
            State.Hydrating -> pendingDomMutations += mutation
            State.Complete -> mutation()
            State.Aborted -> Unit
        }
    }

    fun mismatch(detail: String): Nothing = mismatchAtCurrentNode(detail)

    /** Claims the next element. A missing or differently tagged node is a mismatch. */
    fun claimElement(tagName: String): Element {
        ensureHydrating()

        val expectedTagName = tagName.lowercase()
        val frame = currentFrame
        // allows whitespace around root node, that won't cause a hydration mismatch
        if (frame.node === rootNode && frame.nextChildIndex == 0) {
            skipRootBoundaryWhitespace(frame)
        }
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

    /** Claims an element whose server-only text is not represented by a Compose DOM node. */
    fun claimElementWithRawText(
        tagName: String,
        value: String,
        allowContentMismatch: Boolean,
    ): Element {
        val element = claimElement(tagName)
        frames += Frame(element, allowsContentMismatch = allowContentMismatch)
        try {
            claimRawText(value, allowContentMismatch)
            verifyComplete(currentFrame)
        } finally {
            frames.removeAt(frames.lastIndex)
        }
        nodesWithClaimedRawChildren += element
        return element
    }

    /** Claims the next logical text node. Boundary markers prevent browser merging. */
    fun claimText(value: String): Text {
        ensureHydrating()

        val frame = currentFrame
        if (frame.node === rootNode && frame.nextChildIndex == 0) {
            skipRootBoundaryWhitespace(frame, expectedText = value)
        }
        val allowed = frame.allowsContentMismatch
        val index = frame.nextChildIndex++
        val candidate = frame.nextNode
        val text = when {
            candidate is Text && (allowed || value.isNotEmpty() || candidate.data.isEmpty()) -> {
                frame.nextNode = candidate.nextSibling
                val boundaryMarker = frame.nextNode.asHydrationTextBoundaryMarker()
                if (boundaryMarker != null) {
                    // Skip boundary markers.
                    frame.nextNode = boundaryMarker.nextSibling
                    nodesToRemoveAfterHydration += boundaryMarker
                }
                candidate
            }
            value.isEmpty() || allowed -> {
                // Empty text is omitted from string rendering but still needs a DOM node. An
                // allowed mismatch can also be empty on the server and present on the client.
                document.createTextNode("").also { missingText ->
                    pendingTextsToInsert += PendingText(
                        parent = frame.node,
                        anchor = candidate,
                        text = missingText,
                    )
                }
            }
            else -> mismatchAtChild(
                "text()",
                index,
                "expected text ${value.quoted()}, found ${candidate.describe()}",
            )
        }

        if (!allowed && text.data != value) {
            mismatchAtChild(
                "text()",
                index,
                "expected text ${value.quoted()}, found text ${text.data.quoted()}",
            )
        }

        claimedNodes += text
        return text
    }

    /** Validates server-only text without retaining it as a Compose-managed child. */
    private fun claimRawText(value: String, allowMismatch: Boolean) {
        ensureHydrating()

        val frame = currentFrame
        val candidate = frame.nextNode
        // An allowed mismatch can be empty on the server, which renders no text node at all.
        if (candidate == null && (value.isEmpty() || allowMismatch)) return

        val index = frame.nextChildIndex++
        frame.nextNode = candidate?.nextSibling
        val text = candidate as? Text
            ?: mismatchAtChild(
                "text()",
                index,
                "expected raw text ${value.quoted()}, found ${candidate.describe()}",
            )
        if (!allowMismatch && text.data != value) {
            mismatchAtChild(
                "text()",
                index,
                "expected raw text ${value.quoted()}, found text ${text.data.quoted()}",
            )
        }
    }

    override fun down(node: DomNodeWrapper) {
        if (isHydrating) {
            if (node.node !in claimedNodes) {
                mismatchAtCurrentNode("entered a node that was not claimed")
            }
            frames += Frame(
                node = node.node,
                allowsContentMismatch = node.allowsHydrationMismatch(),
            ).also { frame ->
                if (nodesWithClaimedRawChildren.remove(node.node)) {
                    frame.nextNode = null
                }
            }
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
        ensureComplete()
        current.remove(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        ensureComplete()
        current.move(from, to, count)
    }

    override fun onClear() {
        ensureComplete()
        rootNode.clear()
    }

    override fun onEndChanges() {
        if (isHydrating) {
            finishHydration()
        }
    }

    fun finishHydration() {
        ensureHydrating()
        if (frames.size != 1) {
            mismatchAtCurrentNode("composition ended before leaving the current node")
        }
        skipRootBoundaryWhitespace(currentFrame)
        verifyComplete(currentFrame)
        if (claimedNodes.isNotEmpty()) {
            mismatchAtCurrentNode("composition ended before all claimed nodes were processed")
        }
        if (nodesWithClaimedRawChildren.isNotEmpty()) {
            mismatchAtCurrentNode("composition ended before all raw-text claimed nodes were processed")
        }

        finalizeHydratedNodes()
        applyPendingDomMutations()
        state = State.Complete
        frames.clear()
    }

    fun abortHydration() {
        if (!isHydrating) return
        state = State.Aborted
        claimedNodes.clear()
        nodesWithClaimedRawChildren.clear()
        nodesToRemoveAfterHydration.clear()
        pendingTextsToInsert.clear()
        pendingDomMutations.clear()
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

    private fun skipRootBoundaryWhitespace(frame: Frame, expectedText: String? = null) {
        while (true) {
            val whitespace = frame.nextNode as? Text ?: return
            if (
                whitespace.data == expectedText ||
                whitespace.data.any { it !in AsciiWhitespaceCharacters }
            ) {
                return
            }
            nodesToRemoveAfterHydration += whitespace
            frame.nextNode = whitespace.nextSibling
        }
    }

    private fun ensureHydrating() {
        check(isHydrating) { "Hydration is no longer active" }
    }

    private fun ensureComplete() {
        check(state == State.Complete) {
            "DOM mutations are only allowed after hydration completes (state: $state)"
        }
    }

    private fun finalizeHydratedNodes() {
        pendingTextsToInsert.forEach { pending ->
            pending.parent.insertBefore(pending.text, pending.anchor)
        }
        nodesToRemoveAfterHydration.forEach { node ->
            node.parentNode?.removeChild(node)
        }
        pendingTextsToInsert.clear()
        nodesToRemoveAfterHydration.clear()
    }

    private fun applyPendingDomMutations() {
        val mutations = pendingDomMutations.toList()
        pendingDomMutations.clear()
        mutations.forEach { mutation -> mutation() }
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

/**
 * Opt-out of hydration mismatch reporting for a single element, requested by
 * [org.jetbrains.compose.web.attributes.AttrsScope.allowHydrationMismatch].
 *
 * Composition completes before the server-rendered DOM is claimed, so the flag is written while
 * composing the element's attributes and read while its DOM is claimed and verified.
 */
internal class HydrationMismatchAllowance {
    var isAllowed: Boolean = false
}

/** Implemented by node wrappers of elements that can opt out of hydration mismatch reporting. */
internal interface HydrationMismatchAware {
    val allowsHydrationMismatch: Boolean
}

private fun DomNodeWrapper.allowsHydrationMismatch(): Boolean =
    (this as? HydrationMismatchAware)?.allowsHydrationMismatch == true

private fun Node.pathName(): String = when (this) {
    is Element -> tagName.lowercase()
    is Text -> "text()"
    else -> nodeName
}

private fun Node?.asHydrationTextBoundaryMarker(): Comment? =
    (this as? Comment)?.takeIf { it.data == HydrationTextBoundaryMarker }

private fun Node?.describe(): String = when (this) {
    null -> "the end of the children"
    is Element -> "<${tagName.lowercase()}>"
    is Text -> "text ${data.quoted()}"
    is Comment -> if (data == HydrationTextBoundaryMarker) {
        "an internal text boundary"
    } else {
        "<!--$data-->"
    }
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
