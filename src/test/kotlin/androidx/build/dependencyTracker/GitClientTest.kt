/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.dependencyTracker

import androidx.build.dependencyTracker.GitClient.Companion.CHANGED_FILES_CMD_PREFIX
import androidx.build.dependencyTracker.GitClient.Companion.PREV_MERGE_CMD
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.junit.runners.model.Statement
import java.io.File

@RunWith(JUnit4::class)
class GitClientTest {
    private val logger = ToStringLogger()
    private val commandRunner = MockCommandRunner(logger)
    private val client = GitClient(
            workingDir = File("."),
            logger = logger,
            commandRunner = commandRunner)

    @Rule
    @JvmField
    val attachLogRule = TestRule { base, _ ->
        object : Statement() {
            override fun evaluate() {
                try {
                    base.evaluate()
                } catch (t: Throwable) {
                    throw Exception(
                            """
                                test failed with msg: ${t.message}
                                logs:
                                ${logger.buildString()}
                            """.trimIndent(),
                            t
                    )
                }
            }
        }
    }

    @Test
    fun findMerge() {
        commandRunner.addReply(
                PREV_MERGE_CMD,
                "abcdefghij (m/androidx-md, aosp/androidx-md) Merge blah blah into and"
        )
        assertEquals(
                "abcdefghij",
                client.findPreviousMergeCL())
    }

    @Test
    fun findMerge_fail() {
        assertNull(client.findPreviousMergeCL())
    }

    @Test
    fun findChangesSince() {
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX mySha",
                """
                    a/b/c.java
                    d/e/f.java
                """.trimIndent()
        )
        assertEquals(
                listOf("a/b/c.java", "d/e/f.java"),
                client.findChangedFilesSince(sha = "mySha", includeUncommitted = true))
    }

    @Test
    fun findChangesSince_empty() {
        assertEquals(
                emptyList<String>(),
                client.findChangedFilesSince("foo"))
    }

    @Test
    fun findChangesSince_twoCls() {
        commandRunner.addReply(
                "$CHANGED_FILES_CMD_PREFIX otherSha mySha",
                """
                    a/b/c.java
                    d/e/f.java
                """.trimIndent()
        )
        assertEquals(
                listOf("a/b/c.java", "d/e/f.java"),
                client.findChangedFilesSince(
                        sha = "mySha",
                        top = "otherSha",
                        includeUncommitted = false))
    }

    private class MockCommandRunner(val logger: ToStringLogger) : GitClient.CommandRunner {
        private val replies = mutableMapOf<String, List<String>>()

        fun addReply(command: String, response: String) {
            logger.info("add reply. cmd: $command response: $response")
            replies[command] = response.split(System.lineSeparator())
        }

        override fun execute(command: String): List<String> {
            return replies.getOrDefault(command, emptyList()).also {
                logger.info("cmd: $command response: $it")
            }
        }
    }
}