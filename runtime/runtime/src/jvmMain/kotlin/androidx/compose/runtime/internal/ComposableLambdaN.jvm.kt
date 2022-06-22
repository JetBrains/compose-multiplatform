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
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Stable
import kotlin.jvm.functions.FunctionN

@Stable
internal class ComposableLambdaNImpl(
    val key: Int,
    private val tracked: Boolean,
    override val arity: Int
) : ComposableLambdaN {
    private var _block: Any? = null
    private var scope: RecomposeScope? = null
    private var scopes: MutableList<RecomposeScope>? = null

    private fun trackWrite() {
        if (tracked) {
            val scope = this.scope
            if (scope != null) {
                scope.invalidate()
                this.scope = null
            }
            val scopes = this.scopes
            if (scopes != null) {
                for (index in 0 until scopes.size) {
                    val item = scopes[index]
                    item.invalidate()
                }
                scopes.clear()
            }
        }
    }

    private fun trackRead(composer: Composer) {
        if (tracked) {
            val scope = composer.recomposeScope
            if (scope != null) {
                // Find the first invalid scope and replace it or record it if no scopes are invalid
                composer.recordUsed(scope)
                val lastScope = this.scope
                if (lastScope.replacableWith(scope)) {
                    this.scope = scope
                } else {
                    val lastScopes = scopes
                    if (lastScopes == null) {
                        val newScopes = mutableListOf<RecomposeScope>()
                        scopes = newScopes
                        newScopes.add(scope)
                    } else {
                        for (index in 0 until lastScopes.size) {
                            val scopeAtIndex = lastScopes[index]
                            if (scopeAtIndex.replacableWith(scope)) {
                                lastScopes[index] = scope
                                return
                            }
                        }
                        lastScopes.add(scope)
                    }
                }
            }
        }
    }

    fun update(block: Any) {
        if (block != _block) {
            val oldBlockNull = _block == null
            _block = block as FunctionN<*>
            if (!oldBlockNull) {
                trackWrite()
            }
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

    override fun invoke(vararg args: Any?): Any? {
        val realParams = realParamCount(args.size)
        var c = args[realParams] as Composer
        val allArgsButLast = args.slice(0 until args.size - 1).toTypedArray()
        val lastChanged = args[args.size - 1] as Int
        c = c.startRestartGroup(key)
        trackRead(c)
        val dirty = lastChanged or if (c.changed(this))
            differentBits(realParams)
        else
            sameBits(realParams)
        @Suppress("UNCHECKED_CAST")
        val result = (_block as FunctionN<*>)(*allArgsButLast, dirty)
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

@Stable
@ComposeCompilerApi
interface ComposableLambdaN : FunctionN<Any?>

@Suppress("unused")
@ComposeCompilerApi
fun composableLambdaN(
    composer: Composer,
    key: Int,
    tracked: Boolean,
    arity: Int,
    block: Any
): ComposableLambdaN {
    composer.startReplaceableGroup(key)
    val slot = composer.rememberedValue()
    val result = if (slot === Composer.Empty) {
        val value = ComposableLambdaNImpl(key, tracked, arity)
        composer.updateRememberedValue(value)
        value
    } else {
        @Suppress("UNCHECKED_CAST")
        slot as ComposableLambdaNImpl
    }
    result.update(block)
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
): ComposableLambdaN = ComposableLambdaNImpl(
    key,
    tracked,
    arity
).apply { update(block) }
