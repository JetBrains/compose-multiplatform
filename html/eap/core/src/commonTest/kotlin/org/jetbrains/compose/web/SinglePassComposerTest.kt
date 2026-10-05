/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web

import androidx.compose.runtime.RememberObserver
import kotlin.test.*

class SinglePassComposerTest {
    @Test
    fun disposalReleasesObserversAndSideEffects() {
        val composer = SinglePassComposer()
        val events = mutableListOf<String>()
        composer.updateRememberedValue(observer(events))
        composer.recordSideEffect { events.add("effect") }
        composer.applyEffects()
        composer.dispose(null)

        // Running the lifecycle again must not redispatch callbacks from the disposed request.
        composer.applyEffects()
        composer.dispose(null)
        assertEquals(listOf("remember", "effect", "forget"), events)
    }

    @Test
    fun failedCleanupStillReleasesObserversAndSideEffects() {
        val composer = SinglePassComposer()
        val events = mutableListOf<String>()
        val failure = IllegalStateException("cleanup failure")
        composer.updateRememberedValue(observer(events, failure))
        composer.recordSideEffect { events.add("effect") }
        composer.applyEffects()
        assertSame(failure, assertFailsWith<IllegalStateException> { composer.dispose(null) })

        composer.applyEffects()
        composer.dispose(null)
        assertEquals(listOf("remember", "effect", "forget"), events)
    }

    @Test
    fun abandonedCompositionReleasesObserversAndUnappliedEffects() {
        val composer = SinglePassComposer()
        val events = mutableListOf<String>()
        composer.updateRememberedValue(observer(events))
        composer.recordSideEffect { events.add("effect") }
        composer.dispose(IllegalStateException("composition failure"))

        composer.applyEffects()
        composer.dispose(null)
        assertEquals(listOf("abandon"), events)
    }

    @Test
    fun partialRememberFailurePreservesSharedObserverLifecycle() {
        val composer = SinglePassComposer()
        val events = mutableListOf<String>()
        val original = IllegalStateException("remember failure")
        fun observer(name: String, fails: Boolean = false) = object : RememberObserver {
            override fun onRemembered() {
                events.add("remember $name")
                if (fails) throw original
            }
            override fun onForgotten() {
                events.add("forget $name")
            }
            override fun onAbandoned() {
                events.add("abandon $name")
            }
            override fun equals(other: Any?) = other is RememberObserver
            override fun hashCode(): Int = error("cleanup must use identity")
        }
        val shared = observer("shared")
        val failing = observer("failing", fails = true)
        val unreached = observer("unreached")
        for (observer in listOf(shared, failing, shared, failing, unreached, unreached)) {
            composer.updateRememberedValue(observer)
        }
        composer.recordSideEffect { error("failed remembering must not run side effects") }

        assertSame(original, assertFailsWith<IllegalStateException> { composer.applyEffects() })
        composer.dispose(original)
        assertEquals(
            listOf(
                "remember shared", "remember failing",
                "forget failing", "forget shared", "forget failing", "forget shared",
                "abandon unreached",
            ),
            events,
        )
        composer.applyEffects()
        composer.dispose(null)
    }

    @Test
    fun sharedObserversAreForgottenPerLocationAndAbandonedOnce() {
        for (fail in listOf(false, true)) {
            val composer = SinglePassComposer()
            val events = mutableListOf<String>()
            val shared = observer(events)
            repeat(2) { composer.updateRememberedValue(shared) }
            if (fail) {
                composer.dispose(IllegalStateException("composition failure"))
                assertEquals(listOf("abandon"), events)
            } else {
                composer.applyEffects()
                composer.dispose(null)
                assertEquals(listOf("remember", "remember", "forget", "forget"), events)
            }
        }
    }

    private fun observer(events: MutableList<String>, failure: Throwable? = null) = object : RememberObserver {
        override fun onRemembered() {
            events.add("remember")
        }
        override fun onForgotten() {
            events.add("forget")
            if (failure != null) throw failure
        }
        override fun onAbandoned() {
            events.add("abandon")
        }
    }
}
