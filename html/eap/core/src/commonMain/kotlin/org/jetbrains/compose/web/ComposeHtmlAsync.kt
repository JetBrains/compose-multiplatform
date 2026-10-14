/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlin.coroutines.ContinuationInterceptor
import kotlin.random.Random
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.jetbrains.compose.web.dom.HtmlNamespace
import org.jetbrains.compose.web.dom.LocalComposeHtmlContext
import org.jetbrains.compose.web.dom.LocalStringHtmlChanges
import org.jetbrains.compose.web.dom.StringComposeHtmlContext
import org.jetbrains.compose.web.dom.StringHtmlApplier
import org.jetbrains.compose.web.dom.StringHtmlElementNode
import org.jetbrains.compose.web.dom.StringHtmlNode
import org.jetbrains.compose.web.dom.StringHtmlNodeWrapper
import org.jetbrains.compose.web.dom.StringHtmlRawTextNode
import org.jetbrains.compose.web.dom.StringHtmlSuspenseNode
import org.jetbrains.compose.web.dom.StringHtmlTextNode
import org.jetbrains.compose.web.dom.requireHtmlParserStableTableElement
import org.jetbrains.compose.web.dom.requireHtmlParserStableTableText

/** Options for async HTML rendering. Use `withTimeout` to limit render time. */
class AsyncHtmlRenderOptions(
    val hydratable: Boolean = true,
    /** Also checks that streamed content stays unchanged until completion. */
    val validateStrictly: Boolean = defaultHtmlValidationMode() == HtmlValidationMode.Strict,
    /** CSP nonce for stream patch scripts. */
    val nonce: String? = null,
    /** Unique stream prefix within a document. Allows only ASCII letters, digits and underscores. */
    val identifierPrefix: String = "s" + Random.nextLong().toULong().toString(16).padStart(16, '0'),
    /** Reports unhandled render and producer failures. Ignores cancellation. */
    val onError: (Throwable) -> Unit = {},
) {
    init {
        require(identifierPrefix.isNotEmpty() && identifierPrefix.all {
            it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_'
        }) { "identifierPrefix must contain only ASCII letters, digits and underscores" }
    }
}

/**
 * An HTML stream with one consumer. Rendering waits for the consumer to accept each chunk.
 * Cancellation, sink failure, or [cancel] disposes the composition and cancels its effects.
 * Always consume or cancel the stream.
 */
interface HtmlStream {
    val chunks: Flow<String>
    fun cancel()
}

/**
 * Composes [content] into final HTML without patch scripts. Runs coroutine effects and waits for
 * all [SuspensePending] calls to be removed, including those in nested boundaries and fallbacks.
 * Cancels remaining effects and flow collectors without waiting for them to finish.
 * Producer state changes are applied normally. Keep state local to the request.
 * [HydrationRoot] is supported. Transfer the final public state needed by the client.
 */
suspend fun composeHtmlToStringAsync(
    options: AsyncHtmlRenderOptions = AsyncHtmlRenderOptions(),
    content: @Composable () -> Unit,
): String = coroutineScope {
    val stream = startAsyncHtmlRender(options, progressive = false, content)
    try { stream.chunks.single() } finally { stream.cancel() }
}

/**
 * Renders in this scope and returns when the initial HTML is ready. [Suspense] sends its fallback,
 * then patches in content when ready. [SuspensePending] outside a boundary delays the initial HTML.
 *
 * Write chunks to the same response in order. The browser must allow patch scripts.
 * Load application scripts in the initial HTML. Scripts in replacement content do not run.
 *
 * Keep streamed content unchanged and wait for completion before hydrating.
 * Failures before the initial HTML throw here. Later failures fail [HtmlStream.chunks] and call
 * [AsyncHtmlRenderOptions.onError]. Loaders must preserve coroutine cancellation.
 */
suspend fun CoroutineScope.composeHtmlToStreamAsync(
    options: AsyncHtmlRenderOptions = AsyncHtmlRenderOptions(),
    content: @Composable () -> Unit,
): HtmlStream = startAsyncHtmlRender(options, progressive = true, content)

