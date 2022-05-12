/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.foundation.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LazyLayoutStateReadInCompositionDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = LazyLayoutStateReadInCompositionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(
            LazyLayoutStateReadInCompositionDetector.FrequentlyChangedStateReadInComposition
        )

    private val lazyGridStateStub = compiledStub(
        filename = "LazyGridState.kt",
        filepath = "androidx/compose/foundation/lazy/grid",
        checksum = 0xd5891ae4,
        source = """
                    package androidx.compose.foundation.lazy.grid

                    interface LazyGridLayoutInfo {

                    }

                    class LazyGridState {
                        val firstVisibleItemIndex: Int get() = 0
                        val firstVisibleItemScrollOffset: Int get() = 0
                        val layoutInfo: LazyGridLayoutInfo get() = object : LazyGridLayoutInfo {}
                    }
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGI2Bijg0uSST8xLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczPAzLzhdhCUotLvEuUGLQYAJ1f4slDAAAA
        """,
        """
        androidx/compose/foundation/lazy/grid/LazyGridLayoutInfo.class:
        H4sIAAAAAAAAAJ2OzU7CQBSFzx2UYv0rKgm+hAPEhdGVG02TGhNJ3LAa2ikZ
        KDOGTgm44rlcGNY+lPEW4ws4Nznz3bmTc8/X98cngGt0CDfKZgtnspVM3fzN
        lVrmrrKZ8sZZWaj3tZwsTCYTpkeGRK1d5WObuwBEiKZqqfibncjn8VSnPkCD
        0E5mzhfGyiftFVupO4KYLxu8k2pp1gICzfh9Zequx5T1CZ3tphWKrghFxJR3
        t5uB6FE9HBBuk/+G5QC8L/obDL3y+mrmCeHQVYtUP5hCEy5fKuvNXL+a0owL
        fW+t8zvvsskJsIffI3C+0zNc8N1n432u5giNGEGMVowDhIw4jHGE4xGoxAlO
        RxAlohLtH5JfRp1/AQAA
        """,
        """
        androidx/compose/foundation/lazy/grid/LazyGridState＄layoutInfo＄1.class:
        H4sIAAAAAAAAAKVSTW/TQBB946R1a1zSlo8mfBcCanvATeGAoEJABciSAYlW
        ueS0sbfpNs4ustdVyyl/iRMSB5QzPwoxGxAVx5bLzJu3M7Oz++bHz2/fATzG
        A8ILobPCqOw4Ss3okylltG8qnQmrjI5y8fkkGhQqixJGbxnsWmFlOxcnprKx
        3jftjg8iLB6KI8HpehB96B/K1PqoEZ6crXfyt6uPGcLsttLKPifU1ta7IXzM
        BahjnlC3B6okvEr+d/RnhKVkaGyudPROWsGlgjlvdFTj7yFnZp0BgYbMHysX
        bTLKOoSVyXg+mIwDr+lt0GQ8FzQn4y1vk9zxFuHpGcc7fT2P8OgcT/PRIiwM
        pD3tRNheWz//HCGu4waL+881D4eWJdgxmSQ0EqXl+2rUl8We6OfMLCcmFXlX
        FMrFf8gw1loWO7koS8nCNV7rNDel0gP+9AOTEYJdUxWpfKNcdutjpa0aya4q
        FZe/1NrY6bAlOvB4BXg1pprA7QT724wipxH7mY2vCL4w8HDHyTclL2CVbfg7
        gaOQfR132QbMeU5htHBvyl7DTbSn9bdwn32HMxa46mIPtRiNGIsxlrDMEJdi
        XMaVHqjEVaz04JUONn8B8d9xtF4DAAA=
        """,
        """
        androidx/compose/foundation/lazy/grid/LazyGridState.class:
        H4sIAAAAAAAAAKVTS08UQRD+evbJ7CKzKLo8FBVUQGUAJSFKjEqCmWSEBMwm
        htPsbu/a7GyPme4l4Inf4tmDnkg0MRuP/ihj9bDhJQfFOdTj6/qqaqq6f/76
        +h3AY8wyPApkPY5EfdetRe33keJuI+rIeqBFJN0w+LDnNmNRd32yXpGxqQPN
        c2AMznawE1CEbLrr1W1e0zmkGLLLQgr9jCE1NV0pIoOsjTRyDGn9TiiGRf8C
        9Z4ylJtcr4pY6YpQohpyT/O2J+t8N6nkMYyfE7BZi6MwXG80FNcM/RThB3tR
        R3uyETEsT03/YzPHbOpowo/iprvNdTUOhFRuIGWkE6Zy1yK91glDinp+gd+d
        DI/qTM7nULIxaCa4dNFmc7jCUPJbkQ6FdF9zHRApoOas9k6K7gEzImsEGFiL
        8F1hvDmy6vMMre7+mG2VLdtyuvu2lTdGnnTmEMxb5e7+gjXHXmZ+fMxajrVR
        clIj1lx6aZX87Eg6n3GyhOVOYfkE6yPMPsIKTtGUXGCmkaHG+eseO4uf3rId
        nljxk/9ZsHNqKbMtSp5eieqcYcAXkq912lUevwmoC4ZBP6oFYSWIhfF74OhG
        R2rR5p7cOez2xfEVYSh6UvJ4JQyU4uTam1EnrvFVYZjDPWblDx7mYdGDMl+a
        hkTvi+Q98lyzO9KZmQPkv5BhYcqsNQHzmCZZPAxAH2zSJRQSxJAf9MhW6tMZ
        Zt8JpnXE7P8Lpn0u8xIGyDPMRdLmrPANg28PcLmLoc9nUhROpCj0Usz0Th3S
        KdxPCtH8k4kMUzsmw108JL1B+FX63WtbSHkoexj2MIJRMjHm4TpubIEpjOPm
        FvoVbIVbClmF24lRUCgqTChzNKlwR+GSwsBv73s6jzkFAAA=
        """
    )

    private val lazyListStateStub = compiledStub(
        filename = "LazyListState.kt",
        filepath = "androidx/compose/foundation/lazy",
        checksum = 0xb9a80c68,
        source = """
                    package androidx.compose.foundation.lazy

                    interface LazyListLayoutInfo {

                    }

                    class LazyListState {
                        val firstVisibleItemIndex: Int get() = 0
                        val firstVisibleItemScrollOffset: Int get() = 0
                        val layoutInfo: LazyListLayoutInfo get() = object : LazyListLayoutInfo {}
                    }
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGI2Bijg0uSST8xLKcrPTKnQS87PLcgvTtVL
        yy/NS0ksyczPAzLzhdhCUotLvEuUGLQYAJ1f4slDAAAA
        """,
        """
        androidx/compose/foundation/lazy/LazyListLayoutInfo.class:
        H4sIAAAAAAAAAJ1OTU/CQBB9s9WC9auoJPgnLKCePHkxaVJjIokXTku7NQtl
        17BbAp74XR4MZ3+UcYrxDziTvHnzkffm6/vjE8ANuoRraYqF1cUqye38zTqV
        lLY2hfTamqSS7+skY8i085lc29qnprQtECGeyqXkC/OaPE2mKvctBIRONrO+
        0iZ5VF6yirwjiPkyYDtqIGwABJrxfKWbrs+sGBC62007Ej0RiZhZ2dtuhqJP
        zXJIuM3+8Sd7s1X8txh56dXVzBOika0XuXrQlSJcPtfG67l60U5PKnVvjPU7
        WReyOfbwGwLnOzzDBdcBC+9zhmMEKVop2ikOEDHFYYojHI9BDic4HUM4xA6d
        H5nueFN1AQAA
        """,
        """
        androidx/compose/foundation/lazy/LazyListState＄layoutInfo＄1.class:
        H4sIAAAAAAAAAKVSS2/TQBD+xknr1rikLY8mvKEpanuomwLiUEBCFUiWDJUo
        yiWnjb1Nt3F2kb2uWk75S5yQOKCc+VGI2YB43Apcvpn5PK+dz1++fvoM4CHu
        E3aFzgqjstMoNaN3ppTRoal0JqwyOsrF+7MoYUhUaQ+ssLKdizNT2VgfmnbH
        BxEWj8WJ4Ew9iPb7xzK1PmqEB+dum/xs6GOGMPtEaWWfEWrrG90QPuYC1DFP
        qNsjVRKeJv+x8C5hKRkamysdvZJWcJVgzhud1Pge5GDWAQg0ZP5UuWibvaxD
        WJmM54PJOPCa3iZNxnNBczLe8bbJfd4hPDr/Zr/ezNO3/u5BPlqEhYH8rQnh
        8frGP00PcR03WMM/JmwNLZ97z2SS0EiUlq+rUV8Wb0U/Z2Y5ManIu6JQLv5B
        hrHWstjLRVlKFqnxQqe5KZUe8JWPTEYIDkxVpPKlctmtN5W2aiS7qlRc/lxr
        Y6d7lujAY7n5N5iKAKc/29vsRU4UtjObHxF8YMfDHafXlLyAu4zh9wSOQrZ1
        3GMMmPOcpGhhdcpew020p/W3sMa2wxkLXHWxh1qMRozFGEtYZheXYlzGlR6o
        xFWs9OCVzm1+A4nVJzVAAwAA
        """,
        """
        androidx/compose/foundation/lazy/LazyListState.class:
        H4sIAAAAAAAAAJ1TS08UQRD+evbJ7AKzKLo8FBVUQGUWlGiCMVESkkkGSMBs
        YjjN7vZis7M9ZrqXgCd+i2cPeiLRxGw8+qOM1cOGlxyAOdTj6/qqqqum//z9
        8QvAC8wxzAWyEUeisefWo/anSHG3GXVkI9Aikm4YfN53fRK+UHpTB5rnwBic
        nWA3oEO57a7Xdnhd55BiyL4WUug3DKnpmWoRGWRtpJFjSOuPQjFU/KuVWmIo
        b3O9ImKlq0KJWsg9zduebPC9pIjHMHFBwGY9jsJwvdlUXDP0U4Qf7Ecd7clm
        xPByeubyfZwQqZlJP4q33R2ua3EgpHIDKSOdkJS7Fum1ThhS1NLVLjkVHpeY
        ms+hZGPIjOz5NVrM4SZDyW9FOhTSXeU6oPiAWrLauylaNzMiawQYWIvwPWG8
        ClmNeYZW92DctsqWbTndA9vKGyNPOnME5q1y92DBqrB3md9fspZjbZSc1KhV
        Sb9aIT87ms5nnCxhuTNYPsH6CLOPsYJTNCUXmGlkuHnxfsfP42fXaoendrp4
        zY06Z1Yx16K86eWowRkGfSH5Wqdd4/H7gBpgGPKjehBWg1gYvweObXSkFm3u
        yd2jRt+e/BMMRU9KHi+HgVKcXHsz6sR1viIMc6THrP7HwzwsejfmS9N86BmR
        fEyea9ZGOjN7iPx3MixMm40mYB4zJItHAeiDTbqEQoIY8tMe2Up9PcfsO8W0
        jpn9l2DaFzIHMEieYS6SNmeFnxj6cIgbXQx/O5eicCpFoZditnfqkE7hSVKI
        5p9MZITaMRke4RnpDcJv0XVvbyHloexhxMMoxsjEuIc7uLsFpjCBe1voV7AV
        7itkFR4kRkGhqDCpzNGUwkOFAYXBf29R2u4bBQAA
        """
    )

    @Test
    fun observablePropertiesUsedInComposableFunction() {
        lint().files(
            kotlin(
                """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                @Composable
                fun TestGrid(state: LazyGridState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }

                @Composable
                fun TestList(state: LazyListState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }
            """
            ),
            lazyGridStateStub,
            lazyListStateStub,
            Stubs.Composable
        )
            .run()
            .expect("""
src/androidx/compose/foundation/foo/test.kt:10: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = state.firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:11: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = state.firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:12: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = state.layoutInfo
                                     ~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:17: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = state.firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:18: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = state.firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:19: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = state.layoutInfo
                                     ~~~~~~~~~~~~~~~~
0 errors, 6 warnings
            """)
            .expectFixDiffs("""
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Wrap with derivedStateOf:
@@ -10 +10
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Collect with snapshotFlow:
@@ -10 +10
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Wrap with derivedStateOf:
@@ -11 +11
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Collect with snapshotFlow:
@@ -11 +11
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Wrap with derivedStateOf:
@@ -12 +12
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Collect with snapshotFlow:
@@ -12 +12
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -14 +16
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Wrap with derivedStateOf:
@@ -17 +17
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Collect with snapshotFlow:
@@ -17 +17
-                     val index = state.firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Wrap with derivedStateOf:
@@ -18 +18
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Collect with snapshotFlow:
@@ -18 +18
-                     val offset = state.firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Wrap with derivedStateOf:
@@ -19 +19
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Collect with snapshotFlow:
@@ -19 +19
-                     val layoutInfo = state.layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -21 +23
+                 }
            """.trimIndent())
    }

    @Test
    fun observablePropertiesUsedInNonComposableFunction() {
        lint().files(
            kotlin(
                """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                fun testGrid(state: LazyGridState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }

                fun testList(state: LazyListState) {
                    val index = state.firstVisibleItemIndex
                    val offset = state.firstVisibleItemScrollOffset
                    val layoutInfo = state.layoutInfo
                }
            """
            ),
            lazyGridStateStub,
            lazyListStateStub,
            Stubs.Composable,
        )
            .run()
            .expectClean()
    }

    @Test
    fun observablePropertiesUsedInComposableFunctionWithReceiver() {
        lint().files(
            kotlin(
                """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                @Composable
                fun LazyGridState.TestGrid() {
                    val index = firstVisibleItemIndex
                    val offset = firstVisibleItemScrollOffset
                    val layoutInfo = layoutInfo
                }

                @Composable
                fun LazyListState.TestList() {
                    val index = firstVisibleItemIndex
                    val offset = firstVisibleItemScrollOffset
                    val layoutInfo = layoutInfo
                }
            """
            ),
            lazyGridStateStub,
            lazyListStateStub,
            Stubs.Composable,
        )
            .run()
            .expect("""
src/androidx/compose/foundation/foo/test.kt:10: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:11: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:12: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = layoutInfo
                                     ~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:17: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val index = firstVisibleItemIndex
                                ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:18: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val offset = firstVisibleItemScrollOffset
                                 ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:19: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                    val layoutInfo = layoutInfo
                                     ~~~~~~~~~~
0 errors, 6 warnings
            """.trimIndent()
            )
            .expectFixDiffs("""
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Wrap with derivedStateOf:
@@ -10 +10
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 10: Collect with snapshotFlow:
@@ -10 +10
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Wrap with derivedStateOf:
@@ -11 +11
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 11: Collect with snapshotFlow:
@@ -11 +11
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Wrap with derivedStateOf:
@@ -12 +12
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 12: Collect with snapshotFlow:
@@ -12 +12
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -14 +16
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Wrap with derivedStateOf:
@@ -17 +17
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Collect with snapshotFlow:
@@ -17 +17
-                     val index = firstVisibleItemIndex
+                     val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Wrap with derivedStateOf:
@@ -18 +18
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Collect with snapshotFlow:
@@ -18 +18
-                     val offset = firstVisibleItemScrollOffset
+                     val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Wrap with derivedStateOf:
@@ -19 +19
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Collect with snapshotFlow:
@@ -19 +19
-                     val layoutInfo = layoutInfo
+                     val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
@@ -21 +23
+                 }
            """.trimIndent())
    }

    @Test
    fun observablePropertiesUsedInComposableLambda() {
        lint().files(
            kotlin(
                """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                fun setContent(content: @Composable () -> Unit) {
                    // no-op
                }

                fun testGrid() {
                    val state = LazyGridState()
                    setContent {
                        val index = state.firstVisibleItemIndex
                        val offset = state.firstVisibleItemScrollOffset
                        val layoutInfo = state.layoutInfo
                    }
                }

                fun testList() {
                     val state = LazyListState()
                    setContent {
                        val index = state.firstVisibleItemIndex
                        val offset = state.firstVisibleItemScrollOffset
                        val layoutInfo = state.layoutInfo
                    }
                }
            """
            ),
            lazyGridStateStub,
            lazyListStateStub,
            Stubs.Composable,
        )
            .run()
            .expect("""
src/androidx/compose/foundation/foo/test.kt:15: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = state.firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:16: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = state.firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:17: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = state.layoutInfo
                                         ~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:24: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = state.firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:25: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = state.firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:26: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = state.layoutInfo
                                         ~~~~~~~~~~~~~~~~
0 errors, 6 warnings
            """.trimIndent())
            .expectFixDiffs("""
Fix for src/androidx/compose/foundation/foo/test.kt line 15: Wrap with derivedStateOf:
@@ -15 +15
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 15: Collect with snapshotFlow:
@@ -15 +15
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 16: Wrap with derivedStateOf:
@@ -16 +16
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 16: Collect with snapshotFlow:
@@ -16 +16
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Wrap with derivedStateOf:
@@ -17 +17
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 17: Collect with snapshotFlow:
@@ -17 +17
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 24: Wrap with derivedStateOf:
@@ -24 +24
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 24: Collect with snapshotFlow:
@@ -24 +24
-                         val index = state.firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 25: Wrap with derivedStateOf:
@@ -25 +25
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 25: Collect with snapshotFlow:
@@ -25 +25
-                         val offset = state.firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Wrap with derivedStateOf:
@@ -26 +26
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { state.layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Collect with snapshotFlow:
@@ -26 +26
-                         val layoutInfo = state.layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(state) {
+                     androidx.compose.runtime.snapshotFlow { state.layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
            """.trimIndent())
    }

    @Test
    fun observablePropertiesUsedInComposableLambdaWithReceiver() {
        lint().files(
            kotlin(
                """
                package androidx.compose.foundation.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.foundation.lazy.grid.LazyGridState
                import androidx.compose.foundation.lazy.LazyListState

                fun setListContent(content: @Composable LazyListState.() -> Unit) {
                    // no-op
                }

                fun setGridContent(content: @Composable LazyGridState.() -> Unit) {
                    // no-op
                }

                fun testGrid() {
                    setGridContent {
                        val index = firstVisibleItemIndex
                        val offset = firstVisibleItemScrollOffset
                        val layoutInfo = layoutInfo
                    }
                }

                fun testList() {
                    setListContent {
                        val index = firstVisibleItemIndex
                        val offset = firstVisibleItemScrollOffset
                        val layoutInfo = layoutInfo
                    }
                }
            """
            ),
            lazyGridStateStub,
            lazyListStateStub,
            Stubs.Composable,
        )
            .run()
            .expect("""
src/androidx/compose/foundation/foo/test.kt:18: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:19: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:20: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = layoutInfo
                                         ~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:26: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val index = firstVisibleItemIndex
                                    ~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:27: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val offset = firstVisibleItemScrollOffset
                                     ~~~~~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/compose/foundation/foo/test.kt:28: Warning: Frequently changing state should not be directly read in composable function [FrequentlyChangedStateReadInComposition]
                        val layoutInfo = layoutInfo
                                         ~~~~~~~~~~
0 errors, 6 warnings
            """.trimIndent())
            .expectFixDiffs("""
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Wrap with derivedStateOf:
@@ -18 +18
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 18: Collect with snapshotFlow:
@@ -18 +18
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Wrap with derivedStateOf:
@@ -19 +19
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 19: Collect with snapshotFlow:
@@ -19 +19
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 20: Wrap with derivedStateOf:
@@ -20 +20
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 20: Collect with snapshotFlow:
@@ -20 +20
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Wrap with derivedStateOf:
@@ -26 +26
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemIndex } }
Fix for src/androidx/compose/foundation/foo/test.kt line 26: Collect with snapshotFlow:
@@ -26 +26
-                         val index = firstVisibleItemIndex
+                         val index = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemIndex }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 27: Wrap with derivedStateOf:
@@ -27 +27
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { firstVisibleItemScrollOffset } }
Fix for src/androidx/compose/foundation/foo/test.kt line 27: Collect with snapshotFlow:
@@ -27 +27
-                         val offset = firstVisibleItemScrollOffset
+                         val offset = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { firstVisibleItemScrollOffset }
+                         .collect { TODO("Collect the state") }
+                 }
Fix for src/androidx/compose/foundation/foo/test.kt line 28: Wrap with derivedStateOf:
@@ -28 +28
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.remember { androidx.compose.runtime.derivedStateOf { layoutInfo } }
Fix for src/androidx/compose/foundation/foo/test.kt line 28: Collect with snapshotFlow:
@@ -28 +28
-                         val layoutInfo = layoutInfo
+                         val layoutInfo = androidx.compose.runtime.LaunchedEffect(this) {
+                     androidx.compose.runtime.snapshotFlow { layoutInfo }
+                         .collect { TODO("Collect the state") }
+                 }
            """.trimIndent())
    }
}