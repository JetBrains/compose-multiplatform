/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.text.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.InternalTextApi
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch

private const val DEBUG = false

/** See kdoc on actual interfaces. */
@ExperimentalTextApi
@Immutable
expect interface PlatformTextInputPlugin<T : PlatformTextInputAdapter>

/** See kdoc on actual interfaces. */
@ExperimentalTextApi
expect interface PlatformTextInputAdapter

/**
 * Calls the [PlatformTextInputAdapter]'s onDisposed method. This is done through a proxy method
 * because expect interfaces aren't allowed to have default implementations.
 */
@OptIn(ExperimentalTextApi::class)
internal expect fun PlatformTextInputAdapter.dispose()

/**
 * Represents the text input plugin system to instances of [PlatformTextInputAdapter], and provides
 * methods that allow adapters to interact with it. Instances are passed to
 * [PlatformTextInputPlugin.createAdapter].
 */
@ExperimentalTextApi
sealed interface PlatformTextInput {
    /**
     * Requests that the platform input be connected to this receiver until either:
     * - [releaseInputFocus] is called, or
     * - another adapter calls [requestInputFocus].
     */
    fun requestInputFocus()

    /**
     * If this adapter currently holds input focus, tells the platform that it is no longer handling
     * input. If this adapter does not hold input focus, does nothing.
     */
    fun releaseInputFocus()
}

/**
 * The entry point to the text input plugin API.
 *
 * This is a low-level API for code that talks directly to the platform input method framework
 * (i.e. `InputMethodManager`). Higher-level text input APIs in the Foundation library are more
 * appropriate for most cases. _**The only time a new service type and adapter should be defined is
 * if you are building your own text input system from scratch, including your own `TextField`
 * composables.**_
 *
 * It is expected that the total number of adapters for a given registry will be only one in most
 * cases, or two in rare cases where an entirely separate text input system – from platform IME
 * client to `TextField` composables – is being used in the same composition.
 *
 * See [rememberAdapter] for more information.
 */
// Implementation note: this is separated as a sealed interface + impl pair to avoid exposing
// @InternalTextApi members to code reading LocalPlatformTextInputAdapterProvider.
@ExperimentalTextApi
@Stable
sealed interface PlatformTextInputPluginRegistry {
    /**
     * Returns an instance of the [PlatformTextInputAdapter] type [T] specified by [plugin].
     *
     * The returned adapter instance will be shared by all callers of this method passing the same
     * [plugin] object. The adapter will be created when the first call is made, then cached and
     * returned by every subsequent call in the same or subsequent compositions. When there are no
     * longer any calls to this method for a given plugin, the adapter instance will be
     * [disposed][PlatformTextInputAdapter.onDisposed].
     *
     * @param T The type of [PlatformTextInputAdapter] that [plugin] creates.
     * @param plugin The factory for adapters and the key into the cache of those adapters.
     */
    @Composable
    fun <T : PlatformTextInputAdapter> rememberAdapter(plugin: PlatformTextInputPlugin<T>): T
}

/**
 * Implementation of [PlatformTextInputPluginRegistry] that manages a map of adapters to cached
 * services and allows retrieval of the first active one.
 *
 * @param factory A platform-specific function that invokes the appropriate `create` method on
 * a [PlatformTextInputAdapter] to return a service.
 */