// Yield for server recomposition without waiting for a display frame.
private class HtmlRenderFrameClock : MonotonicFrameClock {
    private val origin = TimeSource.Monotonic.markNow()
    private var lastFrame = 0L

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        yield()
        lastFrame = maxOf(lastFrame + 1, origin.elapsedNow().inWholeNanoseconds)
        return onFrame(lastFrame)
    }
}

private class AsyncHtmlSession(private val options: AsyncHtmlRenderOptions) {
    val root = StringHtmlElementNode.root()
    val registry = SuspenseRegistry()
    private var nextId = 0
    val outstanding = linkedMapOf<String, StringHtmlSuspenseNode>()
    private val flushed = mutableListOf<FlushedRegion>()

    private data class FlushedNode(
        val node: StringHtmlNode,
        val value: Any?,
        val children: List<FlushedNode> = emptyList(),
    )

    private data class FlushedRegion(
        val root: StringHtmlElementNode,
        val holes: Set<StringHtmlSuspenseNode>,
        val snapshot: FlushedNode,
    )

    private fun visibleNodes(): Set<StringHtmlNode> = buildSet {
        fun visit(node: StringHtmlNode) {
            add(node)
            when (node) {
                is StringHtmlElementNode -> node.children.forEach(::visit)
                is StringHtmlSuspenseNode -> visit(node.visible)
                else -> Unit
            }
        }
        visit(root)
    }

    fun pruneOutstanding() {
        val visible = visibleNodes()
        outstanding.entries.removeAll { it.value !in visible }
    }

    fun readyPatches(): List<String> {
        pruneOutstanding()
        // Capture ready visible boundaries before suspending on the sink.
        return buildList {
            for ((id, node) in outstanding.toMap()) {
                if (!node.pending) {
                    outstanding.remove(id)
                    node.streamId = null
                    prepareStreamTree(node.content, node.parentTagName, node.parentNamespace)
                    add(streamReplacement(options, id, html(node.content)))
                    recordFlushed(node.content)
                }
            }
        }
    }

    // Pending ranges and final hydration state may change. Other streamed nodes must stay unchanged.
    private fun snapshot(node: StringHtmlNode, holes: Set<StringHtmlSuspenseNode>): FlushedNode = when (node) {
        is StringHtmlElementNode -> FlushedNode(node, node.attributes,
            if (node.hasAttribute(HydrationStateAttribute)) emptyList()
            else node.children.map { snapshot(it, holes) })
        is StringHtmlSuspenseNode -> if (node in holes) FlushedNode(node, null)
            else FlushedNode(node, node.pending, listOf(snapshot(node.visible, holes)))
        is StringHtmlTextNode -> FlushedNode(node, node.text)
        is StringHtmlRawTextNode -> FlushedNode(node, node.content.text)
    }

    fun recordFlushed(node: StringHtmlElementNode = root) {
        if (!options.validateStrictly) return
        val holes = outstanding.values.toSet()
        flushed += FlushedRegion(node, holes, snapshot(node, holes))
    }

    fun validateFlushed() {
        if (!options.validateStrictly) return
        val visible = visibleNodes()
        for (region in flushed) {
            if (region.root in visible) check(region.snapshot == snapshot(region.root, region.holes)) {
                "Compose HTML content changed after it was streamed; keep flushed regions stable until completion"
            }
        }
    }

    fun documentTail(): String {
        val document = root.children.singleOrNull() as? StringHtmlElementNode ?: return ""
        val body = document.children.lastOrNull() as? StringHtmlElementNode ?: return ""
        return if (document.tagName == "html" && document.namespace == HtmlNamespace &&
            body.tagName == "body" && body.namespace == HtmlNamespace) "</body></html>" else ""
    }

    fun prepareStreamTree(
        node: StringHtmlNode = root,
        parentTagName: String? = null,
        parentNamespace: String? = null,
    ) {
        when (node) {
            is StringHtmlElementNode -> {
                if (node.hasAttribute(HydrationRootAttribute) || node.hasAttribute(HydrationStateAttribute)) {
                    node.streamOwner = options.identifierPrefix
                }
                if (node.tagName != null && parentNamespace == HtmlNamespace) {
                    requireHtmlParserStableTableElement(parentTagName, node.tagName)
                }
                val tagName = node.tagName ?: parentTagName
                val namespace = node.namespace ?: parentNamespace
                node.children.forEach { prepareStreamTree(it, tagName, namespace) }
            }
            is StringHtmlSuspenseNode -> {
                node.parentTagName = parentTagName
                node.parentNamespace = parentNamespace
                if (node.pending && node.streamId == null) {
                    node.streamId = options.identifierPrefix + "_" + nextId++
                }
                node.streamId?.let { outstanding[it] = node }
                prepareStreamTree(node.visible, parentTagName, parentNamespace)
            }
            is StringHtmlTextNode -> if (parentNamespace == HtmlNamespace) {
                requireHtmlParserStableTableText(parentTagName, node.text)
            }
            is StringHtmlRawTextNode -> if (parentNamespace == HtmlNamespace) {
                requireHtmlParserStableTableText(parentTagName, node.content.text)
            }
        }
    }

