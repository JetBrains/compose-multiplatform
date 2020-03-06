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

package androidx.compose.internal

import androidx.compose.Composable
import androidx.compose.Composer
import androidx.compose.RecomposeScope
import androidx.compose.Stable
import androidx.compose.remember

/**
 * A Restart is created to hold composable lambdas to track when they are invoked allowing
 * the invocations to be invalidated when a new composable lambda is created during composition.
 *
 * This allows much of the call-graph to be skipped when a composable function is passed through
 * multiple levels of composable functions.
 */
@Stable
class RestartableFunction<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16,
        P17, P18, P19, P20, P21, R>(val key: Any, private val tracked: Boolean) : Function0<R>,
    Function1<Composer<*>, R>,
    Function2<P1, Composer<*>, R>,
    Function3<P1, P2, Composer<*>, R>,
    Function4<P1, P2, P3, Composer<*>, R>,
    Function5<P1, P2, P3, P4, Composer<*>, R>,
    Function6<P1, P2, P3, P4, P5, Composer<*>, R>,
    Function7<P1, P2, P3, P4, P5, P6, Composer<*>, R>,
    Function8<P1, P2, P3, P4, P5, P6, P7, Composer<*>, R>,
    Function9<P1, P2, P3, P4, P5, P6, P7, P8, Composer<*>, R>,
    Function10<P1, P2, P3, P4, P5, P6, P7, P8, P9, Composer<*>, R>,
    Function11<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, Composer<*>, R>,
    Function12<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, Composer<*>, R>,
    Function13<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, Composer<*>, R>,
    Function14<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, Composer<*>, R>,
    Function15<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, Composer<*>, R>,
    Function16<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, Composer<*>, R>,
    Function17<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16,
            Composer<*>, R>,
    Function18<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17,
            Composer<*>, R>,
    Function19<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18,
            Composer<*>, R>,
    Function20<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18,
            P19, Composer<*>, R>,
    Function21<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18,
            P19, P20, Composer<*>, R>,
    Function22<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10, P11, P12, P13, P14, P15, P16, P17, P18,
            P19, P20, P21, Composer<*>, R> {
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
                if (this.scope == null) {
                    this.scope = scope
                } else {
                    (scopes ?: (mutableListOf<RecomposeScope>().also { scopes = it })).add(scope)
                }
                scope.used = true
            }
        }
    }

    fun update(block: Any) {
        if (_block != block) {
            _block = block
            trackWrite()
        }
    }

    override fun invoke(): R = error("Expected a composer")

    override fun invoke(c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (c: Composer<*>) -> R)(c)
        c.endRestartGroup()?.updateScope(this as (Composer<*>) -> Unit)
        return result
    }

    override fun invoke(p1: P1, c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (p1: P1, c: Composer<*>) -> R)(p1, c)
        c.endRestartGroup()?.updateScope { nc -> this(p1, nc) }
        return result
    }

    override fun invoke(p1: P1, p2: P2, c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (p1: P1, p2: P2, c: Composer<*>) -> R)(p1, p2, c)
        c.endRestartGroup()?.updateScope { nc -> this(p1, p2, nc) }
        return result
    }

    override fun invoke(p1: P1, p2: P2, p3: P3, c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (p1: P1, p2: P2, p3: P3, c: Composer<*>) -> R)(p1, p2, p3, c)
        c.endRestartGroup()?.updateScope { nc -> this(p1, p2, p3, nc) }
        return result
    }

    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (p1: P1, p2: P2, p3: P3, p4: P4, c: Composer<*>) -> R)(
            p1,
            p2,
            p3,
            p4,
            c
        )
        c.endRestartGroup()?.updateScope { nc -> this(p1, p2, p3, p4, nc) }
        return result
    }

    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, c: Composer<*>) -> R)(
            p1,
            p2,
            p3,
            p4,
            p5,
            c
        )
        c.endRestartGroup()?.updateScope { nc -> this(p1, p2, p3, p4, p5,
            nc) }
        return result
    }

    override fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, c: Composer<*>): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
            p1: P1,
            p2: P2,
            p3: P3,
            p4: P4,
            p5: P5,
            p6: P6,
            c: Composer<*>
        ) -> R)(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, nc)
        }
        return result
    }

    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
            p1: P1,
            p2: P2,
            p3: P3,
            p4: P4,
            p5: P5,
            p6: P6,
            p7: P7,
            c: Composer<*>
        ) -> R)(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, nc)
        }
        return result
    }

    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
            p1: P1,
            p2: P2,
            p3: P3,
            p4: P4,
            p5: P5,
            p6: P6,
            p7: P7,
            p8: P8,
            c: Composer<*>
        ) -> R) (
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, nc)
        }
        return result
    }

    override fun invoke(
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9,
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
            p1: P1,
            p2: P2,
            p3: P3,
            p4: P4,
            p5: P5,
            p6: P6,
            p7: P7,
            p8: P8,
            p9: P9,
            c: Composer<*>
        ) -> R)(
            p1,
            p2,
            p3,
            p4,
            p5,
            p6,
            p7,
            p8,
            p9,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
            this(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10, p11, p12, p13, p14, p15, p16, p17, nc)
        }
        return result
    }

    override fun invoke(
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
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            c: Composer<*>
        ) -> R)(
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
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
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
                nc
            )
        }
        return result
    }

    override fun invoke(
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
        p19: P19,
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            p19: P19,
            c: Composer<*>
        ) -> R)(
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
            p19,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
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
                p19,
                nc
            )
        }
        return result
    }

    override fun invoke(
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
        p19: P19,
        p20: P20,
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            p19: P19,
            p20: P20,
            c: Composer<*>
        ) -> R)(
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
            p19,
            p20,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
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
                p19,
                p20,
                nc
            )
        }
        return result
    }

    override fun invoke(
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
        p19: P19,
        p20: P20,
        p21: P21,
        c: Composer<*>
    ): R {
        c.startRestartGroup(key)
        trackRead(c)
        val result = (_block as (
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
            p19: P19,
            p20: P20,
            p21: P21,
            c: Composer<*>
        ) -> R)(
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
            p19,
            p20,
            p21,
            c
        )
        c.endRestartGroup()?.updateScope { nc ->
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
                p19,
                p20,
                p21,
                nc
            )
        }
        return result
    }
}

@Suppress("unused")
@Composable
fun restartableFunction(key: Int, tracked: Boolean, block: Any) =
    remember {
        RestartableFunction<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any,
            Any, Any, Any, Any, Any, Any, Any, Any>(key, tracked)
    }.apply { update(block) }

@Suppress("unused")
fun restartableFunctionInstance(key: Int, tracked: Boolean, block: Any) =
    RestartableFunction<Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any,
            Any, Any, Any, Any, Any, Any, Any, Any>(key, tracked).apply { update(block) }
