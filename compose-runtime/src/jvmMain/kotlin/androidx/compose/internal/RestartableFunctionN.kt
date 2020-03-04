/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.internal

import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.FrameManager
import androidx.compose.Stable
import androidx.compose.remember
import kotlin.jvm.functions.FunctionN

@Stable
class RestartableFunctionN<R>(
    val key: Any,
    private val tracked: Boolean,
    override val arity: Int
) : FunctionN<R> {
    private var _block: Any? = null

    fun update(block: Any) {
        if (block != this._block) {
            if (tracked) {
                FrameManager.recordWrite(this, false)
            }
            this._block = block as FunctionN<*>
        }
    }

    override fun invoke(vararg args: Any?): R {
        val c = args.last() as Composer<*>
        c.startRestartGroup(key)
        if (tracked) {
            FrameManager.recordRead(this)
        }
        @Suppress("UNCHECKED_CAST")
        val result = (_block as FunctionN<*>)(*args) as R
        c.endRestartGroup()?.updateScope { nc ->
            this(*(args.slice(0 until args.size - 1) + nc).toTypedArray())
        }
        return result
    }
}

@Suppress("unused")
@Composable
fun restartableFunctionN(
    key: Int,
    tracked: Boolean,
    arity: Int,
    block: Any
): RestartableFunctionN<*> = remember {
    RestartableFunctionN<Any>(key, tracked, arity)
}.apply { update(block) }

@Suppress("unused")
fun restartableFunctionNInstance(
    key: Int,
    tracked: Boolean,
    arity: Int,
    block: Any
): RestartableFunctionN<*> = RestartableFunctionN<Any>(key, tracked, arity).apply { update(block) }