    fun isComplete(node: StringHtmlNode = root): Boolean = when (node) {
        is StringHtmlSuspenseNode -> !node.pending && isComplete(node.content)
        is StringHtmlElementNode -> node.children.all(::isComplete)
        else -> true
    }

    fun html(node: StringHtmlElementNode = root): String = node.toHtmlString(options.hydratable)

    private fun hydrationStateNodes(): List<StringHtmlElementNode> {
        val states = visibleNodes().filterIsInstance<StringHtmlElementNode>()
            .filter { it.hasAttribute(HydrationStateAttribute) }
        val ids = mutableSetOf<String?>()
        for (node in states) {
            val id = node.attribute(HydrationForAttribute)
            require(ids.add(id)) { "Duplicate Compose hydration state for \"$id\"" }
            for (child in node.children) {
                check(child is StringHtmlRawTextNode) { "Invalid hydration state content" }
            }
        }
        return states
    }

    fun validateHydrationStates() {
        hydrationStateNodes()
    }

    fun finalStates(): List<Pair<String?, String>> = hydrationStateNodes().map { node ->
        node.attribute(HydrationForAttribute) to node.children.joinToString("") {
            (it as StringHtmlRawTextNode).content.text
        }
    }
}

private class RequestHtmlStream(
    private val owner: Job,
    private val options: AsyncHtmlRenderOptions,
    private val failure: CompletableDeferred<Throwable>,
) : HtmlStream {
    private val output = Channel<String>(Channel.RENDEZVOUS)
    // Atomically allow one consumer on every platform.
    private val collected = MutableStateFlow(false)
    var documentTail = ""
    var streamingShell = false

    override val chunks: Flow<String> = flow {
        check(collected.compareAndSet(false, true)) { "HtmlStream supports only one consumer" }
        var receivedShell = false
        try {
            while (true) {
                val result = output.receiveCatching()
                if (result.isClosed) {
                    result.exceptionOrNull()?.let { cause ->
                        if (receivedShell && streamingShell && cause !is CancellationException) {
                            emit(streamFailure(options) + documentTail)
                        }
                        throw cause
                    }
                    break
                }
                emit(result.getOrThrow())
                receivedShell = true
            }
        } finally {
            cancel()
        }
    }

    suspend fun sendChunk(chunk: String) {
        select<Unit> {
            output.onSend(chunk) { }
            failure.onAwait { throw it }
        }
    }

    fun close(cause: Throwable? = null) {
        output.close(cause)
    }

    override fun cancel() {
        owner.cancel()
        output.cancel()
    }
}

