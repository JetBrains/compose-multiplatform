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

import android.content.Context

// TODO(lmr): this is really only needed for "composition management", but that could maybe move
// somewhere else. Consider ways to remove this class. Maybe should merge with FrameManager?
class CompositionContext private constructor(val component: Component, val composer: Composer<*>) {
    companion object {
        fun prepare(
            context: Context,
            root: Any,
            component: Component,
            compositionReference: CompositionReference?
        ) = prepare(
            component,
            compositionReference
        ) { ViewComposer(root, context, this) }

        fun prepare(
            component: Component,
            ambientReference: CompositionReference?,
            makeComposer: Recomposer.() -> Composer<*>
        ): CompositionContext {
            val composer = with(Recomposer.current()) {
                makeComposer()
            }
            composer.parentReference = ambientReference
            ambientReference?.registerComposer(composer)
            return CompositionContext(component, composer)
        }
    }

    fun compose() {
        Recomposer.recompose(component, composer)
    }

    /**
     * Recomposes any changes without forcing the [component] to compose and blocks until
     * composition completes.
     */
    fun recomposeSync() {
        Recomposer.current().recomposeSync(composer)
    }
}