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

@file:OptIn(InternalComposeApi::class)
package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composer
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.Stable
import androidx.compose.runtime.EMPTY
import kotlin.jvm.functions.FunctionN

private const val SLOTS_PER_INT = 10

@Stable
@ComposeCompilerApi
class ComposableLambdaN<R>(
    val key: Int,
    private val tracked: Boolean,
    private val sourceInformation: String?,
    override val arity: Int
) : FunctionN<R> {
    private var _block: Any? = null

    fun update(block: Any, composer: Composer<*>?) {
        if (block != this._block) {
            if (tracked) {
                composer?.recordWriteOf(this)
            }
            this._block = block as FunctionN<*>
        }
    }

    private fun realParamCount(params: Int): Int {
        var realParams = params
        realParams-- // composer parameter
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
        val allArgsButLast = args.slice(0 until args.size - 1).toTypedArray()
        val lastChanged = args[args.size - 1] as Int
        c.startRestartGroup(key, sourceInformation)
        val dirty = lastChanged or if (c.changed(this))
            differentBits(realParams)
        else
            sameBits(realParams)
        if (tracked) {
            c.recordReadOf(this)
        }
        @Suppress("UNCHECKED_CAST")
        val result = (_block as FunctionN<*>)(*allArgsButLast, dirty) as R
        c.endRestartGroup()?.updateScope { nc, _ ->
            val params = args.slice(0 until realParams).toTypedArray()
            @Suppress("UNUSED_VARIABLE")
            val changed = args[realParams + 1] as Int
            val changedN = args.slice(realParams + 2 until args.size).toTypedArray()
            this(
                *params,
                nc,
                changed or 0b1,
                *changedN
            )
        }
        return result
    }
}

@Suppress("unused")
@ComposeCompilerApi
fun composableLambdaN(
    composer: Composer<*>,
    key: Int,
    tracked: Boolean,
    sourceInformation: String?,
    arity: Int,
    block: Any
): ComposableLambdaN<*> {
    composer.startReplaceableGroup(key)
    val slot = composer.nextSlot()
    val result = if (slot === EMPTY) {
        val value = ComposableLambdaN<Any>(key, tracked, sourceInformation, arity)
        composer.updateValue(value)
        value
    } else {
        @Suppress("UNCHECKED_CAST")
        slot as ComposableLambdaN<Any>
    }
    result.update(block, composer)
    composer.endReplaceableGroup()
    return result
}

@Suppress("unused")
@ComposeCompilerApi
fun composableLambdaNInstance(
    key: Int,
    tracked: Boolean,
    arity: Int,
    block: Any
): ComposableLambdaN<*> = ComposableLambdaN<Any>(
    key,
    tracked,
    null,
    arity
).apply { update(block, null) }
