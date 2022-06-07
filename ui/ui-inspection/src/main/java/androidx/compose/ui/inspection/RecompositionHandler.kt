/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.inspection

import android.util.Log
import androidx.annotation.GuardedBy
import androidx.compose.runtime.Composer
import androidx.inspection.ArtTooling

private const val START_RESTART_GROUP = "startRestartGroup(I)Landroidx/compose/runtime/Composer;"
private const val SKIP_TO_GROUP_END = "skipToGroupEnd()V"

/**
 * Detection of recompose counts and skips from installing runtime hooks.
 */
class RecompositionHandler(private val artTooling: ArtTooling) {

    /**
     * For each composable store the recomposition [count] and [skips].
     */
    class Data(var count: Int, var skips: Int)

    /**
     * Key of a Composable method.
     *
     * The [key] identified the runtime method and the [anchorHash] identified a
     * specific compose node.
     */
    private data class MethodKey(val key: Int, val anchorHash: Int)

    private val lock = Any()
    @GuardedBy("lock")
    private var currentlyCollecting = false
    @GuardedBy("lock")
    private var hooksInstalled = false
    @GuardedBy("lock")
    private val counts = mutableMapOf<MethodKey, Data>()
    @GuardedBy("lock")
    private var lastMethodKey: Int = 0

    fun changeCollectionMode(startCollecting: Boolean, keepCounts: Boolean) {
        synchronized(lock) {
            if (startCollecting != currentlyCollecting) {
                if (!hooksInstalled) {
                    installHooks()
                }
                currentlyCollecting = startCollecting
            }
            if (!keepCounts) {
                counts.clear()
            }
        }
    }

    fun getCounts(key: Int, anchorHash: Int): Data? {
        synchronized(lock) {
            return counts[MethodKey(key, anchorHash)]
        }
    }

    /**
     * We install 3 hooks:
     * - entry hook for ComposerImpl.startRestartGroup gives us the [MethodKey.key]
     * - exit hook for ComposerImpl.startRestartGroup gives us the [MethodKey.anchorHash]
     * - entry hook for ComposerImpl.skipToGroupEnd converts a recompose count to a skip count.
     */
    private fun installHooks() {
        val composerImpl = try {
            Class.forName("${Composer::class.java.name}Impl")
        } catch (ex: Throwable) {
            Log.w("Compose", "Could not install recomposition hooks", ex)
            return
        }

        artTooling.registerEntryHook(composerImpl, START_RESTART_GROUP) { _, args ->
            synchronized(lock) {
                lastMethodKey = args[0] as Int
            }
        }

        artTooling.registerExitHook(composerImpl, START_RESTART_GROUP) { composer: Composer ->
            synchronized(lock) {
                if (currentlyCollecting) {
                    composer.recomposeScopeIdentity?.hashCode()?.let { anchor ->
                        val data = counts.getOrPut(MethodKey(lastMethodKey, anchor)) { Data(0, 0) }
                        data.count++
                    }
                }
            }
            composer
        }

        artTooling.registerEntryHook(composerImpl, SKIP_TO_GROUP_END) { obj, _ ->
            synchronized(lock) {
                if (currentlyCollecting) {
                    val composer = obj as? Composer
                    composer?.recomposeScopeIdentity?.hashCode()?.let { anchor ->
                        counts[MethodKey(lastMethodKey, anchor)]?.let {
                            it.count--
                            it.skips++
                        }
                    }
                }
            }
        }

        hooksInstalled = true
    }
}
