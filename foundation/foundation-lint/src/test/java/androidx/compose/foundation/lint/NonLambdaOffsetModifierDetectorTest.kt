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
import androidx.compose.lint.test.kotlinAndCompiledStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NonLambdaOffsetModifierDetectorTest : LintDetectorTest() {

    private val WarningMessage =
        "Warning: ${NonLambdaOffsetModifierDetector.ReportMainMessage} " +
            "[${NonLambdaOffsetModifierDetector.IssueId}]"

    private val OffsetStub: TestFile = compiledStub(
        filename = "Offset.kt",
        filepath = "androidx/compose/foundation/layout",
        checksum = 0xd449361a,
        source = """
        package androidx.compose.foundation.layout

        import androidx.compose.ui.Modifier
        import androidx.compose.ui.unit.Dp
        import androidx.compose.ui.unit.dp
        import androidx.compose.ui.unit.IntOffset
        import androidx.compose.ui.unit.Density

        fun Modifier.offset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this.then(Modifier)
        fun Modifier.absoluteOffset(x: Dp = 0.dp, y: Dp = 0.dp): Modifier = this.then(Modifier)
        fun Modifier.offset(offset: Density.() -> IntOffset): Modifier = this.then(Modifier)
        fun Modifier.absoluteOffset(offset: Density.() -> IntOffset): Modifier = this.then(Modifier)

        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3ApcellJiXUpSfmVKhl5yfW5BfnKqX
                ll+al5JYkpmfp5eTWJlfWiLE4Z+WVpxa4l3CpcAlgaG+NFOvNC+zRIjFpcC7
                RIlBiwEAXUlt+WoAAAA=
                """,
        """
                androidx/compose/foundation/layout/OffsetKt.class:
                H4sIAAAAAAAAALVW3U8jVRT/3en3tOyW8rXAwtZtdaGwTAcXVimiyIdWWkC7
                ITFEk2k7LQPtTNOZIfBi9skX/wDjqy++GhPjuiaG+Ogftdlzpx+UD9sVtMnc
                e+6555z7O79z53T+fvXyTwBP8DnDjKIX64ZWPJEKRrVmmKpUMmy9qFiaoUsV
                5dSwLWmnVDJVa8vygTGED5VjhXb0srSTP1QLpHUxeA3HhuFkKnMloq1JWaOo
                lTS1nrp219Y1S1qvdd+c7h6YIZYx6mXpULXydUXTTUnRdcNyEjGlbcPatisV
                svIuWweaueKHn2HyyLAqmi4dHlclTbfUuq5UpLRu1cldK5g+iAxDhQO1cNT0
                31XqSlUlQ4ZHU5nLTKQ6NDkepJya3gshhD4RQdxhYCd+hGk69SPCcL9bPj4M
                MgTWaEPRKQOG7rzG25apEIYxEsAQ7jG4rQOVfFd6FKUHtSGMYTxIEe8zhOKc
                vnir3JO9ijLRtaYMdxqR4kW1pNgVivjd/3aB0lcL1vNOjXcJ6EOMXyeNVitE
                9dQmL/bbeEdEHI9C8MArQsA0gxjX4qV4WbXWa1T8NEOwQWKxFteOiQIlbxoV
                21J3mqQONrYvq4cvKlqUhTDXOElm+LoXeR0XvmTrhcbLsdmU5N58/HS7A5Yf
                d6mQSi+dddqlhvRmNlJPrfRC6oeHIdorXYb+lklWtRTqegrphOqxi9oj44OP
                CnbEBYH0JxqXkiQVietfz55HxbPnonBPaE2dT3tnLBamQUiyBD3zI2FhbCDi
                jghJtzN6kq6/fvQKfu/YZKeZ3y+EfbTyO1KASxRHeKMwQrcoHPw8Q6SVdycZ
                D3sXhyH+JvXhjaF5wMaJxX0NvXXSs9OaSgaBhuncEd1s95pRVBnuZjRd3bar
                ebX+TMlXVA7TKCiVPaWu8XVTOf6FrVtaVU3rx5qpkWr1vNETvsu77Z59wawv
                ZymFo6xSawYN5LSyrlh2nWQxZ9j1grqp8Y3BxmJdzdvldi4Mo81j9q5AgEzv
                ohv858UobwNw4RNafUV6foEeJCKBF7g7E+mncTYyQGPiF4yeYeh3TAj4md82
                fOq4EwMQkSY52nDFJD1wpDDpmCNFSBLwWdPDR/MWPQOu5qJjDAfwFh6SzPF8
                TyH5zsKE+5sfMOha9LgWvX8g/uWQ59vf6P9mwnOdOpuYmX2BBEfpQoZGN4SR
                YQfvMCULwhsibCM0x2iea+JPkF2I5BnMEsoA+vCYJA/ZtHTRtm6Ud7TbkRa8
                QJp0W9KS/wlp8/9MWpDI6SfSgkRIP6UfvAFpHrxL9hxkljITaB5tkbbQlS7+
                iZJ2oDhO7TvGQy7SftaxfnKFrD7hAlm8bDdG0HcBgXQzBC5sO4ab2CEVy2VX
                d8V2mxG3nIYkJnLRlrQpzkTl6LnFv/gWJs/56HrtWq/zTwSyS2REOSbPygtL
                srgQm1+SFxvT03MgTm+5HRp+SjIm8+iyzGcKvyFil+hIEzlPicf39uFK4/00
                lmhEKo1lfJDGCj7cBzPxEVb3IZqYM/HAhMfExybWTKyb2DARNhE1EXkN1SFU
                dTgMAAA=
                """
    )

    // common_typos_disable
    private val AnotherOffsetDefinitionStub = kotlinAndCompiledStub(
        filename = "InitialTestPackage.kt",
        filepath = "initial/test/pack",
        checksum = 0xd4dfae47,
        source = """
            package initial.test.pack

            class OffsetClass {
                fun offset(x: Int, y: Int): Int {
                    return x + y
                }

                fun absoluteOffset(x: Int, y: Int): Int {
                    return x + y
                }
            }

            data class AnotherClass(val property: Int)

            fun AnotherClass.offset(x: Int, y: Int): Int {
                return x + y
            }

            fun AnotherClass.absoluteOffset(x: Int, y: Int): Int {
                return x + y
            }

        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3AJc0lmJiXX5KRWqRXklpcoleQmJwt
                xBYCZHqXcGlyCWbmZZZkJuYgSYp4QoRAagKAAonpqd4lSgxaDACYtrgJYAAA
                AA==
                """,
        """
                initial/test/pack/AnotherClass.class:
                H4sIAAAAAAAAAIVU308cVRT+7sz+mB0WmIVCKay0yorL0nYAW62FooBWBgFJ
                aYgVXy674zIwzKwzd4m+GJ76JzTRFxNjfOKhJgrGJg1t3/ybjPHcnemCC4Fk
                5txzz5zzne+ce+78/e9fzwHcwjLDoOM5wuGuKexQmDVe3jZnPF9s2sGcy8Mw
                DcZgbPFdbrrcq5qfb2zZZZGGypCakqHTDImiNbLGoBZH1rJIIq0jAY1BqwV+
                zQ7EdwzMykJHWwYKsuQvNp2Q4dri+aknGdqqtlhpolACi0Ev+zs137M9MU5Q
                Zb9GX4aIwcVoQ4t+UDW3bLERcMcLTe7Rdy4cn/RlXyzXXXdSFpDSiWcvQ1aC
                Fyr217zuCoa14kUpLGuxtVOTF/LKohuXZMZ+apnwV0XgeFWGS8WRE2CRlWq4
                3GqbrTtuxQ7SGNRxVba99yR68fUZ3NPwJh0Zr9Vsr8Jwo3ga/HS+GJsoDqEg
                4d9myMtWn+f4jnQsSse58x1L0nE0izzekNoNKn+Th5tzfsVmyB1HWp6wq7LC
                sWjUaJZMTOgYx7tUkf1Nnbs0TT3FM3r/JUPhvEOnE+cbrk19TTZ6xtB1GoXI
                LG77wnU8c8kWvMIFJ5uys6vSHWJSpGnCt8n0rSN3RFOp0Gz+crQ3qCt9iq4Y
                R3s6PYqh6YqWorWNVpXWDu3lY63vaG9CGWOz7V0pQ+lXxtSXP6cUI7GQMdJy
                N//qsbrQbWikk6OmKZETmRmZM6TrE5rR1p/oY2Ns/tUTlQKzkccTRno76R1S
                f5BrwmtEpz+hJY2U5DrBqA70WNGcPqQxXaEp5VX75jaNfSI6kE6aY3u5vrNh
                Bw9lz2Sr/DJ313jgyH1sHHhQ94SzY1verhM6ZJo57jdD+6og5CVei70Lrd4r
                POA7trCD/4Xpq349KNv3HRlzJY5ZO4VPE6HQr0cW0yV/N6RppNOFJnmfdtP0
                XaFVLx0iUxr4A+2/0U7BpySlD9CBeZK9kRftOoGGJtFoDmDQG2GZtMqIZOl3
                tO+fCZONHGKYHJF6HTzUGszODKAfA8HKgHGoDU6ZZ1AeDRzi8tNmUEQ20ySb
                iclaMZsewMigD1fi3MNxk3L5xPc/QJMMpkoDBxiIIBdIqmASgS5mnP4urZJa
                /hmuPjrEta63DjAsIw8wYowc4PoBbj5tKSMfMzrBg6TZ7MFw3IMGgz9xq7UN
                WhzPcBvvxTy+olVesUJp9FckE/ujL6D8iKS6P3oEZUkCXaf3J2lJRGey0Dg+
                Na39g1ya9scdKzQ7VsAdfEB5PouvMt5vhC425CdYovULst6lk5lch2physI9
                kpiW4kMLH2FmHSzELObW0RnK5+MQekOmQhghciG6QnSHuN0w3glhhsiT/h8X
                5RN2iwcAAA==
                """,
        """
                initial/test/pack/InitialTestPackageKt.class:
                H4sIAAAAAAAAAJVRXU8TQRQ9M4W2LghtbZUWRJQCbR9cIL5VTQiJcUMFIqQv
                fXG6Hdppt7tmZ9rgGz/IH2B8MDz7o4x3aFWEGHWSuffcc7/mzv367fMXAM9Q
                Y9hUoTJKBK6R2rjvhT9wvQlzSsQx2aIrD0wKjCHTF2PhBiLsukftvvSJTTAk
                o7MzLQ3DRqVxu9heGJmejPcDoXXd86oew3ojirtuX5p2LFSoXRFSjDAqInwY
                mcNRENSp7HPTU/plGmmG1UFkAhW6/fHQVaGRcUg9vNDElK58nYLDUPB70h9M
                849FLIaSAhm2Ko2bz65fY05skW692pzHPO46mMMCw3zZ9i7/GGztb3MxsHO6
                NBv7wLAg2joKRkYeTfPzk3I36WxjOtYbaURHGEF1+HCcoNUwK1JUbmABJ/5c
                WbRNqLPDcHB5seBcXjg8k3b4End4OlEqZS4vSnyb1fg2301mEhaTnrGanLN/
                8tmSu4x6onB7808H9NCZ/agjGRbpG+ThaNiW8aloB8TkGpEvgqaIlbWnZPnt
                KDRqKL1wrLQi6ucy9n4tmsE5iUaxL18pm1Oc5jQnGdcCsQOOGdjDUcQskqTL
                ZL0mbb/EqeXufMLi8sq7j1cxGySTNEwSWWwSXptEIUM2rlAO98hvUZ4Qxxbh
                uQRRKUxOEYX/aFP4rc39f2/DUbmS66iSfkGeBzTfUgsJD0UPJZJY9rCChx5W
                8agFpqnN4xaSGgWNJxpZjZxGXmP2O5cbQQjTAwAA
                """,
        """
                initial/test/pack/OffsetClass.class:
                H4sIAAAAAAAAAI1QPW8TQRB9u/dhc/nwOYTgfBGgSig4x6IDRSKWIl1kMILI
                FG5Y25uw8fkOeddR6PxbqGlSRaKILEp+FGL2fFUUoZx082bezLzZmT9/f90A
                eIXnDNsqVUaJJDJSm+ib6A+j9umplqaZCK1LYAzhubgQUSLSs6jdO5d9U4LD
                4L+xnQcMzu5eZxEe/AAuSgyu+ao0w07rv8qvSSHLQwZvN473YgZ2Sb/F7wzL
                oqezZGJkuyiqtoaZSVQavZNGDIQRpMBHFw4twqyhyWxI1KWyUZ28wT7D59l0
                NeA1HvBwNg142TplQqc2mzZ4nR16v3/4POTHa6Gzwetuww+9An2LxJfu4q18
                g9FoPIrna57Qlh9oSXEmXw7pvW4zG0iGCp1Bvp+MenJ8InoJMSutrC+Sjhgr
                Gxdk8CmbjPvySNlg/eMkNWokO0oryr5N08wIo7JUYx+crmw/h2bT0cluUxQR
                0nPgvbhG+Yocjidk/Zx0sUN2cV6ABwgIq1jIGdt8SNX2aO7m1peft3r9vPfp
                PF/0Wm8Jy4V2hTxOeuG99IJ76nGqsZ1beEbYpFyV3r7ShRPjYYxVsnR6Mmsx
                HqPWBdNYx0YXZY1AY1PD1whzZ0mjorHwDymQrhH2AgAA
                """
    )
    // common_typos_enabled

    private val DensityStub: TestFile = compiledStub(
        filename = "Density.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0xaa534a7a,
        """
            package androidx.compose.ui.unit

            interface Density
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcIlnpiXUpSfmVKhl5yfW5BfnKpX
        mqmXlp8vxOmWn++SWJLoXaLEoMUAAALEmjo+AAAA
        """,
        """
        androidx/compose/ui/unit/Density.class:
        H4sIAAAAAAAAAIVOTUvDQBB9s7FNjV+pH1Bv4g9w2+LNkyBCoCIoeMlpm6yy
        Tbor3U2pt/4uD9KzP0qcqHdn4M17M/DefH69fwC4xDHhTNly4Uy5koWbvzqv
        ZWNkY02QN9p6E95iECGdqaWStbIv8n4600WIERH6k8qF2lh5p4MqVVBXBDFf
        RuxNLXQIVPFqZVo1ZFaOCCebdS8RA5GIlNnzYLMeiyG1xzHhfPLfP5wBQvKn
        LqrA4tE1i0LfmloTTh8aG8xcPxlvprW+ttYFFYyzvssZ2MJvCRz+YB9HPEds
        2eHu5ogyxBl6jNhuIcmwg90c5LGH/RzC48Aj/QaMxaG1RAEAAA==
        """
    )

    override fun getDetector(): Detector = NonLambdaOffsetModifierDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(NonLambdaOffsetModifierDetector.UseOfNonLambdaOverload)

    @Test
    fun lambdaOffset_simpleUsage_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.IntOffset

            val modifier1 = Modifier.offset { IntOffset(0, 0) }
            val modifier2 = Modifier.absoluteOffset { IntOffset(0, 0) }

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                Modifier.offset { IntOffset(0, 0) }
                Modifier.absoluteOffset { IntOffset(0, 0) }
                modifier.offset { IntOffset(0, 0) }
                modifier.absoluteOffset { IntOffset(0, 0) }
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.IntOffset,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun lambdaOffset_withStateUsages_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.IntOffset

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                val offsetX = remember { mutableStateOf(0f) }
                val offsetY = remember { mutableStateOf(0) }

                Modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                Modifier.offset { IntOffset(offsetY.value, offsetY.value) }
                Modifier.offset { IntOffset(offsetY.value, 0) }

                Modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                Modifier.absoluteOffset { IntOffset(offsetY.value, offsetY.value) }
                Modifier.absoluteOffset { IntOffset(offsetY.value, 0) }

                modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                modifier.offset { IntOffset(offsetY.value, offsetY.value) }
                modifier.offset { IntOffset(offsetY.value, 0) }
                modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                modifier.absoluteOffset { IntOffset(offsetY.value, offsetY.value) }
                modifier.absoluteOffset { IntOffset(offsetY.value, 0) }
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.IntOffset,
            Stubs.SnapshotState,
            Stubs.Remember,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun lambdaOffset_withAnimatableUsage_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.IntOffset

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                val offsetX = remember { Animatable(0f) }

                Modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                Modifier.offset { IntOffset(offsetX.value.toInt(), 0) }

                Modifier.absoluteOffset {
                    IntOffset(
                        offsetX.value.toInt(),
                        offsetX.value.toInt()
                    )
                }
                Modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), 0) }

                modifier.offset { IntOffset(offsetX.value.toInt(), offsetX.value.toInt()) }
                modifier.offset { IntOffset(offsetX.value.toInt(), 0) }
                modifier.absoluteOffset {
                    IntOffset(
                        offsetX.value.toInt(),
                        offsetX.value.toInt()
                    )
                }
                modifier.absoluteOffset { IntOffset(offsetX.value.toInt(), 0) }
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.IntOffset,
            Stubs.Animatable,
            Stubs.Remember,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonLambdaOffset_usingVariableDp_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Density
            import androidx.compose.ui.unit.dp

            val offsetX = 0.dp
            val modifier1 = Modifier.offset(offsetX, 0.dp)
            val modifier2 = Modifier.absoluteOffset(offsetX, 0.dp)
            val density = object : Density {
                override val density: Float
                    get() = 0f
                override val fontScale: Float
                    get() = 0f
            }

            @Composable
            fun ComposableFunction(modifier: Modifier) {
                Modifier.offset(offsetX, 0.dp)
                modifier.offset(offsetX, 0.dp)
                Modifier.absoluteOffset(offsetX, 0.dp)
                Modifier.offset(offsetX, with(density) { 0.dp })
            }
        """
            ),
            Stubs.Composable,
            Stubs.Modifier,
            DensityStub,
            Stubs.Dp,
            Stubs.IntOffset,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonLambdaOffset_usingPassedStaticArguments_shouldNotWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction(passedOffset: Dp) {
                val yAxis = 10.dp

                Modifier.offset(passedOffset, yAxis)
                Modifier.absoluteOffset(0.dp, passedOffset)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expectClean()
    }

    // State tests

    @Test
    fun nonLambdaOffset_usingStateLocalVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(offsetStateful.value, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(offsetStateful.value, yAxis)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingDelegatedStateVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful by remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(offsetStateful, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(offsetStateful, yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateReceiver_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.State
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunctionWithReceiver(offsetStateful: State<Dp>) {
                with(offsetStateful) {
                    val yAxis = 10.dp
                    Modifier.offset(value, yAxis)
                    Modifier.absoluteOffset(0.dp, value)
                }
            }

            @Composable
            fun State<Dp>.ComposableFunctionExtensionReceiver() {
                Modifier.offset(value, 10.dp)
                Modifier.absoluteOffset(value, 10.dp)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                    Modifier.offset(value, yAxis)
                             ~~~~~~
src/test/test.kt:17: $WarningMessage
                    Modifier.absoluteOffset(0.dp, value)
                             ~~~~~~~~~~~~~~
src/test/test.kt:23: $WarningMessage
                Modifier.offset(value, 10.dp)
                         ~~~~~~
src/test/test.kt:24: $WarningMessage
                Modifier.absoluteOffset(value, 10.dp)
                         ~~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingTopLevelStateVariables_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            private val offsetStateful = mutableStateOf(0.dp)

            @Composable
            fun ComposableFunction() {
                val yAxis = 10.dp
                Modifier.offset(offsetStateful.value, yAxis)
                Modifier.absoluteOffset(offsetStateful.value, yAxis)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                Modifier.offset(offsetStateful.value, yAxis)
                         ~~~~~~
src/test/test.kt:17: $WarningMessage
                Modifier.absoluteOffset(offsetStateful.value, yAxis)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingClassPropertiesState_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            class SecondaryClass {
                val offsetStateful = mutableStateOf(0.dp)
            }

            @Composable
            fun ComposableFunction(secondaryClass: SecondaryClass) {
                val yAxis = 10.dp
                Modifier.offset(secondaryClass.offsetStateful.value, yAxis)
                Modifier.absoluteOffset(secondaryClass.offsetStateful.value, yAxis)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/SecondaryClass.kt:18: $WarningMessage
                Modifier.offset(secondaryClass.offsetStateful.value, yAxis)
                         ~~~~~~
src/test/SecondaryClass.kt:19: $WarningMessage
                Modifier.absoluteOffset(secondaryClass.offsetStateful.value, yAxis)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingLambdaMethodWithState_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(run { offsetStateful.value }, yAxis)
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(run { offsetStateful.value }, yAxis)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateArgumentsHoisted_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.State
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction(offsetStateful: State<Dp>) {
                val yAxis = 10.dp

                Modifier.offset(offsetStateful.value, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                Modifier.offset(offsetStateful.value, yAxis)
                         ~~~~~~
src/test/test.kt:17: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateVariableWithSecondaryMethodCallNoStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful by remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
            }

            fun anotherTransformation(offsetStateful: Dp): Dp {
                return offsetStateful + 10.dp
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:19: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                         ~~~~~~
src/test/test.kt:20: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingStateVariableWithSecondaryMethodCallStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.State
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
            }

            fun anotherTransformation(offsetStateful: State<Dp>): Dp {
                return offsetStateful.value + 10.dp
            }

        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:19: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                         ~~~~~~
src/test/test.kt:20: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingDelegatedStateVariableWithComplexExpression_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.getValue
            import androidx.compose.runtime.mutableStateOf
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful by remember { mutableStateOf(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(offsetStateful + 50.dp + yAxis, yAxis)
                Modifier.absoluteOffset(0.dp, offsetStateful + 100.dp + yAxis)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(offsetStateful + 50.dp + yAxis, yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, offsetStateful + 100.dp + yAxis)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    // Animatable tests

    @Test
    fun nonLambdaOffset_usingAnimatableArgumentsLocalVariable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetX = remember { Animatable(0.dp, null) }
                Modifier.offset(x = offsetX.value, 0.dp)
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
            }
        """
            ),
            Stubs.Dp,
            Stubs.Animatable,
            Stubs.Modifier,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:15: $WarningMessage
                Modifier.offset(x = offsetX.value, 0.dp)
                         ~~~~~~
