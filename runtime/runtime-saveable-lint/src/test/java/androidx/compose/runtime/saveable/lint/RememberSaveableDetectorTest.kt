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

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)
/**
 * Test for [RememberSaveableDetector].
 */
class RememberSaveableDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RememberSaveableDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RememberSaveableDetector.RememberSaveableSaverParameter)

    private val rememberSaveableStub: TestFile = compiledStub(
        filename = "RememberSaveable.kt",
        filepath = "androidx/compose/runtime/saveable",
        checksum = 0x90b6d5a7,
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
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcUlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsTnW1qSmJSTGlySWJLqXcJlzqWIS61ecWJZKkitkFBQam5qblJq
        UTBUBKiRl4ulJLW4RIjVLT/fu0SJQYsBAFB+NmSMAAAA
        """,
        """
        androidx/compose/runtime/saveable/RememberSaveableKt.class:
        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
        """,
        """
        androidx/compose/runtime/saveable/Saver.class:
        H4sIAAAAAAAAAI1PTUvDQBB9m/QjjVVT60f9BWIOphZBUBG8CJVKoQEvPW2b
        tWzbbCS7LT3md3mQnP1R4sTai3pwYd6bffOGmXn/eH0DcIEWwwlXUZrIaBWM
        k/gl0SJIF8rIWASaLwUfzUUQUpJWwRgebvqpnEjF51e9KV/yYM7VJOiPpmJs
        rsNv/x+l298Sg/dTq6LE0OjNEjOXKngUhkfccHJa8dKmfVkBZQY2I2kli1+b
        suicwc+zumu1LNdyKLw8c55beeaXnDzzmO84zLN8q213KIqODsNp75+H03xn
        czWlmysZ7YPmQMQiHol0o57NDEMtlBPFzSIlkxsmi3Qs7mXRcTxYT3iSWpL5
        TqnEcCMTpSu0FcpYPxsHhBbx4Rfv44j4kuZVyFMdwu7C6aJGCLeArS7q2B6C
        aexgd4iShqfR0NjTaH4C2V0QQe0BAAA=
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
            Stubs.Composable,
            Stubs.SnapshotState
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
            Stubs.Composable,
            Stubs.SnapshotState
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
