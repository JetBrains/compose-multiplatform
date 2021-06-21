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

package androidx.compose.animation.core.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [TransitionDetector].
 */
class TransitionDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = TransitionDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(TransitionDetector.UnusedTransitionTargetStateParameter)

    // Simplified Transition.kt stubs
    private val TransitionStub = compiledStub(
        filename = "Transition.kt",
        filepath = "androidx/compose/animation/core",
        """
            package androidx.compose.animation.core

            import androidx.compose.runtime.Composable

            class Transition<S> {
                class Segment<S>
            }

            @Composable
            inline fun <S> Transition<S>.animateFloat(
                noinline transitionSpec: @Composable Transition.Segment<S>.() -> Unit = {},
                label: String = "FloatAnimation",
                targetValueByState: @Composable (state: S) -> Float
            ): Float = 5f
        """,
"""
        androidx/compose/animation/core/TransitionKt.class:
        H4sIAAAAAAAAAMVVT1MjRRR/PZkknSHAMAsI2d24stldYGEnZDHqJovLIsho
        iJZBLhysJmmyA5MeamZCsRcLvfgFvOzN8uDRg6ctD1aKLS9+Jsvy9ZCEQKzC
        eDFV6fev3++91/36zR9//fobACzDJwQWmKh5rl07Matu48j1ucmE3WCB7QrU
        eNzc9pjwbSl/GsSBENAP2DEzHSbq5md7B7yK2giB5Lkb33BcFhD4Zrb074EL
        pUM3cGxhHhw3zP2mqEqlb260uaVC6SJkJfBsUb/WY26DwJ/FypPS1WQLK4Nk
        VtyuFFauC1ZcHAAxU+H1BhfBZeQvhR1IcdA6i4sI0+MVnj4CyfLv9WflNUVg
        N7i5Fspsz+EFAndLrlc3D3iw5zEbwZkQbsDOA5XdoNx0HNwVKwYvbH+FgkYg
        3ZOULQLuCeaYlpAZ+3bVj0OSwET1Ba8etv0/Zx5rcNxI4MFs/5X0lz23k4QR
        GNVgGHQCI0H3+CpHvErBIBB12B53KIwTMALm1Xmww5wmf/6ygrlzCpPqsx8A
        CIxl7Mx+5nJzEgudMrKeK4aFQZqWwJ3ruhDD9NdGYLw3aqbG91nTwejf/89v
        xuq/GdlHhUEmxKUDzSzF4R0C1CpXtlfLa+sEng5QYR9YIQl3IZOAGbh3uQf/
        oZg4PMC+CR1XOwEozBG43X/vXzVzy91LGOuc0hYPWI0FDO9LaRxHcF4SuUSx
        fQ4lo6D+xJZcFrnaEoHfW6c5rXWqKXo8JFNKh4R/Xeny4aaRrpUqqUBvnaaU
        LJlRaetUV+apoRrKppKN5DJU09VU2tCMji4maTaejZ79GFMoDddEjlJF1xBi
        KHdfT6ZmjBvG2KaCtiQdNigdMVRKZ0dDT9Lx3Dz7jr55TVqnZ98qcS1Kz17l
        skQWkyPy6ZCK7N/2cfQ29fJ/mHfolu5grZ8EHM2u6IBuvzySk2iys6E7L8pI
        0KAKpPjmffm2CQxfwD86xDtT19waqkdLtuDlZmOPe9tytsns3SpzdphnS7mt
        TFTsumBB00P+5hfnE9ESx7Zvo3n1YvgRyFy1dvO6tG0YR071cIsdtQMkLSG4
        t+Yw3+do1ipu06vyDVvaptuQO33hYAkUUEH+FJiGKMRQeobSEcpRpOl5Y+g1
        jD00buC6YEzgumi8hWskrxpTP4d+q7jG8Opuwig8Dz/xUeQjiJcKsdMo3Qpj
        pMGA27hTcuP4V0JuEnURWAux4vBRG40iXcf/tIpCQr6Eq6uegLfhDvIy4Z/Q
        OYY0P6GqX78C7Re434LZ0oQaRSlmzG9dW0gENnBVQRlJhCWlQrwhTEl+FoZg
        Ar8LU0gf95T5uKfMPDxsl5nvlpnvlplvlxmBj1HS0DoT7p2GzbDwD8FCuov6
        BcRd3IWIBY8sMHGFrIX3lLMw2jJu8OFdyO+C7mNrwns+vO/DLR8MHz7w4Umo
        oT4UfBgP+Ukfij489WHlb+6OIxeBCQAA
        """,
        """
        androidx/compose/animation/core/Transition＄Segment.class:
        H4sIAAAAAAAAAJVQW2sTQRT+zuwm2263dltvqbd6A2se3DYUBC0FLQiBVcGU
        vORpkh3iNLuzsjMpfcxv8R/4JPggwUd/lHh2LQj6ogPznfN95zJnzvcfX74C
        OMADQk+arCp1dp5MyuJDaVUijS6k06VhpVLJSSWN1TV/OFDTQhkXgAi7h4Nn
        6ak8k0kuzTR5Oz5VE/f86G+JEP+pBfAJ7UNttDsieLuPhxHaCEK0sELw3Xtt
        CQfp/0/Gj22ms9Ll2iSvlZOZdJI1UZx5/F+qoUWgGUvnumZ77GX7hEfLRRSK
        jgiXi1DEDMtFZ7no+isUU0/siZetbx/bIvbq9B53GBC3Q/ffJwzQIQQXYxLW
        f0eezJj7x2WmCBupNurNvBir6kSOc1a20nIi86GsdM0vxKhvjKqOc2mt4k2t
        DvTUSDevOBQOynk1Ua90nbf9bm6cLtRQW82FL4wpXTOdxT4Eb7s+/Jt6+Yy3
        mCUN5zV1P2P1EzsCtxnbjRjgDmP0KwEh1tj62GEMWRPYxg2+d5sqD/caexP3
        2T7leMQ16yN4fVzqY4MRcQ2bfWzh8ghkcQVXR/At1iyuWVy3CH4CEksXNqoC
        AAA=
        """,
        """
        androidx/compose/animation/core/TransitionKt＄animateFloat＄1.class:
        H4sIAAAAAAAAAKVVXW8bRRQ9s3ZsZ7Mljpt+JIUSGtM6dujGIbRQuwGT2mSJ
        ayocIqE8je2ps/F6ttpdW+EtD/0J/BAKEkUgIT/zo1DvrF03VkPbhJeZO3fu
        uffMnTO7//z7598ANlBlKHDZ8ly7dWQ23e4T1xcml3aXB7YryeMJc9fj0rfV
        eidID/dExXF5kM7HwRieVjtu4NjSPOx3TVsGwpPcMau822jxwsm9xz3ZVGl8
        szKy8sXqu1dP10W7K2RQ3K0XNseJf5B2QEuGxf9mEUeU4fqbmcQRY4gVbUq3
        yRDJrOwxRDPWyp6BBHQdU5ghR3Bg+wz3z8D6tZ4R1Zgt+25HMNzNnOP8BUWt
        eB7ksHMKvlx1vbZ5KIKGx21qA5fSDfiwJTU3qPUch3jqaXXetKRVAhcnWzhu
        sSUDj1LYTT+OSwyXmgei2RnleMQ93hUUyHArUz3kfW46XLbN7xqHohkUTnjq
        Kkm7oNp9BVd1XMYCw8Z5usNw85RSK6+7GNbPnj6ODwzMIqlDw4cMMydUGMdH
        DAmrVt8t1bbKDBcmJGpgGelp3MDHDNqTPEPqNEaJYtMJJahUN62KZBXwvWmy
        VhnmXqZ8KALe4gEniNbtR+gtMzVMMbCOMiLkP7KVtUZWi8rlBseGPjjWtaQW
        Tle15OB4UVtjN6KJwXFSyyZS0ZS2ra1FtnUFWafDFbl05U9dt+fTkwDlrjOs
        nkX6cRQZ5if03xKPec8JGH4+i4Lf9h05RUpvQ1iniKRiYBNf0tW9qny7Q1yj
        W25LqCtzm9zZ457NG47YVQPDbNWWotbrNoQ38kzX7bbkQc8jO/19TwZ2V1iy
        b/s2bY+fROnVk2MwLCmFt+Vw3xe0nC3LpuP6dAy66AO3RU+x7va8pqjYqsD8
        cPFANHrt8lEgiKorGRZGtfaGlU4UQJ70M0U3SB9sLChBkTKiZJPIaPyaVmmK
        oEtGLBt9DuOZUhS2aDSGXlwIMXNK+4iEiAIhNJrjudT8H1hUEA0PwmD6xBFQ
        wS8PQ0ZwZV3ENdovh9FzqCgfKQspIFmi7O+P+Hw1ym5kcwNc/x1Lv+LmLxMl
        MFHCGJcwcAuZ8Gwr49NdCWOAmb+g/fgcud/wybPQEcM3NOoUNgxYwHbYmvtE
        YMgxAiucS/iW5qf1h6VH+oQ89J1QaHq2vvTSqui5pfzSZNT/+GXo2aqeX86v
        5u/cI7usY4eY3CXOt+lSzX1ELKxZyNOIdQufYsPCZ7izD+ZT1Of7iPr4wsc9
        HwUf114AvnRqlvwHAAA=
        """,
        """
        androidx/compose/animation/core/Transition.class:
        H4sIAAAAAAAAAI1RTW8TMRB99m52023apuUr4atAeygRYtuIU6gqQSWkSAtI
        bJVLTk7WCm52vWjtVD3mt/APOCFxQBFHfhRivFRCgkt9eOP3PG88Hv/89e07
        gBfYY+gJnVWlyi7jaVl8Ko2MhVaFsKrUpFQyPquENsrxEIzh4DgdJOfiQsS5
        0LP4/eRcTu3Lk/8lhva/WgifIThWWtkTBu/g6aiFAGGEBpoMvv2oDMOz5Pod
        0SXbyby0udLxW2lFJqwgjRcXHr2POWgwsDlJl8qxQ9plR3TJarkZ8Q6PVsuI
        tx00eWe17PnN1bLN+vyQD5j/uvHjc8DbnvP0qUzKqCbCVM4KqS1D//qN7l+Z
        QnQZNv7qz+dUxz8tM8mwlSgt3y2KiazOxCQnZScppyIfiUo5fiW2hlrL6jQX
        xkga11qqZlrYRUVHUVouqql8o1xe98NCW1XIkTKKjK+0Lm3dm8EROI3cLXqV
        +wHC+8TimtPMel+x9oU2HA8Ig1oM8JCw9ScBEdYp+tgljEi7S7kddPGodnl4
        XMd7eEJxQOct8myM4Q2xOcQWIdoOtofYwY0xmMFN3BqjYbBucNvgjkFo0PkN
        zab+FKoCAAA=
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3Apc8ln5iXUpSfmVKhl5yfW5BfnKqX
        mJeZm1iSmZ8HFClKFeIJKUrMK84ECXiXcPFyMafl5wuxhaQWl3iXKDFoMQAA
        iDN6X1gAAAA=
        """
    )

    @Test
    fun unreferencedParameters() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*

                val transition = Transition<Boolean>()

                var foo = false

                @Composable
                fun Test() {
                    transition.animateFloat { if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { if (foo) 1f else 0f })
                    transition.animateFloat { param -> if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { param -> if (foo) 1f else 0f })
                    transition.animateFloat { _ -> if (foo) 1f else 0f }
                    transition.animateFloat(targetValueByState = { _ -> if (foo) 1f else 0f })
                }
            """
            ),
            TransitionStub,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { if (foo) 1f else 0f }
                                            ~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:14: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { if (foo) 1f else 0f })
                                                                 ~~~~~~~~~~~~~~~~~~~~~~~
src/foo/test.kt:15: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { param -> if (foo) 1f else 0f }
                                              ~~~~~
src/foo/test.kt:16: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { param -> if (foo) 1f else 0f })
                                                                   ~~~~~
src/foo/test.kt:17: Error: Target state parameter _ is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { _ -> if (foo) 1f else 0f }
                                              ~
src/foo/test.kt:18: Error: Target state parameter _ is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat(targetValueByState = { _ -> if (foo) 1f else 0f })
                                                                   ~
6 errors, 0 warnings
            """
            )
    }

    @Test
    fun unreferencedParameter_shadowedNames() {
        lint().files(
            kotlin(
                """
                package foo

                import androidx.compose.animation.core.*
                import androidx.compose.runtime.*

                val transition = Transition<Boolean>()

                var foo = false

                @Composable
                fun Test() {
                    transition.animateFloat {
                        foo.let {
                            // These `it`s refer to the `let`, not the `animateFloat`, so we
                            // should still report an error
                            it.let {
                                if (it) 1f else 0f
                            }
                        }
                    }
                    transition.animateFloat { param ->
                        foo.let { param ->
                            // This `param` refers to the `let`, not the `animateFloat`, so we
                            // should still report an error
                            if (param) 1f else 0f
                        }
                    }
                }
            """
            ),
            TransitionStub,
            Stubs.Composable
        )
            .run()
            .expect(
                """
src/foo/test.kt:13: Error: Target state parameter it is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat {
                                            ^
src/foo/test.kt:22: Error: Target state parameter param is not used [UnusedTransitionTargetStateParameter]
                    transition.animateFloat { param ->
                                              ~~~~~
2 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint().files(
            kotlin(
                """
            package foo

            import androidx.compose.animation.core.*
            import androidx.compose.runtime.*

            val transition = Transition<Boolean>()

            var foo = false

            @Composable
            fun Test() {
                transition.animateFloat { if (it) 1f else 0f }
                transition.animateFloat(targetValueByState = { if (it) 1f else 0f })
                transition.animateFloat { param -> if (param) 1f else 0f }
                transition.animateFloat(targetValueByState = { param -> if (param) 1f else 0f })
                transition.animateFloat { param ->
                    foo.let {
                        it.let {
                            if (param && it) 1f else 0f
                        }
                    }
                }
                transition.animateFloat {
                    foo.let { param ->
                        param.let { param ->
                            if (param && it) 1f else 0f
                        }
                    }
                }

                transition.animateFloat {
                    foo.run {
                        run {
                            if (this && it) 1f else 0f
                        }
                    }
                }

                fun multipleParameterLambda(lambda: (Boolean, Boolean) -> Float): Float
                    = lambda(true, true)

                transition.animateFloat {
                    multipleParameterLambda { _, _ ->
                        multipleParameterLambda { param1, _ ->
                            if (param1 && it) 1f else 0f
                        }
                    }
                }
            }
        """
            ),
            TransitionStub,
            Stubs.Composable
        )
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
