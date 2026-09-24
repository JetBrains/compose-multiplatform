@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package org.jetbrains.compose.web.dom

import androidx.compose.runtime.AbstractApplier
import kotlinx.browser.dom.Comment
import kotlinx.browser.dom.Element
import kotlinx.browser.dom.Node
import kotlinx.browser.dom.Text
import org.jetbrains.compose.web.HtmlValidationMode
import org.jetbrains.compose.web.HydrationMismatchException
import org.jetbrains.compose.web.internal.runtime.DomNodeWrapper
import org.jetbrains.compose.web.internal.runtime.browserDocument

/**
 * Claims the existing DOM during initial composition, then switches to normal DOM mutations.
 * Works by maintaining stack of frames.
 */
internal class HydrationDomApplier(
    root: DomNodeWrapper,
    val validationMode: HtmlValidationMode,
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
        var mergedTextChildCount: Int = 0
        var expectedMergedText: StringBuilder? = null
        var serverMergedText: String = ""
    }

    private data class PendingText(
        val parent: Node,
        val anchor: Node?,
        val text: Text,
    )

    private val rootNode = root.node
    private val frames = mutableListOf(Frame(rootNode))
    private val claimedNodes = NativeNodeSet()  // claimed nodes that still need to be called by insertBottomUp
    private val nodesWithClaimedRawChildren = NativeNodeSet()
    private val mergedTextsToInitialize = NativeNodeSet()
    private val allowedTextsToInitialize = NativeNodeSet()
    // Boundary markers and formatting-only root text must remain in place until hydration succeeds.
    private val nodesToRemoveAfterHydration = mutableListOf<Node>()
    private val pendingTextsToInsert = mutableListOf<PendingText>()
    private val pendingDomMutations = mutableListOf<() -> Unit>()
    private val hydrationAbortActions = mutableListOf<() -> Unit>()
    private var hasClaimedRootNode = false
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

    fun onAbortHydration(cleanup: () -> Unit) {
        if (isHydrating) hydrationAbortActions += cleanup
    }

    fun mismatch(detail: String): Nothing = mismatchAtCurrentNode(detail)

    /** Initializes split or allowed text. Fast hydration retains other server values. */
    fun initializeText(text: Text, value: String) {
        when (state) {
            State.Hydrating -> {
                val mergedText = mergedTextsToInitialize.remove(text)
                val allowedText = allowedTextsToInitialize.remove(text)
                if ((mergedText || allowedText || validationMode == HtmlValidationMode.Strict) &&
                    !text.matchesText(value)
                ) {
                    pendingDomMutations += { text.data = value }
                }
            }
            State.Complete -> if (!text.matchesText(value)) text.data = value
            State.Aborted -> Unit
        }
    }

    /** Claims the next element by local name and namespace in both modes. */
    fun claimElement(tagName: String, namespace: String): Element {
        ensureHydrating()

        val frame = currentFrame
        // allows whitespace around root node, that won't cause a hydration mismatch
        if (frame.node === rootNode && !hasClaimedRootNode) {
            skipRootBoundaryWhitespace(frame)
        }
        val index = frame.nextChildIndex++
        val candidate = frame.nextNode
        frame.nextNode = candidate?.nextSibling
        val element = candidate as? Element
            ?: mismatchAtChild(
                tagName,
                index,
                elementMismatchDescription(tagName, namespace, candidate),
            )
        if (!element.matchesElement(tagName, namespace)) {
            mismatchAtChild(
                tagName,
                index,
                elementMismatchDescription(tagName, namespace, element),
            )
        }

        if (frame.node === rootNode) hasClaimedRootNode = true
        claimedNodes += element
        return element
    }

    /** Claims an element whose server-only text is not represented by a Compose DOM node. */
    fun claimElementWithRawText(
        tagName: String,
        namespace: String,
        rawText: () -> RawTextContent,
        allowContentMismatch: Boolean,
    ): Element {
        val element = claimElement(tagName, namespace)
        frames += Frame(element, allowsContentMismatch = allowContentMismatch)
        try {
            claimRawText(rawText, allowContentMismatch)
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
        val parent = frame.node as? Element
        if (
            isHtmlRawTextElement(parent?.localName, parent?.namespaceURI) ||
            isHtmlRcdataElement(parent?.localName, parent?.namespaceURI)
        ) {
            return claimMergedTextChild(frame, value)
        }
        // An omitted empty text must not consume whitespace belonging to a later text node.
        if (frame.node === rootNode && !hasClaimedRootNode && value.isNotEmpty()) {
            skipRootBoundaryWhitespace(frame, expectedText = value)
        }
        val allowed = frame.allowsContentMismatch
        val index = frame.nextChildIndex++
        val candidate = frame.nextNode
        val text = when {
            candidate is Text && (allowed || value.isNotEmpty() || candidate.data.isEmpty()) -> {
                if (frame.node === rootNode) hasClaimedRootNode = true
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
                browserDocument.createTextNode("").also { missingText ->
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

        if (validationMode == HtmlValidationMode.Strict && !allowed && !text.matchesText(value)) {
            mismatchAtChild(
                "text()",
                index,
                "expected text ${value.quoted()}, found text ${text.data.quoted()}",
            )
        }

        if (allowed) allowedTextsToInitialize += text
        claimedNodes += text
        return text
    }

    // HTML raw text and RCDATA cannot contain boundary comments. Claim the parsed text node once,
    // then restore the Compose text boundaries only after the entire hydration has succeeded.
    private fun claimMergedTextChild(frame: Frame, value: String): Text {
        val isFirstTextChild = frame.mergedTextChildCount == 0
        frame.mergedTextChildCount++
        if (validationMode == HtmlValidationMode.Strict) {
            val expected = frame.expectedMergedText ?: StringBuilder().also {
                frame.expectedMergedText = it
            }
            expected.append(value)
        }

        val index = frame.nextChildIndex++
        val serverText = if (isFirstTextChild) {
            frame.nextNode?.let { candidate ->
                candidate as? Text ?: mismatchAtChild(
                    "text()", index, "expected text, found ${candidate.describe()}",
                )
            }
        } else null
        val text = if (serverText != null) {
            if (validationMode == HtmlValidationMode.Strict) {
                frame.serverMergedText = serverText.data
            }
            frame.nextNode = serverText.nextSibling
            serverText
        } else {
            browserDocument.createTextNode("").also { text ->
                pendingTextsToInsert += PendingText(frame.node, frame.nextNode, text)
            }
        }
        claimedNodes += text

        if (serverText != null && validationMode == HtmlValidationMode.Fast && !frame.allowsContentMismatch) {
            // Preserve a single server text node. Multiple Compose nodes need their original
            // boundaries restored before later updates can target them independently. The
            // deferred mutation reads the final count after this parent frame has been popped.
            pendingDomMutations += {
                if (frame.mergedTextChildCount > 1) serverText.data = value
            }
        } else {
            mergedTextsToInitialize += text
        }
        return text
    }

    /** Validates server-only text without retaining it as a Compose-managed child. */
    private fun claimRawText(rawText: () -> RawTextContent, allowMismatch: Boolean) {
        ensureHydrating()

        val frame = currentFrame
        val candidate = frame.nextNode
        val expectedValue = if (validationMode == HtmlValidationMode.Strict) rawText().text else null
        // An allowed mismatch can be empty on the server, which renders no text node at all.
        if (candidate == null && (expectedValue == null || expectedValue.isEmpty() || allowMismatch)) return

        val index = frame.nextChildIndex++
        frame.nextNode = candidate?.nextSibling
        val text = candidate as? Text
            ?: mismatchAtChild(
                "text()",
                index,
                if (expectedValue == null) {
                    "expected raw text, found ${candidate.describe()}"
                } else {
                    "expected raw text ${expectedValue.quoted()}, found ${candidate.describe()}"
                },
            )
        if (expectedValue != null && !allowMismatch && !text.matchesText(expectedValue)) {
            mismatchAtChild(
                "text()",
                index,
                "expected raw text ${expectedValue.quoted()}, found text ${text.data.quoted()}",
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
                if (nodesWithClaimedRawChildren.remove(node.node) ||
                    (node.node as? Element)?.localName == "noscript"
                ) {
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
        // Failed initialization owns no DOM. Preserve the tree, including any mutations
        // already applied if initialization failed during the commit phase.
        if (state == State.Aborted) return
        ensureComplete()
        rootNode.clearChildren()
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

        // Validation is complete. From here, abortHydration can cancel pending work and
        // listeners, but cannot roll back DOM changes (including arbitrary property setters).
        finalizeHydratedNodes()
        applyPendingDomMutations()
        state = State.Complete
        hydrationAbortActions.clear()
        mergedTextsToInitialize.clear()
        allowedTextsToInitialize.clear()
        frames.clear()
    }

    /** Cancels pending work and listeners; does not roll back mutations already committed. */
    fun abortHydration() {
        if (!isHydrating) return
        state = State.Aborted
        claimedNodes.clear()
        nodesWithClaimedRawChildren.clear()
        mergedTextsToInitialize.clear()
        allowedTextsToInitialize.clear()
        nodesToRemoveAfterHydration.clear()
        pendingTextsToInsert.clear()
        pendingDomMutations.clear()
        frames.clear()
        // Deferred mutations can throw before DisposableEffects start owning the listeners.
        hydrationAbortActions.forEach { it() }
        hydrationAbortActions.clear()
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
        frame.expectedMergedText?.let { expected ->
            val parent = frame.node as? Element
            // Raw text follows HTML input normalization. RCDATA preserves CR via character references.
            val text = if (isHtmlRawTextElement(parent?.localName, parent?.namespaceURI)) {
                expected.toString().normalizeHtmlInputCharacters()
            } else {
                expected.toString()
            }
            if (!frame.allowsContentMismatch && text != frame.serverMergedText) {
                mismatchAtCurrentNode(
                    "expected text ${text.quoted()}, found text ${frame.serverMergedText.quoted()}",
                )
            }
        }
        val extra = frame.nextNode ?: return
        mismatchAtCurrentNode("expected end of node, found extra ${extra.describe()}")
    }

    private fun skipRootBoundaryWhitespace(frame: Frame, expectedText: String? = null) {
        while (true) {
            val whitespace = frame.nextNode as? Text ?: return
            if (
                whitespace.data == expectedText ||
                whitespace.nextSibling.asHydrationTextBoundaryMarker() != null ||
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

internal fun Node.clearChildren() {
    while (firstChild != null) {
        removeChild(firstChild!!)
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
    is Element -> localName
    is Text -> "text()"
    else -> nodeName
}

private fun Node?.asHydrationTextBoundaryMarker(): Comment? =
    (this as? Comment)?.takeIf { it.data == HydrationTextBoundaryMarker }

private fun elementMismatchDescription(
    expectedLocalName: String,
    expectedNamespace: String,
    found: Node?,
): String = if (found is Element && found.namespaceURI != expectedNamespace) {
    "expected <$expectedLocalName> in namespace ${expectedNamespace.quoted()}, " +
        "found <${found.localName}> in namespace ${found.namespaceURI?.quoted() ?: "null"}"
} else {
    "expected <$expectedLocalName>, found ${found.describe()}"
}

private fun Node?.describe(): String = when (this) {
    null -> "the end of the children"
    is Element -> "<$localName>"
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

// Use a JavaScript Set to preserve DOM identity and avoid Wasm wrapper overhead.
private class NativeNodeSet {
    private val set = BrowserIdentitySet()

    operator fun plusAssign(node: Node) {
        set.add(node)
    }

    operator fun contains(node: Node): Boolean = set.has(node)

    fun remove(node: Node): Boolean = set.delete(node)

    fun isNotEmpty(): Boolean = set.size != 0

    fun clear() {
        set.clear()
    }
}

@kotlin.js.JsName("Set")
private external class BrowserIdentitySet : kotlinx.browser.JsAny {
    constructor()

    val size: Int

    fun add(node: Node): BrowserIdentitySet

    fun has(node: Node): Boolean

    fun delete(node: Node): Boolean

    fun clear()
}
