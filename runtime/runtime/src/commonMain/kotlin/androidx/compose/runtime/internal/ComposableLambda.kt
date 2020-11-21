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

@file:Suppress("UNCHECKED_CAST")
@file:OptIn(InternalComposeApi::class)
package androidx.compose.runtime.internal

import androidx.compose.runtime.ComposeCompilerApi
import androidx.compose.runtime.Composer
import androidx.compose.runtime.EMPTY
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.Stable

private const val SLOTS_PER_INT = 10
private const val BITS_PER_SLOT = 3

internal fun bitsForSlot(bits: Int, slot: Int): Int {
    val realSlot = slot.rem(SLOTS_PER_INT)
    return bits shl (realSlot * BITS_PER_SLOT + 1)
}

internal fun sameBits(slot: Int): Int = bitsForSlot(0b01, slot)
internal fun differentBits(slot: Int): Int = bitsForSlot(0b10, slot)

/**
 * A Restart is created to hold composable lambdas to track when they are invoked allowing
 * the invocations to be invalidated when a new composable lambda is created during composition.
 *
 * This allows much of the call-graph to be skipped when a composable function is passed through
 * multiple levels of composable functions.
 */
@Stable
@ComposeCompilerApi
/* ktlint-disable parameter-list-wrapping */ // TODO(https://github.com/pinterest/ktlint/issues/921): reenable
class ComposableLambda<
    P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16,
    P17, P18, R>(
    val key: Int,
    private val tracked: Boolean,
    private val sourceInformation: String?
) :
    Function2<Composer<*>, Int, R>,
    Function3<P1, Composer<*>, Int, R>,
    Function4<P1, P2, Composer<*>, Int, R>,
    Function5<P1, P2, P3, Composer<*>, Int, R>,
    Function6<P1, P2, P3, P4, Composer<*>, Int, R>,
    Function7<P1, P2, P3, P4, P5, Composer<*>, Int, R>,
    Function8<P1, P2, P3, P4, P5, P6, Composer<*>, Int, R>,
    Function9<P1, P2, P3, P4, P5, P6, P7, Composer<*>, Int, R>,
    Function10<P1, P2, P3, P4, P5, P6, P7, P8, Composer<*>, Int, R>,
    Function11<P1, P2, P3, P4, P5, P6, P7, P8, P9, Composer<*>, Int, R>,
    Function13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, Composer<*>, Int, Int, R>,
    Function14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, Composer<*>, Int, Int, R>,
    Function15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, Composer<*>, Int, Int, R>,
    Function16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, Composer<*>, Int, Int, R>,
    Function17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, Composer<*>, Int,
        Int, R>,
    Function18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, Composer<*>,
        Int, Int, R>,
    Function19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16,
        Composer<*>, Int, Int, R>,
    Function20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17,
        Composer<*>, Int, Int, R>,
    Function21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18,
        Composer<*>, Int, Int, R> {
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

    private fun trackRead(composer: Composer<*>) {
        if (tracked) {
            val scope = composer.currentRecomposeScope
            if (scope != null) {
                // Find the first invalid scope and replace it or record it if no scopes are invalid
                scope.used = true
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
        if (_block != block) {
            val oldBlockNull = _block == null
            _block = block
            if (!oldBlockNull) {
                trackWrite()
            }
        }
    }

    override operator fun invoke(c: Composer<*>, changed: Int): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(0) else sameBits(0)
        val result = (_block as (c: Composer<*>, changed: Int) -> R)(c, dirty)
        c.endRestartGroup()?.updateScope(this as (Composer<*>, Int) -> Unit)
        return result
    }

    override operator fun invoke(p1: P1, c: Composer<*>, changed: Int): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(1) else sameBits(1)
        val result = (
            _block as (
                p1: P1,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ -> this(p1, nc, changed or 0b1) }
        return result
    }

    override operator fun invoke(p1: P1, p2: P2, c: Composer<*>, changed: Int): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(2) else sameBits(2)
        val result = (_block as (p1: P1, p2: P2, c: Composer<*>, changed: Int) -> R)(
            p1,
            p2,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ -> this(p1, p2, nc, changed or 0b1) }
        return result
    }

    override operator fun invoke(p1: P1, p2: P2, p3: P3, c: Composer<*>, changed: Int): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(3) else sameBits(3)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ -> this(p1, p2, p3, nc, changed or 0b1) }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        c: Composer<*>,
        changed: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(4) else sameBits(4)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, nc, changed or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        c: Composer<*>,
        changed: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(5) else sameBits(5)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, nc, changed or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        c: Composer<*>,
        changed: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(6) else sameBits(6)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, nc, changed or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        c: Composer<*>,
        changed: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(7) else sameBits(7)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, nc, changed or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        c: Composer<*>,
        changed: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(8) else sameBits(8)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, nc, changed or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        c: Composer<*>,
        changed: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed or if (c.changed(this)) differentBits(9) else sameBits(9)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                c: Composer<*>,
                changed: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            c,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, nc, changed or 0b1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(10) else sameBits(10)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, nc, changed or 0b1, changed)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(11) else sameBits(11)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, nc, changed or 0b1, changed1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(12) else sameBits(12)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, nc, changed or 0b1, changed1)
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(13) else sameBits(13)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                p13: P13,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            p13,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                nc,
                changed or 0b1,
                changed1
            )
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(14) else sameBits(14)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                p13: P13,
                p14: P14,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            p13,
            p14,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                nc,
                changed or 0b1,
                changed1
            )
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(15) else sameBits(15)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                p13: P13,
                p14: P14,
                p15: P15,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            p13,
            p14,
            p15,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                nc,
                changed or 0b1,
                changed1
            )
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(16) else sameBits(16)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                p13: P13,
                p14: P14,
                p15: P15,
                p16: P16,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            p13,
            p14,
            p15,
            p16,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                nc,
                changed or 0b1,
                changed1
            )
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(17) else sameBits(17)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                p13: P13,
                p14: P14,
                p15: P15,
                p16: P16,
                p17: P17,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            p13,
            p14,
            p15,
            p16,
            p17,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                p17,
                nc,
                changed or 0b1,
                changed1
            )
        }
        return result
    }

    override operator fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        p10: P10,
        p11: P11,
        p12: P12,
        p13: P13,
        p14: P14,
        p15: P15,
        p16: P16,
        p17: P17,
        p18: P18,
        c: Composer<*>,
        changed: Int,
        changed1: Int
    ): R {
        c.startRestartGroup(key, sourceInformation)
        trackRead(c)
        val dirty = changed1 or if (c.changed(this)) differentBits(18) else sameBits(18)
        val result = (
            _block as (
                p1: P1,
                p2: P2,
                p3: P3,
                p4: P4,
                p5: P5,
                p6: P6,
                p7: P7,
                p8: P8,
                p9: P9,
                p10: P10,
                p11: P11,
                p12: P12,
                p13: P13,
                p14: P14,
                p15: P15,
                p16: P16,
                p17: P17,
                p18: P18,
                c: Composer<*>,
                changed: Int,
                changed1: Int
            ) -> R
            )(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            p10,
            p11,
            p12,
            p13,
            p14,
            p15,
            p16,
            p17,
            p18,
            c,
            changed,
            dirty
        )
        c.endRestartGroup()?.updateScope { nc, _ ->
            this(
                p1,
                p2,
                p3,
                p4,
                p5,
                p6,
                p7,
                p8,
                p9,
                p10,
                p11,
                p12,
                p13,
                p14,
                p15,
                p16,
                p17,
                p18,
                nc,
                changed or 0b1,
                changed1
            )
        }
        return result
    }
}

private fun RecomposeScope?.replacableWith(other: RecomposeScope) =
    this == null || !this.valid || this == other || this.anchor == other.anchor

@OptIn(ComposeCompilerApi::class)
private typealias CLambda = ComposableLambda<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any,
    Any, Any, Any, Any, Any, Any, Any, Any, Any>

@Suppress("unused")
@ComposeCompilerApi
fun composableLambda(
    composer: Composer<*>,
    key: Int,
    tracked: Boolean,
    sourceInformation: String?,
    block: Any
): CLambda {
    composer.startReplaceableGroup(key)
    val slot = composer.nextSlot()
    val result = if (slot === EMPTY) {
        val value = CLambda(key, tracked, sourceInformation)
        composer.updateValue(value)
        value
    } else {
        slot as CLambda
    }
    result.update(block)
    composer.endReplaceableGroup()
    return result
}

@Suppress("unused")
@ComposeCompilerApi
fun composableLambdaInstance(key: Int, tracked: Boolean, block: Any) =
    CLambda(key, tracked, null).apply { update(block) }
