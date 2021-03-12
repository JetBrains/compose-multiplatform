/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("UnstableApiUsage")

package androidx.compose.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
class ListIteratorDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = ListIteratorDetector()

    override fun getIssues(): MutableList<Issue> = mutableListOf(
        ListIteratorDetector.ISSUE
    )

    // These come from class files, so there is a notable difference vs defining them in source
    // in terms of our parsing.
    @Test
    fun stdlibIterableExtensions_calledOnList() {
        lint().files(
            kotlin(
                """
                package test

                val list = listOf(1, 2, 3)

                fun test() {
                    list.forEach {  }
                    list.forEachIndexed { _,_ -> }
                    list.map { }
                    list.mapIndexed { _,_ -> }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:7: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    list.forEach {  }
                         ~~~~~~~
src/test/test.kt:8: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    list.forEachIndexed { _,_ -> }
                         ~~~~~~~~~~~~~~
src/test/test.kt:9: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    list.map { }
                         ~~~
src/test/test.kt:10: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    list.mapIndexed { _,_ -> }
                         ~~~~~~~~~~
4 errors, 0 warnings
            """
            )
    }

    @Test
    fun userDefinedExtensions_calledOnList() {
        lint().files(
            kotlin(
                """
                package test

                val list = listOf(1, 2, 3)

                fun test() {
                    list.fancyForEach {  }
                    list.fancyDoSomething()
                }

                inline fun <T> Iterable<T>.fancyForEach(action: (T) -> Unit): Unit {}

                fun Iterable<*>.fancyDoSomething(): Boolean = true
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:7: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    list.fancyForEach {  }
                         ~~~~~~~~~~~~
src/test/test.kt:8: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    list.fancyDoSomething()
                         ~~~~~~~~~~~~~~~~
2 errors, 0 warnings
            """
            )
    }

    // These come from class files, so there is a notable difference vs defining them in source
    // in terms of our parsing.
    @Test
    fun stdlibIterableExtensions_calledOnNonList() {
        lint().files(
            kotlin(
                """
                package test

                val set = setOf(1, 2, 3)

                fun test() {
                    set.forEach {  }
                    set.forEachIndexed { _,_ -> }
                    set.map { }
                    set.mapIndexed { _,_ -> }
                }
            """
            )
        )
            .run()
            .expectClean()
    }

    @Test
    fun userDefinedExtensions_calledOnNonList() {
        lint().files(
            kotlin(
                """
                package test

                val set = setOf(1, 2, 3)

                fun test() {
                    set.fancyForEach {  }
                    set.fancyDoSomething()
                }

                inline fun <T> Iterable<T>.fancyForEach(action: (T) -> Unit): Unit {}

                fun Iterable<*>.fancyDoSomething(): Boolean = true
            """
            )
        )
            .run()
            .expectClean()
    }

    @Test
    fun inOperatorCalledOnList() {
        lint().files(
            kotlin(
                """
                package test

                val list = listOf(1, 2, 3)

                fun test() {
                    for (e in list) { }
                }
            """
            )
        )
            .run()
            .expect(
                """
src/test/test.kt:7: Error: Creating an unnecessary Iterator to iterate through a List [ListIterator]
                    for (e in list) { }
                           ~~
1 errors, 0 warnings
            """
            )
    }

    @Test
    fun inOperatorCalledOnNonList() {
        lint().files(
            kotlin(
                """
                val list = listOf(1, 2, 3)
                val set = setOf(1, 2, 3)

                fun test() {
                    for (i in list.indices) { }
                    for (e in set) { }
                }
            """
            )
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