src/test/test.kt:16: $WarningMessage
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableArgumentsHoisted_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction(offsetX: Animatable<Dp, Any>) {
                Modifier.offset(x = offsetX.value, 0.dp)
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:14: $WarningMessage
                Modifier.offset(x = offsetX.value, 0.dp)
                         ~~~~~~
src/test/test.kt:15: $WarningMessage
                Modifier.absoluteOffset(0.dp, y = offsetX.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableReceiver_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunctionWithReceiver(offsetStateful: Animatable<Dp, Any>) {
                with(offsetStateful) {
                    val yAxis = 10.dp
                    Modifier.offset(value, yAxis)
                    Modifier.absoluteOffset(0.dp, value)
                }
            }

            @Composable
            fun Animatable<Dp, Any>.ComposableFunctionExtensionReceiver() {
                Modifier.offset(value, 10.dp)
                Modifier.absoluteOffset(value, 10.dp)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:16: $WarningMessage
                    Modifier.offset(value, yAxis)
                             ~~~~~~
src/test/test.kt:17: $WarningMessage
                    Modifier.absoluteOffset(0.dp, value)
                             ~~~~~~~~~~~~~~
src/test/test.kt:23: $WarningMessage
                Modifier.offset(value, 10.dp)
                         ~~~~~~
src/test/test.kt:24: $WarningMessage
                Modifier.absoluteOffset(value, 10.dp)
                         ~~~~~~~~~~~~~~
0 errors, 4 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingLambdaMethodWithAnimatable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { Animatable<Dp, Any>(0.dp) }
                val yAxis = 10.dp
                Modifier.offset(run { offsetStateful.value }, yAxis)
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(run { offsetStateful.value }, yAxis)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, run { offsetStateful.value })
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingTopLevelAnimatableVariables_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            private val animatable = Animatable<Dp, Any>(0.dp)

            @Composable
            fun ComposableFunction() {
                val yAxis = 10.dp
                Modifier.offset(0.dp, animatable.value)
                Modifier.absoluteOffset(0.dp, animatable.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:17: $WarningMessage
                Modifier.offset(0.dp, animatable.value)
                         ~~~~~~
src/test/test.kt:18: $WarningMessage
                Modifier.absoluteOffset(0.dp, animatable.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingClassPropertiesAnimatable_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            class SecondaryClass {
                val animatable = Animatable<Dp, Any>(0.dp)
            }

            @Composable
            fun ComposableFunction(secondaryClass: SecondaryClass) {
                val yAxis = 10.dp
                Modifier.offset(0.dp, secondaryClass.animatable.value)
                Modifier.absoluteOffset(0.dp, secondaryClass.animatable.value)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/SecondaryClass.kt:19: $WarningMessage
                Modifier.offset(0.dp, secondaryClass.animatable.value)
                         ~~~~~~
src/test/SecondaryClass.kt:20: $WarningMessage
                Modifier.absoluteOffset(0.dp, secondaryClass.animatable.value)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableVariableWithComplexExpression_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetX = remember { Animatable(0.dp, null) }
                Modifier.offset(x = offsetX.value + 2.dp, 0.dp)
                Modifier.absoluteOffset(0.dp, y = offsetX.value + 5.dp)
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:15: $WarningMessage
                Modifier.offset(x = offsetX.value + 2.dp, 0.dp)
                         ~~~~~~
src/test/test.kt:16: $WarningMessage
                Modifier.absoluteOffset(0.dp, y = offsetX.value + 5.dp)
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_animatableVariableWithSecondaryMethodCallNoStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { Animatable<Dp, Any>(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful.value), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful.value))
            }

            fun anotherTransformation(offsetStateful: Dp): Dp {
                return offsetStateful + 10.dp
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful.value), yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful.value))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    @Test
    fun nonLambdaOffset_usingAnimatableArgumentsWithMethodCallStateInSignature_shouldWarn() {
        lint().files(
            kotlin(
                """
            package test

            import androidx.compose.animation.core.Animatable
            import androidx.compose.foundation.layout.absoluteOffset
            import androidx.compose.foundation.layout.offset
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.remember
            import androidx.compose.ui.Modifier
            import androidx.compose.ui.unit.Dp
            import androidx.compose.ui.unit.dp

            @Composable
            fun ComposableFunction() {
                val offsetStateful = remember { Animatable<Dp, Any>(0.dp) }
                val yAxis = 10.dp

                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
            }

            fun anotherTransformation(offsetStateful: Animatable<Dp, Any>): Dp {
                return offsetStateful.value + 10.dp
            }
        """
            ),
            Stubs.Modifier,
            Stubs.Dp,
            Stubs.Remember,
            Stubs.Composable,
            Stubs.SnapshotState,
            Stubs.Animatable,
            OffsetStub
        )
            .run()
            .expect(
                """
src/test/test.kt:18: $WarningMessage
                Modifier.offset(anotherTransformation(offsetStateful), yAxis)
                         ~~~~~~
src/test/test.kt:19: $WarningMessage
                Modifier.absoluteOffset(0.dp, anotherTransformation(offsetStateful))
                         ~~~~~~~~~~~~~~
0 errors, 2 warnings
            """
            )
    }

    // Non modifier related tests

    @Test
    fun nonModifierOffset_bytecode_shouldNotWarn() {
        lint().files(
            kotlin(
                """
                package another.test.pack

                import initial.test.pack.AnotherClass
                import initial.test.pack.offset
                import initial.test.pack.OffsetClass

                val offsets = OffsetClass()
                val otherOffsets = AnotherClass(0)
                val anotherOffset = offsets.offset(0, 0)
                val anotherOffsetCalculation = otherOffsets.offset(0, 0)

        """
            ),
            AnotherOffsetDefinitionStub.compiled
        )
            .run()
            .expectClean()
    }

    @Test
    fun nonModifierOffsetKotlin_shouldNotWarn() {
        lint().files(
            kotlin(
                """
                package another.test.pack

                import initial.test.pack.AnotherClass
                import initial.test.pack.offset
                import initial.test.pack.OffsetClass

                val offsets = OffsetClass()
                val otherOffsets = AnotherClass(0)
                val anotherOffset = offsets.offset(0, 0)
                val anotherOffsetCalculation = otherOffsets.offset(0, 0)

        """
            ),
            AnotherOffsetDefinitionStub.kotlin
        )
            .run()
            .expectClean()
    }
}