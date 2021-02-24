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
package androidx.compose.ui.text.benchmark

import android.graphics.Canvas
import android.util.Log
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import kotlin.random.Random

/**
 * Collection of text benchmark utilities. It tries to
 * - trigger garbage collection
 * - free text layout caches
 * before each test run.
 *
 * It also provides random text generation capabilities.
 *
 */
class TextBenchmarkTestRule(alphabet: Alphabet = Alphabet.Latin) : TestRule {
    private val textGeneratorTestRule = RandomTextGeneratorTestRule(alphabet)

    override fun apply(base: Statement, description: Description): Statement {
        return RuleChain
            .outerRule(GarbageCollectTestRule())
            .around(TextLayoutCacheTestRule())
            .around(textGeneratorTestRule)
            .apply(base, description)
    }

    fun <T> generator(block: (generator: RandomTextGenerator) -> T): T {
        return textGeneratorTestRule.generator(block)
    }

    // Width and fontSize used for Layout measurement should be the same for all text benchmarks.
    // It is helpful when we compare the performance of different layers.
    // Notice that different test cases accept different length units. The unit of width and
    // fontSize here are dp and sp, which should be converted into needed unit in the test case.
    val widthDp: Float = 160f
    val fontSizeSp: Float = 8f

    // We noticed that benchmark a single composable Text will lead to inaccurate result. To fix
    // this problem, we benchmark a column of Texts with its length equal to [repeatTimes].
    val repeatTimes: Int = 10
}

/**
 * At the beginning of each test calls Canvas.freeTextLayoutCaches in order to clear the native
 * text layout cache.
 */
private class TextLayoutCacheTestRule : TestRule {
    private val TAG = "TextLayoutCacheTestRule"

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                tryFreeTextLayoutCache()
                base.evaluate()
            }
        }

    fun tryFreeTextLayoutCache() {
        try {
            val freeCaches = Canvas::class.java.getDeclaredMethod("freeTextLayoutCaches")
            freeCaches.isAccessible = true
            freeCaches.invoke(null)
        } catch (e: Exception) {
            Log.w(TAG, "Cannot fre text layout cache", e)
            // ignore
        }
    }
}

/**
 * At the beginning of each test calls Runtime.getRuntime().gc() in order to free memory and
 * possibly prevent GC during measurement.
 */
private class GarbageCollectTestRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                Runtime.getRuntime().gc()
                base.evaluate()
            }
        }
}

/**
 * Test rule that initiates a [RandomTextGenerator] using a different seed based on the class and
 * function name. This way each function will have a different text generated, but at each run
 * the same function will get the same text.
 *
 * This will ensure that the execution order of a test class or functions in a test class does
 * not affect others because of the native text layout cache.
 */
private class RandomTextGeneratorTestRule(
    private val alphabet: Alphabet = Alphabet.Latin
) : TestRule {
    private lateinit var textGenerator: RandomTextGenerator

    override fun apply(base: Statement, description: Description): Statement =
        object : Statement() {
            override fun evaluate() {
                // gives the full class and function name including the parameters
                val fullName = "${description.className}#${description.methodName}"

                textGenerator = RandomTextGenerator(alphabet, Random(fullName.hashCode()))

                base.evaluate()
            }
        }

    fun <T> generator(block: (generator: RandomTextGenerator) -> T): T {
        return block(textGenerator)
    }
}