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

package androidx.compose.mock

import androidx.compose.AbstractApplier
import androidx.compose.Composable
import androidx.compose.ComposeCompilerApi
import androidx.compose.Composer
import androidx.compose.ExperimentalComposeApi
import androidx.compose.Stable
import androidx.compose.currentComposer

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@OptIn(ExperimentalComposeApi::class)
class ViewApplier(root: View) : AbstractApplier<View>(root) {
    override fun insert(index: Int, instance: View) {
        current.addAt(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.removeAt(index, count)
    }

    override fun move(from: Int, to: Int, count: Int) {
        current.moveAt(from, to, count)
    }
}

@Stable
class MockComposeScope

// TODO(lmr): we should really remove this from our tests
@Suppress("UNCHECKED_CAST")
@OptIn(ComposeCompilerApi::class)
@Composable
fun <P1> MockComposeScope.memoize(
    key: Int,
    p1: P1,
    block: @Composable (p1: P1) -> Unit
) {
    currentComposer.startGroup(key)
    if (!currentComposer.changed(p1)) {
        currentComposer.skipToGroupEnd()
    } else {
        val realFn = block as Function4<P1, Composer<*>, Int, Int, Unit>
        realFn(p1, currentComposer, 0, 0)
    }
    currentComposer.endGroup()
}
