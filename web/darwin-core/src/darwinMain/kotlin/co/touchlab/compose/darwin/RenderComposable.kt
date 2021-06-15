package org.jetbrains.compose.web

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.Composition
import androidx.compose.runtime.ControlledComposition
import androidx.compose.runtime.DefaultMonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import co.touchlab.compose.darwin.UIKitApplier
import co.touchlab.compose.darwin.UIViewWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.UIKit.UILabel
import platform.UIKit.UIView
import platform.UIKit.UIViewController

interface UIViewScope<out TView : UIView>

/**
 * Platform-specific mechanism for starting a monitor of global snapshot state writes
 * in order to schedule the periodic dispatch of snapshot apply notifications.
 * This process should remain platform-specific; it is tied to the threading and update model of
 * a particular platform and framework target.
 *
 * Composition bootstrapping mechanisms for a particular platform/framework should call
 * [ensureStarted] during setup to initialize periodic global snapshot notifications.
 */
@ThreadLocal
internal object GlobalSnapshotManager {
    private var started = false
    private var commitPending = false
    private var removeWriteObserver: (ObserverHandle)? = null

    private val scheduleScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    fun ensureStarted() {
        if (!started) {
            started = true
            removeWriteObserver = Snapshot.registerGlobalWriteObserver(globalWriteObserver)
        }
    }

    private val globalWriteObserver: (Any) -> Unit = {
        // Race, but we don't care too much if we end up with multiple calls scheduled.
        if (!commitPending) {
            commitPending = true
            schedule {
                commitPending = false
                Snapshot.sendApplyNotifications()
            }
        }
    }

    /**
     * List of deferred callbacks to run serially. Guarded by its own monitor lock.
     */
    private val scheduledCallbacks = mutableListOf<() -> Unit>()

    /**
     * Guarded by [scheduledCallbacks]'s monitor lock.
     */
    private var isSynchronizeScheduled = false

    /**
     * Synchronously executes any outstanding callbacks and brings snapshots into a
     * consistent, updated state.
     */
    private fun synchronize() {
        scheduledCallbacks.forEach { it.invoke() }
        scheduledCallbacks.clear()
        isSynchronizeScheduled = false
    }

    private fun schedule(block: () -> Unit) {
        scheduledCallbacks.add(block)
        if (!isSynchronizeScheduled) {
            isSynchronizeScheduled = true
            scheduleScope.launch { synchronize() }
        }
    }
}

@Composable
fun Text(value: String) {
    ComposeNode<UIViewWrapper, UIKitApplier>(
        factory = { UIViewWrapper(UILabel()) },
        update = {
            set(value) { value -> (view as UILabel).text = value }
        },
    )
}

fun UIViewController.startComposable() {
    renderComposable(view) {
        Text("Hello world")
    }
}

/**
 * Use this method to mount the composition at the certain [root]
 *
 * @param root - the [Element] that will be the root of the DOM tree managed by Compose
 * @param content - the Composable lambda that defines the composition content
 *
 * @return the instance of the [Composition]
 */
fun <TView : UIView> renderComposable(
    root: TView,
    content: @Composable UIViewScope<TView>.() -> Unit
): Composition {
    GlobalSnapshotManager.ensureStarted()

    val context = DefaultMonotonicFrameClock + Dispatchers.Main // JsMicrotasksDispatcher()
    val recomposer = Recomposer(context)
    val composition = ControlledComposition(
        applier = UIKitApplier(UIViewWrapper(root)),
        parent = recomposer
    )
    val scope = object : UIViewScope<TView> {}
    composition.setContent @Composable {
        content(scope)
    }

    CoroutineScope(context).launch(start = CoroutineStart.UNDISPATCHED) {
        recomposer.runRecomposeAndApplyChanges()
    }
    return composition
}

///**
// * Use this method to mount the composition at the element with id - [rootElementId].
// *
// * @param rootElementId - the id of the [Element] that will be the root of the DOM tree managed
// * by Compose
// * @param content - the Composable lambda that defines the composition content
// *
// * @return the instance of the [Composition]
// */
//@Suppress("UNCHECKED_CAST")
//fun renderComposable(
//    rootElementId: String,
//    content: @Composable DOMScope<Element>.() -> Unit
//): Composition = renderComposable(
//    root = document.getElementById(rootElementId)!!,
//    content = content
//)
//
///**
// * Use this method to mount the composition at the [HTMLBodyElement] of the current document
// *
// * @param content - the Composable lambda that defines the composition content
// *
// * @return the instance of the [Composition]
// */
//fun renderComposableInBody(
//    content: @Composable DOMScope<HTMLBodyElement>.() -> Unit
//): Composition = renderComposable(
//    root = document.getElementsByTagName("body")[0] as HTMLBodyElement,
//    content = content
//)