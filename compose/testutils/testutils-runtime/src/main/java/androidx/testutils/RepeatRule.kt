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

package androidx.testutils

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * A [TestRule] which can be used to run the same test multiple times. Useful when trying to
 * debug flaky tests.
 *
 * To use this [TestRule] do the following.
 *
 * Add the Rule to your JUnit test.
 *
 * ```
 * @get:Rule
 * val repeatRule = RepeatRule();
 * ```
 *
 * Add the [Repeat] annotation to your test case.
 *
 * ```
 * @Test
 * @RepeatRule.Repeat(times = 10)
 * fun yourTestCase() {
 *
 * }
 * ```
 *
 */
class RepeatRule : TestRule {
    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Repeat(val times: Int)

    class RepeatStatement(private val times: Int, private val statement: Statement) :
        Statement() {

        @Throws(Throwable::class)
        override fun evaluate() {
            for (i in 0 until times) {
                statement.evaluate()
            }
        }
    }

    override fun apply(base: Statement, description: Description): Statement {
        val repeat = description.getAnnotation(Repeat::class.java)
        return if (repeat != null) {
            RepeatStatement(repeat.times, base)
        } else {
            base
        }
    }
}
