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

package androidx.compose.runtime.saveable.lint

import androidx.compose.lint.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

// TODO: add tests for methods defined in class files when we update Lint to support bytecode()
//  test files

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
/**
 * Test for [RememberSaveableDetector].
 */
class RememberSaveableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RememberSaveableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RememberSaveableDetector.RememberSaveableSaverParameter)

    private val rememberSaveableStub: TestFile = kotlin(
        """
        package androidx.compose.runtime.saveable

        import androidx.compose.runtime.*

        @Composable
        fun <T : Any> rememberSaveable(
            vararg inputs: Any?,
            saver: Saver<T, out Any> = autoSaver(),
            key: String? = null,
            init: () -> T
        ): T = init()

        @Composable
        fun <T> rememberSaveable(
            vararg inputs: Any?,
            stateSaver: Saver<T, out Any>,
            key: String? = null,
            init: () -> MutableState<T>
        ): MutableState<T> = rememberSaveable(
            *inputs,
            saver = mutableStateSaver(stateSaver),
            key = key,
            init = init
        )

        interface Saver<Original, Saveable : Any>

        @Suppress("UNCHECKED_CAST")
        private fun <T> autoSaver(): Saver<T, Any> =
            (Any() as Saver<T, Any>)

        @Suppress("UNCHECKED_CAST")
        private fun <T> mutableStateSaver(inner: Saver<T, out Any>) =
            Any() as Saver<MutableState<T>, MutableState<Any?>>
    """
    )

    @Test
    fun saverPassedToVarargs() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*
                import androidx.compose.runtime.saveable.*

                class Foo
                object FooSaver : Saver<Any, Any>
                class FooSaver2 : Saver<Any, Any>
                val fooSaver3 = object : Saver<Any, Any> {}
                val fooSaver4 = FooSaver2()

                @Composable
                fun Test() {
                    val foo = rememberSaveable(FooSaver) { Foo() }
                    val mutableStateFoo = rememberSaveable(FooSaver) { mutableStateOf(Foo()) }
                    val foo2 = rememberSaveable(FooSaver2()) { Foo() }
                    val mutableStateFoo2 = rememberSaveable(FooSaver2()) { mutableStateOf(Foo()) }
                    val foo3 = rememberSaveable(fooSaver3) { Foo() }
                    val mutableStateFoo3 = rememberSaveable(fooSaver3) { mutableStateOf(Foo()) }
                    val foo4 = rememberSaveable(fooSaver4) { Foo() }
                    val mutableStateFoo4 = rememberSaveable(fooSaver4) { mutableStateOf(Foo()) }
                }
            """
            ),
            rememberSaveableStub,
            kotlin(Stubs.Composable),
            kotlin(Stubs.MutableState)
        )
            .run()
            .expect(
                """
src/test/Foo.kt:15: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo = rememberSaveable(FooSaver) { Foo() }
                                               ~~~~~~~~
src/test/Foo.kt:16: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo = rememberSaveable(FooSaver) { mutableStateOf(Foo()) }
                                                           ~~~~~~~~
src/test/Foo.kt:17: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo2 = rememberSaveable(FooSaver2()) { Foo() }
                                                ~~~~~~~~~~~
src/test/Foo.kt:18: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo2 = rememberSaveable(FooSaver2()) { mutableStateOf(Foo()) }
                                                            ~~~~~~~~~~~
src/test/Foo.kt:19: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo3 = rememberSaveable(fooSaver3) { Foo() }
                                                ~~~~~~~~~
src/test/Foo.kt:20: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo3 = rememberSaveable(fooSaver3) { mutableStateOf(Foo()) }
                                                            ~~~~~~~~~
src/test/Foo.kt:21: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val foo4 = rememberSaveable(fooSaver4) { Foo() }
                                                ~~~~~~~~~
src/test/Foo.kt:22: Error: Passing Saver instance to vararg inputs [RememberSaveableSaverParameter]
                    val mutableStateFoo4 = rememberSaveable(fooSaver4) { mutableStateOf(Foo()) }
                                                            ~~~~~~~~~
8 errors, 0 warnings
            """
            )
            .expectFixDiffs(
                """
