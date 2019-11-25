/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.compose

abstract class Recomposer {

    companion object {

        /**
         * Check if there's pending changes to be recomposed in this thread
         *
         * @return true if there're pending changes in this thread, false otherwise
         */
        fun hasPendingChanges() = current().hasPendingChanges()

        /**
         * Retrieves [Recomposer] for the current thread. Needs to be the main thread.
         */
        @TestOnly
        fun current(): Recomposer {
            require(isMainThread()) {
                "No Recomposer for this Thread"
            }
            return threadRecomposer.get()
        }

        internal fun recompose(composable: @Composable() () -> Unit, composer: Composer<*>) =
            current().recompose(composable, composer)

        private val threadRecomposer = ThreadLocal { createRecomposer() }
    }

    private val composers = mutableSetOf<Composer<*>>()

    @Suppress("PLUGIN_WARNING", "PLUGIN_ERROR")
    private fun recompose(composable: @Composable() () -> Unit, composer: Composer<*>) {
        composer.runWithCurrent {
            val composerWasComposing = composer.isComposing
            try {
                composer.isComposing = true
                FrameManager.composing {
                    trace("Compose:recompose") {
                        var complete = false
                        try {
                            composer.startRoot()
                            composer.startGroup(invocation)
                            composable()
                            composer.endGroup()
                            composer.endRoot()
                            complete = true
                        } finally {
                            if (!complete) composer.abortRoot()
                        }
                    }
                }
            } finally {
                composer.isComposing = composerWasComposing
            }
            // TODO(b/143755743)
            composer.applyChanges()
            if (!composerWasComposing) {
                FrameManager.nextFrame()
            }
        }
    }

    private fun performRecompose(composer: Composer<*>): Boolean {
        if (composer.isComposing) return false
        return composer.runWithCurrent {
            var hadChanges: Boolean
            try {
                composer.isComposing = true
                hadChanges = FrameManager.composing {
                    composer.recompose()
                }
                composer.applyChanges()
            } finally {
                composer.isComposing = false
            }
            hadChanges
        }
    }

    internal abstract fun hasPendingChanges(): Boolean

    internal fun scheduleRecompose(composer: Composer<*>) {
        composers.add(composer)
        scheduleChangesDispatch()
    }

    internal fun recomposeSync(composer: Composer<*>): Boolean {
        return performRecompose(composer)
    }

    protected abstract fun scheduleChangesDispatch()

    protected fun dispatchRecomposes() {
        val cs = composers.toTypedArray()
        composers.clear()
        cs.forEach { performRecompose(it) }
        FrameManager.nextFrame()
    }

    /**
     * Used to recompose changes from [scheduleChangesDispatch] immediately without waiting.
     *
     * This is supposed to be used in tests only.
     */
    @TestOnly
    abstract fun recomposeSync()
}

internal expect fun createRecomposer(): Recomposer