private fun Throwable.renderCause(): Throwable {
    var cause = this
    while (cause is CancellationException && cause.cause != null) cause = checkNotNull(cause.cause)
    return cause
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun CoroutineScope.startAsyncHtmlRender(
    options: AsyncHtmlRenderOptions,
    progressive: Boolean,
    content: @Composable () -> Unit,
): HtmlStream {
    val owner = SupervisorJob(coroutineContext[Job])
    val shellReady = CompletableDeferred<Unit>()
    val failure = CompletableDeferred<Throwable>()
    val changes = Channel<Unit>(Channel.CONFLATED)
    val handler = CoroutineExceptionHandler { _, throwable ->
        failure.complete(throwable)
        changes.trySend(Unit)
    }
    val requestDispatcher = coroutineContext[ContinuationInterceptor] as? CoroutineDispatcher ?: Dispatchers.Default
    val dispatcher = try {
        requestDispatcher.limitedParallelism(1)
    } catch (_: UnsupportedOperationException) {
        // Dispatchers.Unconfined cannot limit execution to one task at a time.
        Dispatchers.Default.limitedParallelism(1)
    }
    val scope = CoroutineScope(coroutineContext + owner + handler + dispatcher + HtmlRenderFrameClock())
    val stream = RequestHtmlStream(owner, options, failure)
    val renderJob = scope.launch {
        AsyncHtmlSnapshots.acquire()
        val session = AsyncHtmlSession(options)
        val recomposer = Recomposer(scope.coroutineContext)
        val composition = ControlledComposition(
            StringHtmlApplier(StringHtmlNodeWrapper(session.root)) { changes.trySend(Unit) }, recomposer,
        )
        val runner = scope.launch {
            try { recomposer.runRecomposeAndApplyChanges() }
            catch (throwable: Throwable) {
                val cause = throwable.renderCause()
                if (cause !is CancellationException) failure.complete(cause)
                changes.trySend(Unit)
            }
        }
        val observer = scope.launch {
            recomposer.currentState.collect { changes.trySend(Unit) }
        }
        var renderFailure: Throwable? = null
        fun report(throwable: Throwable) {
            if (throwable !is CancellationException) {
                try { options.onError(throwable) } catch (callbackFailure: Throwable) {
                    if (callbackFailure !== throwable) throwable.addSuppressed(callbackFailure)
                }
            }
        }
        try {
            composition.setContent {
                CompositionLocalProvider(
                    LocalComposeHtmlContext provides StringComposeHtmlContext,
                    LocalStringHtmlChanges provides { changes.trySend(Unit) },
                    LocalSuspenseRegistry provides session.registry,
                    LocalHydratedDocumentContext provides true,
                    LocalHtmlValidationMode provides htmlValidationMode(options.validateStrictly),
                ) { content() }
            }
            var sentShell = false
            while (true) {
                Snapshot.sendApplyNotifications()
                recomposer.awaitIdle()
                if (failure.isCompleted) throw failure.await()
                if (recomposer.currentState.value <= Recomposer.State.ShuttingDown) {
                    // Only cancellation or effect failure stops this recomposer.
                    // Await the failure report unless cancellation interrupts the wait.
                    throw failure.await()
                }
                val complete = session.registry.count == 0 && session.isComplete()
                if (!sentShell && complete) {
                    session.validateHydrationStates()
                    val html = session.html()
                    shellReady.complete(Unit)
                    stream.sendChunk(html)
                    break
                }
                if (progressive && !sentShell && session.registry.count == 0) {
                    session.prepareStreamTree()
                    session.validateHydrationStates()
                    val html = session.html()
                    session.recordFlushed()
                    stream.documentTail = session.documentTail()
                    stream.streamingShell = true
                    shellReady.complete(Unit)
                    stream.sendChunk(html.removeSuffix(stream.documentTail) + streamBootstrap(options))
                    sentShell = true
                } else if (sentShell) {
                    for (patch in session.readyPatches()) stream.sendChunk(patch)
                    if (complete) {
                        session.validateFlushed()
                        stream.sendChunk(streamCompletion(options, session.finalStates()) + stream.documentTail)
                        stream.streamingShell = false
                        break
                    }
                }
                changes.receive()
            }
        } catch (throwable: Throwable) {
            val cause = if (failure.isCompleted) failure.await() else throwable
            renderFailure = cause
            report(cause)
            shellReady.completeExceptionally(cause)
        } finally {
            observer.cancel()
            try { composition.dispose() } catch (cleanupFailure: Throwable) {
                val prior = renderFailure
                if (prior == null) {
                    renderFailure = cleanupFailure
                    report(cleanupFailure)
                } else if (prior !== cleanupFailure) prior.addSuppressed(cleanupFailure)
            } finally {
                recomposer.cancel()
                runner.cancel()
                stream.close(renderFailure)
                withContext(NonCancellable) { AsyncHtmlSnapshots.release() }
            }
        }
    }
    renderJob.invokeOnCompletion { cause ->
        if (cause != null) {
            shellReady.completeExceptionally(cause)
            stream.close(cause)
        }
        owner.cancel()
    }
    try { shellReady.await() } catch (throwable: Throwable) {
        stream.cancel()
        throw throwable
    }
    return stream
}