Fix for src/test/Foo.kt line 15: Change to `saver = FooSaver`:
@@ -15 +15
-                     val foo = rememberSaveable(FooSaver) { Foo() }
+                     val foo = rememberSaveable(saver = FooSaver) { Foo() }
Fix for src/test/Foo.kt line 16: Change to `stateSaver = FooSaver`:
@@ -16 +16
-                     val mutableStateFoo = rememberSaveable(FooSaver) { mutableStateOf(Foo()) }
+                     val mutableStateFoo = rememberSaveable(stateSaver = FooSaver) { mutableStateOf(Foo()) }
Fix for src/test/Foo.kt line 17: Change to `saver = FooSaver2()`:
@@ -17 +17
-                     val foo2 = rememberSaveable(FooSaver2()) { Foo() }
+                     val foo2 = rememberSaveable(saver = FooSaver2()) { Foo() }
Fix for src/test/Foo.kt line 18: Change to `stateSaver = FooSaver2()`:
@@ -18 +18
-                     val mutableStateFoo2 = rememberSaveable(FooSaver2()) { mutableStateOf(Foo()) }
+                     val mutableStateFoo2 = rememberSaveable(stateSaver = FooSaver2()) { mutableStateOf(Foo()) }
Fix for src/test/Foo.kt line 19: Change to `saver = fooSaver3`:
@@ -19 +19
-                     val foo3 = rememberSaveable(fooSaver3) { Foo() }
+                     val foo3 = rememberSaveable(saver = fooSaver3) { Foo() }
Fix for src/test/Foo.kt line 20: Change to `stateSaver = fooSaver3`:
@@ -20 +20
-                     val mutableStateFoo3 = rememberSaveable(fooSaver3) { mutableStateOf(Foo()) }
+                     val mutableStateFoo3 = rememberSaveable(stateSaver = fooSaver3) { mutableStateOf(Foo()) }
Fix for src/test/Foo.kt line 21: Change to `saver = fooSaver4`:
@@ -21 +21
-                     val foo4 = rememberSaveable(fooSaver4) { Foo() }
+                     val foo4 = rememberSaveable(saver = fooSaver4) { Foo() }
Fix for src/test/Foo.kt line 22: Change to `stateSaver = fooSaver4`:
@@ -22 +22
-                     val mutableStateFoo4 = rememberSaveable(fooSaver4) { mutableStateOf(Foo()) }
+                     val mutableStateFoo4 = rememberSaveable(stateSaver = fooSaver4) { mutableStateOf(Foo()) }
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*
                import androidx.compose.runtime.saveable.*

                class Foo
                object FooSaver : Saver<Any, Any>
                class FooSaver2 : Saver<Any, Any>
                val fooSaver3 = object : Saver<Any, Any> {}
                val fooSaver4 = FooSaver2()

                @Composable
                fun Test() {
                    val foo = rememberSaveable(saver = FooSaver) { Foo() }
                    val mutableStateFoo = rememberSaveable(stateSaver = FooSaver) {
                        mutableStateOf(Foo())
                    }
                    val foo2 = rememberSaveable(saver = FooSaver2()) { Foo() }
                    val mutableStateFoo2 = rememberSaveable(stateSaver = FooSaver2()) {
                        mutableStateOf(Foo())
                    }
                    val foo3 = rememberSaveable(saver = fooSaver3) { Foo() }
                    val mutableStateFoo3 = rememberSaveable(stateSaver = fooSaver3) {
                        mutableStateOf(Foo())
                    }
                    val foo4 = rememberSaveable(saver = fooSaver4) { Foo() }
                    val mutableStateFoo4 = rememberSaveable(stateSaver = fooSaver4) {
                        mutableStateOf(Foo())
                    }

                    val fooVarargs = rememberSaveable(Any(), FooSaver, Any()) { Foo() }
                    val mutableStateFooVarargs = rememberSaveable(Any(), FooSaver, Any()) {
                        mutableStateOf(Foo())
                    }
                }
            """
            ),
            rememberSaveableStub,
            kotlin(Stubs.Composable),
            kotlin(Stubs.MutableState)
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
