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

package androidx.build.lint

import androidx.build.lint.Stubs.Companion.RestrictTo
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class BanUncheckedReflectionTest : AbstractLintDetectorTest(
    useDetector = BanUncheckedReflection(),
    useIssues = listOf(BanUncheckedReflection.ISSUE),
    stubs = arrayOf(Stubs.ChecksSdkIntAtLeast),
) {

    @Test
    fun `Detection of unchecked reflection in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.app.ActivityRecreator"),
            RestrictTo
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/app/ActivityRecreator.java:262: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                        performStopActivity3ParamsMethod.invoke(activityThread,
                        ^
src/androidx/sample/core/app/ActivityRecreator.java:265: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                        performStopActivity2ParamsMethod.invoke(activityThread,
                        ^
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of unchecked reflection in real-world Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.sample.core.app.ActivityRecreatorKt"),
            RestrictTo
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/app/ActivityRecreatorKt.kt:177: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                        performStopActivity3ParamsMethod!!.invoke(
                        ^
src/androidx/sample/core/app/ActivityRecreatorKt.kt:182: Error: Calling Method.invoke without an SDK check [BanUncheckedReflection]
                        performStopActivity2ParamsMethod!!.invoke(
                        ^
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        lint()
            .files(*input)
            .run()
            .expect(expected)
    }

    @Test
    fun `Checked reflection in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.app.ActivityRecreatorChecked"),
            RestrictTo
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Checked reflection in real-world Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.sample.core.app.ActivityRecreatorKtChecked"),
            RestrictTo
        )

        check(*input).expectClean()
    }

    @Test
    fun `Checked reflection using preceding if with return`() {
        val input = kotlin("""
            package androidx.foo

            import android.os.Build

            fun forceEnablePlatformTracing() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) return
                if (Build.VERSION.SDK_INT >= 29) return
                val method = android.os.Trace::class.java.getMethod(
                    "setAppTracingAllowed",
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(null, true)
            }
        """.trimIndent())

        check(input).expectClean()
    }

    @Test
    fun `Checked reflection using @DeprecatedSinceApi method`() {
        val input = arrayOf(kotlin("""
            package androidx.foo

            import android.os.Build
            import androidx.annotation.DeprecatedSinceApi

            @DeprecatedSinceApi(29)
            fun forceEnablePlatformTracing() {
                val method = android.os.Trace::class.java.getMethod(
                    "setAppTracingAllowed",
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(null, true)
            }
        """.trimIndent()),
            Stubs.DeprecatedSinceApi
        )

        check(*input).expectClean()
    }

    @Test
    fun `Checked reflection using @DeprecatedSinceApi class`() {
        val input = arrayOf(java("""
            package androidx.foo;

            import android.os.Build;
            import androidx.annotation.DeprecatedSinceApi;

            public class OuterClass {
                public static void doCheckedReflection() {
                    if (Build.VERSION.SDK_INT < 29) {
                        PreApi29Impl.forceEnablePlatformTracing();
                    }
                }

                @DeprecatedSinceApi(29)
                static class PreApi29Impl {
                    public static void forceEnablePlatformTracing() {
                        android.os.Trace.class.getMethod(
                            "setAppTracingAllowed",
                            Boolean.class
                        ).invoke(null, true);
                    }
                }
            }
        """.trimIndent()),
            Stubs.DeprecatedSinceApi
        )

        check(*input).expectClean()
    }

    @Test
    fun `Checked reflection using Kotlin range check`() {
        val input = arrayOf(
            kotlin("""
                package androidx.foo

                import android.os.Build

                fun forceEnablePlatformTracing() {
                    if (Build.VERSION.SDK_INT in 18..28) {
                        val method = android.os.Trace::class.java.getMethod(
                            "setAppTracingAllowed",
                            Boolean::class.javaPrimitiveType
                        )
                        method.invoke(null, true)
                    }
                }
            """.trimIndent())
        )

        check(*input).expectClean()
    }
}