// This doesn't violate the EndsWithImpl rule because it's not public API.
@Suppress("EndsWithImpl")
@OptIn(ExperimentalTextApi::class)
@InternalTextApi
class PlatformTextInputPluginRegistryImpl(
    private val factory: (
        PlatformTextInputPlugin<*>,
        PlatformTextInput
    ) -> PlatformTextInputAdapter
) : PlatformTextInputPluginRegistry {

    /**
     * This must be a snapshot state object because it gets modified by [getOrCreateAdapter] which
     * may be called from composition.
     */
    private val adaptersByPlugin =
        mutableStateMapOf<PlatformTextInputPlugin<*>, AdapterWithRefCount<*>>()

    private var adaptersMayNeedDisposal = false

    /**
     * The plugin of the last adapter whose [PlatformTextInput] called
     * [PlatformTextInput.requestInputFocus] and hasn't called
     * [PlatformTextInput.releaseInputFocus] yet.
     *
     * Not backed by snapshot state – input focus management happens from UI focus events, not from
     * composition.
     */
    private var focusedPlugin: PlatformTextInputPlugin<*>? = null

    /**
     * Returns the currently-focused adapter, or null if no adapter is focused.
     *
     * An adapter can request input focus by calling [PlatformTextInput.requestInputFocus]. It will
     * keep input focus until either:
     *  1. It calls [PlatformTextInput.releaseInputFocus], or
     *  2. Another adapter calls [PlatformTextInput.requestInputFocus].
     */
    val focusedAdapter: PlatformTextInputAdapter?
        get() = adaptersByPlugin[focusedPlugin]?.adapter
            .also { if (DEBUG) println("Found focused PlatformTextInputAdapter: $it") }

    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalTextApi::class)
    @Composable
    override fun <T : PlatformTextInputAdapter> rememberAdapter(
        plugin: PlatformTextInputPlugin<T>
    ): T {
        // The AdapterHandle is a RememberObserver, so it will automatically get callbacks for
        // disposal.
        val adapterHandle = remember(plugin) { getOrCreateAdapter(plugin) }

        // If this composition is discarded, the refcount increment won't be applied, and any newly
        // instantiated adapter won't be added to the map, so we only need to handle actual
        // disposal.
        val scope = rememberCoroutineScope()
        DisposableEffect(adapterHandle) {
            onDispose {
                if (DEBUG) println("Disposing PlatformTextInputAdapter handle")
                // Dispose returning true means that the adapter's refcount may have reached zero,
                // so we should confirm that after all effects have been ran and if necessary
                // actually dispose it.
                if (adapterHandle.dispose()) {
                    // We need to wait to check for tombstoned adapters until after all effects'
                    // onDisposes have been ran. New coroutines will only be resumed after all
                    // effects have been ran and changes applied. However, because the coroutine
                    // scope will also be cancelled by this function being removed from composition,
                    // we need to launch with the NonCancellable job to ensure it runs.
                    // Note that dispose() returning true only means that there _may_ be a
                    // tombstoned adapter. So this may launch a coroutine that no-ops, but that
                    // should be relatively rare (only when multiple text fields are removed and
                    // added in the same composition), and is cheap enough that it's not worth the
                    // extra bookkeeping to avoid.
                    scope.launch(NonCancellable) { disposeTombstonedAdapters() }
                }
            }
        }
        return adapterHandle.adapter
    }

    /**
     * Returns the text input service [T] managed by [plugin].
     *
     * The first time this method is called for a given [PlatformTextInputAdapter], the adapter's
     * platform-specific factory method is called to instantiate the service. The service is then
     * added to the cache and returned. Subsequent calls passing the same adapter, over the entire
     * lifetime of this service provider, will return the same service instance. It is expected that
     * adapters will be singleton objects, and in most apps there will only ever be one or maybe
     * two input adapters in use, since each input adapter represents an entire text input
     * subsystem.
     *
     * @param T The type of the [PlatformTextInputAdapter] that [plugin] creates.
     * @param plugin The factory for service objects and the key into the cache of those objects.
     */
    @Suppress("UnnecessaryOptInAnnotation")
    @OptIn(ExperimentalTextApi::class)
    @InternalTextApi
    fun <T : PlatformTextInputAdapter> getOrCreateAdapter(
        plugin: PlatformTextInputPlugin<T>
    ): AdapterHandle<T> {
        if (DEBUG) println("Getting PlatformTextInputAdapter for plugin $plugin")
        @Suppress("UNCHECKED_CAST")
        val adapterWithRefCount = (adaptersByPlugin[plugin] as AdapterWithRefCount<T>?)
            ?: instantiateAdapter(plugin)
        adapterWithRefCount.incrementRefCount()
        return AdapterHandle(adapterWithRefCount.adapter, onDispose = {
            adapterWithRefCount.decrementRefCount()
        })
    }

    /**
     * Cleans up any [PlatformTextInputAdapter] instances that have a 0 refcount.
     *
     * Should only be called after the composition is finished, to ensure that adapters won't be
     * reused later during the same composition.
     */
    private fun disposeTombstonedAdapters() {
        if (DEBUG) println(
            "Composition applied, checking for tombstoned PlatformTextInputAdapters…"
        )
        // This method may be called multiple times for the same frame, but we only need the first
        // one to do the work.
        if (adaptersMayNeedDisposal) {
            adaptersMayNeedDisposal = false
            val toDispose = adaptersByPlugin.entries.filter { it.value.isRefCountZero }
            toDispose.fastForEach { (plugin, adapter) ->
                if (DEBUG) println(
                    "Disposing PlatformTextInputAdapter. " +
                        "plugin=$plugin, adapter=$adapter"
                )
                if (focusedPlugin == plugin) {
                    focusedPlugin = null
                }
                adaptersByPlugin -= plugin
                adapter.adapter.dispose()
            }
        }
    }

    private fun <T : PlatformTextInputAdapter> instantiateAdapter(
        plugin: PlatformTextInputPlugin<T>
    ): AdapterWithRefCount<T> {
        val input = AdapterInput(plugin)

        @Suppress("UNCHECKED_CAST")
        val newAdapter = this.factory(plugin, input) as T
        val withRefCount = AdapterWithRefCount(newAdapter)
        adaptersByPlugin[plugin] = withRefCount
        if (DEBUG) println(
            "Instantiated new PlatformTextInputAdapter. " +
                "plugin=$plugin, adapter=$newAdapter"
        )
        return withRefCount
    }

    @InternalTextApi
    class AdapterHandle<T : PlatformTextInputAdapter>(
        val adapter: T,
        private val onDispose: () -> Boolean
    ) {
        private var disposed = false

        fun dispose(): Boolean {
            check(!disposed) { "AdapterHandle already disposed" }
            disposed = true
            return onDispose()
        }
    }

    private inner class AdapterWithRefCount<T : PlatformTextInputAdapter>(val adapter: T) {
        /**
         * This is backed by a MutableState because it is incremented in [getOrCreateAdapter] which
         * can be called directly from a composition, inside a [remember] block.
         */
        private var refCount by mutableStateOf(0)

        val isRefCountZero get() = refCount == 0

        fun incrementRefCount() {
            refCount++
            if (DEBUG) println(
                "Incremented PlatformTextInputAdapter refcount: $refCount (adapter=$adapter)"
            )
        }

        fun decrementRefCount(): Boolean {
            refCount--
            if (DEBUG) println(
                "Decremented PlatformTextInputAdapter refcount: $refCount (adapter=$adapter)"
            )
            check(refCount >= 0) {
                "AdapterWithRefCount.decrementRefCount called too many times (refCount=$refCount)"
            }
            // Defer actual disposal until after the composition is finished, in case it goes from
            // 0 back to 1 later during the same composition pass.
            if (refCount == 0) {
                adaptersMayNeedDisposal = true
                return true
            }
            return false
        }
    }

    private inner class AdapterInput(
        private val plugin: PlatformTextInputPlugin<*>
    ) : PlatformTextInput {
        override fun requestInputFocus() {
            if (DEBUG) println("PlatformTextInputAdapter requested input focus. plugin=$plugin")
            focusedPlugin = plugin
        }

        override fun releaseInputFocus() {
            if (DEBUG) println("PlatformTextInputAdapter released input focus. plugin=$plugin")
            if (focusedPlugin == plugin) {
                focusedPlugin = null
            }
        }
    }
}