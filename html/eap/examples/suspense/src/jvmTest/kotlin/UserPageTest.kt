/*
 * Copyright 2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlin.test.*

class UserPageTest {
    private val user = User(42, "Maya Chen", "Design engineer", "Amsterdam", "A public biography")

    @Test
    fun shellArrivesBeforeLookupAndSuccessTransfersTheUser() = runBlocking<Unit> {
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            var lookups = 0
            val stream = renderUserPage(42) {
                lookups++
                release.await()
                user
            }
            val chunks = Channel<String>(Channel.UNLIMITED)
            val collecting = launch { stream.chunks.collect { chunks.send(it) } }
            try {
                val shell = chunks.receive()
                assertContains(shell, "data-user-loading=\"true\"")
                assertContains(shell, "Header and footer arrive with the skeleton.")
                assertContains(shell, "src=\"/app.js\"")
                assertFalse(shell.contains("data-user-id=\"42\""))
                assertFalse(collecting.isCompleted)

                release.complete(Unit)
                val profile = chunks.receive()
                assertContains(profile, ".replace(")
                assertContains(profile, "Maya Chen")
                assertContains(profile, "data-compose-hydration-root")
                assertContains(profile, UserHydrationId)
                assertContains(profile, "A public biography")
                assertContains(chunks.receive(), ".complete(")
                collecting.join()
                assertEquals(1, lookups)
            } finally {
                stream.cancel()
                collecting.cancel()
            }
        }
    }

    @Test
    fun failureReplacesSkeletonWithPublicErrorWithoutTransferringException() = runBlocking<Unit> {
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            val stream = renderUserPage(0) {
                release.await()
                error("private database connection details")
            }
            val chunks = mutableListOf<String>()
            stream.chunks.collect {
                chunks += it
                release.complete(Unit)
            }
            assertContains(chunks.first(), "data-user-loading=\"true\"")
            assertContains(chunks[1], "This profile couldn't be loaded.")
            assertFalse(chunks[1].contains("data-compose-hydration-root"))
            assertFalse(chunks.joinToString("").contains("private database connection details"))
            assertContains(chunks.last(), ".complete(")
        }
    }

    @Test
    fun cancellingTheResponseCancelsTheLookup() = runBlocking<Unit> {
        withTimeout(5_000) {
            val started = CompletableDeferred<Unit>()
            val stopped = CompletableDeferred<Unit>()
            val stream = renderUserPage(42) {
                started.complete(Unit)
                try { awaitCancellation() } finally { stopped.complete(Unit) }
            }
            started.await()
            stream.cancel()
            stopped.await()
            assertFailsWith<CancellationException> { stream.chunks.toList() }
        }
    }
}
