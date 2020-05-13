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

import androidx.compose.Composer
import androidx.compose.FrameManager
import androidx.compose.SlotTable
import androidx.compose.Stable
import kotlin.jvm.functions.FunctionN

private const val SLOTS_PER_INT = 15

@Stable
class RestartableFunctionN<R>(
    val key: Int,
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

    private fun realParamCount(params: Int): Int {
        var realParams = params
        realParams-- // composer parameter
        realParams-- // key parameter
        realParams-- // changed parameter
        var changedParams = 1
        while (changedParams * SLOTS_PER_INT < realParams) {
            realParams--
            changedParams++
        }
        return realParams
    }

    override fun invoke(vararg args: Any?): R {
        val realParams = realParamCount(args.size)
        val c = args[realParams] as Composer<*>
        c.startRestartGroup(key)
        if (tracked) {
            FrameManager.recordRead(this)
        }
        @Suppress("UNCHECKED_CAST")
        val result = (_block as FunctionN<*>)(*args) as R
        c.endRestartGroup()?.updateScope { nc, nk, _ ->
            val params = args.slice(0 until realParams).toTypedArray()
            @Suppress("UNUSED_VARIABLE")
            val key = args[realParams + 1] as Int
            val changed = args[realParams + 2] as Int
            val changedN = args.slice(realParams + 3 until args.size).toTypedArray()
            this(
                *params,
                nc,
                nk,
                changed or 0b1,
                *changedN
            )
        }
        return result
    }
}

@Suppress("unused")
fun restartableFunctionN(
    composer: Composer<*>,
    key: Int,
    tracked: Boolean,
    arity: Int,
    block: Any
): RestartableFunctionN<*> {
    composer.startReplaceableGroup(key)
    val slot = composer.nextSlot()
    val result = if (slot === SlotTable.EMPTY) {
        val value = RestartableFunctionN<Any>(key, tracked, arity)
        composer.updateValue(value)
        value
    } else {
        @Suppress("UNCHECKED_CAST")
        slot as RestartableFunctionN<Any>
    }
    result.update(block)
    composer.endReplaceableGroup()
    return result
}

@Suppress("unused")
fun restartableFunctionNInstance(
    key: Int,
    tracked: Boolean,
    arity: Int,
    block: Any
): RestartableFunctionN<*> = RestartableFunctionN<Any>(key, tracked, arity).apply { update(block) }
