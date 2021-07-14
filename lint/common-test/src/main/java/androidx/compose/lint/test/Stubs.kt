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

package androidx.compose.lint.test

import com.android.tools.lint.checks.infrastructure.LintDetectorTest.compiled
import com.android.tools.lint.checks.infrastructure.LintDetectorTest.kotlin
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestFiles.bytecode
import org.intellij.lang.annotations.Language
import java.util.Locale

/**
 * Common Compose-related bytecode lint stubs used for testing
 */
object Stubs {
    val Color: TestFile = compiledStub(
        filename = "Color.kt",
        filepath = "androidx/compose/ui/graphics",
        source = """
            package androidx.compose.ui.graphics

            inline class Color(val value: ULong) {
                companion object {
                    val Black = Color(0xFF000000)
                    val DarkGray = Color(0xFF444444)
                    val Gray = Color(0xFF888888)
                    val LightGray = Color(0xFFCCCCCC)
                    val White = Color(0xFFFFFFFF)
                    val Red = Color(0xFFFF0000)
                    val Green = Color(0xFF00FF00)
                    val Blue = Color(0xFF0000FF)
                    val Yellow = Color(0xFFFFFF00)
                    val Cyan = Color(0xFF00FFFF)
                    val Magenta = Color(0xFFFF00FF)
                    val Transparent = Color(0x00000000)
                }
            }

            fun Color(color: Long): Color {
                return Color(value = (color.toULong() and 0xffffffffUL) shl 32)
            }

            fun Color(color: Int): Color {
                return Color(value = color.toULong() shl 32)
            }

            fun Color(
                red: Float,
                green: Float,
                blue: Float,
                alpha: Float = 1f,
            ): Color = Color.Black

            fun Color(
                red: Int,
                green: Int,
                blue: Int,
                alpha: Int = 0xFF
            ): Color = Color.Black
        """,
"""
        androidx/compose/ui/graphics/ColorKt.class:
        H4sIAAAAAAAAAJVUz28bRRT+Zn/a2026ae202dSpk7hglyR2UkgAN6EhUiSn
        LpUo6SUHNF4v7ibrXWt3XZULREj8EVy5c+GALA4oKjdO/EWUN5uVnaZCLSvN
        vB8z73vfmzezf/3z+x8APsQuQ4UH3Sj0ui/qTtgfhLFbH3r1XsQHzzwnru+F
        fhg9THQwBuuYP+d1nwe9+uPOseuQV2ZQ0y0MSvWgdsBgnoSJ7wX1w3YY9HTo
        FOaEQZxEQycJo1WvP/BN5KEZyMFQiQRe0cew9HYaOqYNXIVBOZ3znOxA5G2J
        vKzFoFf36RNWfo9AeOCFAcNa++3QlfH+polZ3MhTnpsMK/8nUodNxfbc5HOf
        Oyerje7W1w+PDxnkau3AxC2UDMxjgezI7RLffSqjF7kuMVQ6/tAlk/uDZ5xh
        6hy4637Dh37CYKdltdqXj78pcFUUDUh4X1Tfok9Ub6fafwbURMAHDDPtrFeP
        3IR3ecKbDFL/uUw9YWJSieWJUCTyv/CE1iCtu87w99npgnF2akg3pVRYuiHl
        VJI5khLJPLnthkWT1GAbmiWRlEkqmVRTmctJlia0P0fs7PTlzxoB2bOTqBzJ
        /GTNuIyYzxDzGWJ+jHgh6splRHOyNvXyB0khynOirg16DO9wV+iUwJBL9bUT
        6o+yF3apfVfbXuB+Mex33Ogr3vHJc60dOtx/yiNP2Jlz6klC1+MRH2S28SQc
        Ro677wlj7sthkHh996kXe7S6GwRhwhO6XDHWqWmKeC/QMEdd1IhEk6wOeXWS
        Kwu78qZSHuHKbgHmliJvauVZ5XthW+VNMtVyUfmWzBGsX0RTcZ/maynmLAwa
        No0qjW3ymIROmJihHUjzXc/ytSFDvFvbvi9vz/9IgA+s8g6lvl16A30a4tLY
        hG6n+BNkm5ALGXIxQ36cVaj/irnfcHsCo6UhS2n43fMtKGMxPQyd/Ivpuo5l
        0qRUq5AmW3ncwXsZeJPiNFFwUbFy3/0E1dxfWq7cGaF6nmeHZhnMuMC7RL+o
        ReJeSlnW3o1l7RLLwphlYcyyMGZZyFjezcC3M5aFjKU+g1c79vyt0ggrr/E0
        L/CsEM8qnWuFlj9LN32KByQPCXSVmK8dQW6h3kKDZqy3sIF7Lfr5f3QEFmMT
        W0eYjqHG+DhGOcYnMZZiLMeoxCjGmEk912PUUkX7F4jgH2k8BgAA
        """,
        """
        androidx/compose/ui/graphics/Color＄Companion.class:
        H4sIAAAAAAAAAJWWXVMcRRSG3579ZFiW5TNACBpcI2BgAWOiIUbDYpBkSZQQ
        YkTFZnaEgdkZanoWkzvKKv0feu+FV6a8sCi883/4NyxP9ywMjl2roYrunvO+
        Tx+mZ84Z/vjr198AXIPFcJV79cB36s8qlt848IVdaTqVnYAf7DqWqFR91w/K
        VVK45/heDoyhtMcPecXl3k7l4faebYU5pBiytxzPCW8zpCYmNwrIIGsijRxD
        Otx1BMNM7WUSLVCaHTtcdLm1Pz1bv7F1f++x2voew/h/b5RDF0MvtyxbiPLp
        PmXroIBuFEwUUWLoo/gSD/aXA/48TjEQQ6ei4voirp+hm6R/Mj0xc+a/EPmH
        GPopXHN2dhPQYAydqYq8GJGj0Qk82XVCO6bO3ZNSFPFKRLzKUKT4ml2P/aXY
        T3Hlfi1yl6P9lwPb9rT7K0URb0TERHTvi27T1t67FJT/zch/lVQKP7Vd1/8m
        JvpiIpIUU4mY2ShH9Tn3tDmkoPxvRf5r9CdTeJXv2F7IY6Q/Rlqaom5E1Dt0
        /KSsB9wTBzwgOSaHYvKcruiFiL7FMDdR2/dD1/Eqe4eNiuOFduBxt7Jkf82b
        blj1PREGTSv0g1V6h+xgYXLDhCGLoa9sxeJWQ6myNl5qNzqRU2DVDnmdh5xi
        RuMwRUXN5JBhYPsUeubIKzpUoz7H2J/HR/2mMWSYRun4yDTyRnSRH86ffJ8a
        Oj6aN2bZYi5vnPyYNUrG2mgpNWLMpn9/wY6PaGD0KyWT8NxIOp8pZcmSb2fp
        UBaznaVTWQrtLF3KUmxn6VaWUjtLj7L0trP0KUt/O8uAsgy2s1xQlqF2lmFl
        GWlnuagso+0sl0rZk2+NbvkAzUz+5IexWUbry/JhzzN6EZBRbY+h/D9aL71A
        jHprWhYxTbLOGPKnLZAi0ZRRXYGh46xnMeRaFUb9mXoMQ+e5uiFC9Sn6PkTF
        TuhZk6cEKvfMfihT+nWyddccz37QbGzbwTrfdinSW/Mt7m7wwJHXrWBhxfPs
        oOpyIWz6uJiP/GZg2XcdqQ2vNb3QadgbjnDIfMfz/JCHlE9gjqowDflTpBV9
        o+iUvqCrCs1M1s3UL8j/TAsDX9KYjYLYorHQWnfApLkHnSoi4RlySy39Aj0/
        JdjsOTZ9xvbq2IEkm9Oygzp2OMnmteyIjr2UZDu07JiOvZxkTS07rmNfT7Kd
        WvaKjp1MsgUtO6Vjp5Nsl5ad0bFzSbaoZed17NtJtlvLXtex7ybZkpa9qWPf
        S7I9/2bp36EMbrfYaZqNVjG8L4uBKWAwCraSydUHuENaCl+pBy+hIjYxDK4S
        fo5tmr+j+CJ5q5tIrWBpBR/SiLtyWF7BR1jZBBO4h/ubGBMwBWoCWYFVgQcC
        nQIFgYcCHwtMCXwiMC+wJtAr8EhgUGBd4IrAY4ERgQ2B6wJPBMYFPhW4KfBU
        yD0/E5j5G4ru30HxCgAA
        """,
        """
        androidx/compose/ui/graphics/Color.class:
        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
        QjGCDwAA
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcolmZiXUpSfmVKhl5yfW5BfnKqX
        m1iSWpSZmCPE4Zyfk19U7F3Cpc4li1OZXlp+vhBbSGpxCVihDIbC0ky99KLE
        gozM5GIhdrCR3iVKDFoMAMec7K6RAAAA
        """
    )

    val Composable: TestFile = compiledStub(
        filename = "Composable.kt",
        filepath = "androidx/compose/runtime",
        source = """
        package androidx.compose.runtime

        @MustBeDocumented
        @Retention(AnnotationRetention.BINARY)
        @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
            AnnotationTarget.PROPERTY
        )
        annotation class Composable
        """,
"""
        androidx/compose/runtime/Composable.class:
        H4sIAAAAAAAAAI1STW/TQBB96yRNCFASykfSUvqdFg64VNw4OWkqIuVLjotU
        5YC29qpy49hVvA7tLQck/hMHFHHkRyFmExEHyRLY0tvZmTc7O2/n569v3wG8
        w2uGPe47o8B1bnU7GN4EodBHkS/dodBrsz2/9EQWjKFwzcdc97h/pXcur4Ut
        s0gxbMVe7vuB5NINfN1YmFlkGPabg0B6rr9MaUWhrIrTwI6GwpfCec+wmUAz
        haQwWRTPjLkXCYbDBF5ccTljpdpoG+YFw3pCisVHV0ISa5V7XvBZOHNHmHzf
        uMAiL3d23q5ZjU6bIW1ddOt0klo+dQ3TaNWtukmUrtnp1k2LrrDTTJTqLwW2
        kznLLVX+QekGnmvfKbVqTaPXU6omJiy62E2O1z2hrmXd3QglJPXzoXNKDc06
        PO9Rs8U/KrWE5A6XnHjacJyiyWIK6OHZgFy3rtodk+W8ZShPJ7m8VtLyWmEj
        9+OrVppOTrRjVp1OFOGE4aD5HxNJpcDwMHa8GUiGfC+IRrY4cz2akrI5z/ro
        hi4R4vcLK1QJacpfgfo0HM3wEK9o/YIs/UCO4vcE8riPB6pUH2mBVTxSUFBQ
        VPAYa8R9Muc+xTM8V2YfKYESygqKCtaxgQxekL+BzQZeEmJLwXYDO9jtg4XY
        w34fWoiDEJXfIrDY76EDAAA=
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AJcbFUpJaXCLEFlxSmuQNpEOAPO8S
        JQYtBgBDd0xtMAAAAA==
        """
    )

    val Modifier: TestFile = compiledStub(
        filename = "Modifier.kt",
        filepath = "androidx/compose/ui",
        source = """
        package androidx.compose.ui

        @Suppress("ModifierFactoryExtensionFunction")
        interface Modifier {
            infix fun then(other: Modifier): Modifier =
                if (other === Modifier) this else CombinedModifier(this, other)

            interface Element : Modifier

            companion object : Modifier {
                override infix fun then(other: Modifier): Modifier = other
            }
        }

        class CombinedModifier(
            private val outer: Modifier,
            private val inner: Modifier
        ) : Modifier
        """,
"""
        androidx/compose/ui/CombinedModifier.class:
        H4sIAAAAAAAAAKVTWU8TURT+7nRlKFJGyw6iVOmCDCAusQajGJMmBQ0QYoIv
        t+0FbjudITPThkfiT/EX6AOR+GAIj/4o47ltKVpIeTBNz/Lds58zv37/+Alg
        Bc8Zktwuu44sH5klp3boeMKsS3PNqRWlLcrrTlnuSeFGwBjiFd7gpsXtffN9
        sSJKfgQBhsnr/C/9Qgzhl9KW/irDq1Shl3Gu92t6h2G24Lj7ZkX4RZdL2zO5
        bTs+96VD8objb9QtK8cQcuq+cKPQGaarjm9J26w0aqa0CbW5ZeZt3yVvWfIi
        iDEkSgeiVG27f+AurwkyZJhLFbo7zv2FbKkg+1RWDLcwqGMAcUotbVulNhgC
        KfUUwR0dQSSolN7txaBjpA8aRmMwWtI4Q9A/kB7Dw2t9u7eUa9oLm2H1hkmn
        ez8zpHu9J9+KPV63/Hzt0KIRzjB8+r/N3jSZadzXcQ+zarXUIO1mqNBe7Lrw
        eZn7nGrWao0AXTVThM6OVQk6kkpbJKm8xLB9dhzTtVGt9Y8GRs+Ol7VF9iZh
        hOPaOEnEA4qffwlr8eDmrZb28fxzUCH62TFBWhekYi8z9F/Uu1D1GSY267Yv
        ayJvN6Qni5Z4fXmptKU1pywYBgu0vY16rSjcbU42DEbBKXFrh7tS6W0w2R2r
        c6P/BNW3nLpbEu+k8hlr++xcyY4luqwg6LMkbqijI3mJhhQm3kfcUOfXhWnE
        I4gSXyZNEFdjncga/acYmjduE818x3Ame4KxzPwJJr41nR43wwURQ5x+Q0iQ
        Nk7SCuEzrRCYxBTQlFqlKEkVoOEJyQNau4ILOo275KiqeEEmmqoxkz1F8msn
        YbgZpJVkuGXRSRLGgyu9qTQR1mn0aZMu4lkzBX16CGFuF4E8UnmkiSKjSDaP
        eTzaBfOwAHMXIQ9THkY86B4MD9E/kXas0GMFAAA=
        """,
        """
        androidx/compose/ui/Modifier＄Element.class:
        H4sIAAAAAAAAAI1QTU8CMRB9s6ssXyogKqjxRDy6QLx5Mn4km0BMNPHCqbDF
        VHZbst0lHPldHgxnf5RxNtHEE6HJvL5586ad9uv74xPANc4IHaHDxKhw6U9M
        PDdW+pnyhyZUUyWTzkMkY6lTD0SovYuF8COh3/yn8bucsOoSzjf1e9gl1Acz
        k0ZK+0OZilCk4obgxAuXJ6Ac2EIzlpYqz7rMwh6huV4Vy07LyaM4ba1XfadL
        ea1PuBxsMzRfc7HRyIbeNgd17uVUZFEaxPPIejgkVP8rBO/XSKj89V7NOKsG
        WsvkLhLWSraVX0yWTOSjiiSh/ZzpVMXyVVk1juSt1iYVqTLaFviV2AGhgHy5
        aDE2WGtyHKHNWMBxocQMzE9wynuP/fzbKI7gBigFKDOikkM1wB72RyCLA9RG
        cCzqFo0fJIqRGAUCAAA=
        """,
        """
        androidx/compose/ui/Modifier＄Element＄DefaultImpls.class:
        H4sIAAAAAAAAAKVSS2/TQBD+Ni8nqUvTlBRKS3k0tHlATSVuOaECkiU3IIpy
        gcvG2aab2LuVvYn6szhWHBBnfhRinBpoixQiYcmz8/hmZveb+f7jy1cAL/Cc
        4YCrQaTl4NzxdXimY+FMpHOkB/JEiqj+OhChUKb+SpzwSWDc8CyILTCGyohP
        uRNwNXTe9kfCNxayDDlzKhTDoOEtUrUzF9Vpzg8z7Hg6GjojYfoRlyp2uFLa
        cCM16V1tupMg6MzuJOMiigzbY20CqZzRNHSkMiJSPHBcZSJKlj69q8xQ80+F
        P06z3/GIh4KADHsN7+aLO1c8x0mRYafZs2FjuYwl3GLIa6IjKqLCsDXvKRaq
        DM25jF3n/zbDp/kU/x+1NvJYL6OGOwy7i42S2P3XuFa9lP8jYfiAG06+TDjN
        0iayROQZ2DhRaJEy5zLRaD/ri7S3sMNgXyWJwUpjDEu/4PtjsnKHeiAYVjyp
        RHcS9kX0gfcD8lQ97fOgxyOZ2Klz8/1EGRkKV01lLMn18s+S0eVuRn9vzDWY
        7SolosOAx7Egs3ysJ5Ev3sikwUZaovdXeRwggxySj6FEIykgiwZZLvkzdNZa
        1dIFVtrVVZKtb1hrX+DuZwpk0CRZoDSbElukr18moIiNWcEaKrhH8XaKs+h8
        Sv9yJjUuZRbPSFZn/ep4Qsl1rGG3UML+rM0eHDq3Cb1J2K2PyLq472KbJB64
        eIhHLh7/BG04LjBuBAAA
        """,
        """
        androidx/compose/ui/Modifier＄DefaultImpls.class:
        H4sIAAAAAAAAAKVSXU8TQRQ9swW2LUVKEbCCoFKlLcqi8a1GYzAmG9tqrGli
        9GW6HWDa3RmyO9vwi/TV+KLRxPjsjzLehRUQTX1wk71zz/04M/fj+4/PXwHc
        w12GGlf9UMv+oePp4EBHwoml09J9uStFWHksdnnsGzc48CMbjKE44CPu+Fzt
        Oc96A+EZGxmGCbMvFMObanMcW2O8tzbezbDe1OGeMxCmF3KpIocrpQ03UpPe
        1qYd+37j6C0yyiLLsDrUxpfKGYwCRyojQsV9x1UmpGTpUT15hgVvX3jDNPs5
        D3kgKJBho9o8X2njjKWTkOw1at0CCpjJYxoXGCY1tSHMosiwMq4UGyWG3A45
        uKLHM4xvW+UkslHARSzkMI9Fhsrfcii0J5Xon151iWHqvlTSPGB4+H/zoWIv
        YzmPMlaouf+a1lwzbX9LGN7nhpPNCkYZWjyWiEkGNkwU2h/rUCbaNkPh7MYx
        TP8i3BoaGu2O7guGmY7h3rDFD17ynk94tkklt+OgJ8LUUmpqj/tdHsoEp8bl
        F7EyMhCuGslIkunR6f5QO897T5bht7CCq5QId3weRYJgvqPj0BNPZHJBOaXo
        /kGPO7AwgeSjyWMSU8igRkiQ1aKzUi/lPmF2szSXyA9Yegu7/g6zX1B+Vd/8
        iCvfMP8+6RXqJG1YS09tWoNNAlNEWSDTLdIXj8mQxerRZRUUSWO4ncbZdG7R
        P2Ol4Fhm4JDME7KIdIMGvE3YQpXejSOCNYq5+hoZF9dcXCeJdZfYb7i4+RNB
        PoFzTQQAAA==
        """,
        """
        androidx/compose/ui/Modifier.class:
        H4sIAAAAAAAAAIVSS2/TQBD+ZuPESRogoTzSB6XQUBJeLhVcKFSqQhFGbUAU
        9dLTNtmUbZ115d1EPeZX8D+AGwcUceRHIcZVoRSkYMvzzXwzOzOe2e8/vnwF
        8Aj3CbPSdJJYd46Cdtw7jK0K+jrYjDu6q1XigwjlfTmQQSTNXvB6d1+1nY8M
        wXPvlSGs1jfGJVhpjHcTFjbiZC/YV243kdrYQBoTO+l0zHordq1+FHFU/mk7
        0ka7VUKm3tgmLI5LW2syJw3n8FEkFGu1sLX1bq3VXCeM7/f05EoJJZwrYALn
        CYXfdAnllBWoECobB7HjtoJN5WRHOsmNit4gw6OlVGQJdMDUkU6tJdY6DwmP
        R8NSUVRFUZRHw6LIe/ludTSc95bFEj0RXvZVpSymxdJouJwrZ46Vl98+eOnh
        ZcLc/8YJQjbm1SQEfz1SPWUcoTb2l0/CfNwgNMZGPldd2Y9c2DuMrI8FQulP
        hjDxK/LBAVededs3TvdUaAba6t1IrZ2ulnv62/tGJrKnnErOhHnNuKO4UGiM
        SpqRtFYxW9yK+0lbvdAR+6ZOMm3/UyXHU4OXrgNT6d4Yb7OVY/QZBU81y5Y4
        w9b58+nEyPNbYL3B+iRj+vifcOEzLn48NjK4w3Ka8Sbnmc+lF0ZwrSnUGG8x
        3k0rYRH3GJ9xikmueWkHmRCXQ1xhiaupqIYcO70DspjB7A5yFtcs5iyyFtct
        yhbzPwFNAZX3twMAAA==
        """,
        """
        androidx/compose/ui/Modifier＄Companion.class:
        H4sIAAAAAAAAAI1SXU8TQRQ9s1u6y1JkQYHyIX5QoYCyQHwwlpBgo7GmVCOE
        xPA03Y4w7XbW7G4bHnnyJ/gD/AUSHzCaGMKjP8p4p1QUTMCHuTP3zDn33rl3
        fvz8+h3AQ6wwzHBVi0JZ2/f8sPkujIXXkt5GWJNvpYhyRcK4kqGywBjcOm9z
        L+Bq13tZrQs/sWAyTF4WwUIPQ3pVKpmsMZj5ue0MLNgOUuhlSCV7MmbIl/+v
        iEJHIRTD2uWSwtzl1wzT5TDa9eoiqUZcqtjjSoUJTyhJ7FXCpNIKAmL1hJQu
        suEyTDXCJJDKq7ebnlSJiBQPvJJKIlJLP7YwxDDs7wm/0ZW/4hFvCiIyzObL
        FztX+AvZ1EF2C7o1NzDs4DpGKN9VL7BX/aDTVgeG7qWTy5Uqm1vrleLTDCaQ
        6SV4kmGw3C18QyS8xhNOUqPZNmn+TBsaD2sQtC+1t0Sn2jLDo+ODjGNkjdNl
        m/bJezN7fLBiLLEnlm2cfEwbrvFi0DXHCVlJuym9Pz/5kNJ6+lW9ZzNj6Ptd
        9GIjoQEWw5pgGChLJSqtZlVEW7waEDJUDn0ebPNIar8LTrxuqUQ2RUm1ZSwJ
        Wv8zJ4bcxduznp+jZUpKiagY8DgW5DqbYSvyxTOpE4x1Q2z/Ex7L1MAU6PeC
        uVndUeqPSRh9X0LnyfNoZ7qH80dwDulgYIFsugP24T7ZzCmBPC1n6Mc1CqLF
        q8Q2aLcXhga/YHTh0zl9mvhaP3LK6er1yUWW7h90eQO0L9KyWNexMXZW32hH
        TKV8g/HmCOOfcfOwA5hUOuAQzaAoeZIsdbLP0aOBx4RPUc23dmCWcLuEO2Rx
        V5vpEnK4twMWYwazO+iJkYmRjWHH6I/h/gJ6Cp4UWgQAAA==
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcklnpiXUpSfmVKhl5yfW5BfnKpX
        mqmXlp8vxBeSWlzim5+SmZaZWuRdosSgxQAATlErVEMAAAA=
        """
    )

    val Remember: TestFile = compiledStub(
        filename = "Remember.kt",
        filepath = "androidx/compose/runtime",
        source = """
        package androidx.compose.runtime

        import androidx.compose.runtime.Composable

        @Composable
        inline fun <T> remember(calculation: () -> T): T = calculation()

        @Composable
        inline fun <T, V1> remember(
            v1: V1,
            calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <T, V1, V2> remember(
            v1: V1,
            v2: V2,
            calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <T, V1, V2, V3> remember(
            v1: V1,
            v2: V2,
            v3: V3,
            calculation: () -> T
        ): T = calculation()

        @Composable
        inline fun <V> remember(
            vararg inputs: Any?,
            calculation: () -> V
        ): V = calculation()
        """,
"""
        androidx/compose/runtime/RememberKt.class:
        H4sIAAAAAAAAAK1WXVPbRhQ9K38J29jCfJSYJiFgyndkG5q2MZBSWhpPCekE
        j9opT8JWqMCWMlrZk0emL33pH+hrf0Ef0z50GPrWH9XpXVlgg4UJTTzW3qvr
        u+ecu3el9T///vkXgFV8xTCtWzXHNmuv1ardeGVzQ3Walms2DPWF0TAaB4bz
        jRsDY1CO9Jau1nXrUH1+cGRUKRpikB0/i2F1bufYduumpR61GurLplV1Tdvi
        6rbv5UvzO1cxSgyba5XHvfGNm8DWFiuV0kZpnkaGmZ1rq9jy7vWDukF50zu2
        c6geGe6Bo5uEpluW7ept5F3b3W3W65SVqOr1arPuxWXEGe51STEt13Asva6W
        LdchDLPKY0gyjFZ/NKrHPsi3uqM3DFesyuxcb3FdkT0Bclia15JIIR3HIJTL
        fAGlx5BhiJpWyz42GEbmApY1iRGMJjCMMYbBnJl7mev0iZUZJm9qFcN2kPD/
        0+AfAhusFQK7XtEKN7Fc6rzUKjBkgmi/77/w71IRf/uKtOK1ZVa04i1LLTIc
        vV1V76fOX96xTm2lb/EVbeWWC7DC8PXc/nuqrrKmBcq7PT6p1DyVWsl7MF81
        XS5jlmE4AIth6BzumeHqNd3VRW2NVoheyUwMEXpGj4UjUfy1Kbw8ebUCY+7p
        yVhckkNxaVwim4xLyhBdpyfx05PsDNms9JRNheXTE4UVk4qUlTPhDIXyoadn
        P8t/v2GnJ2e/RSUlnF25nEyGKZFiVIlSMNJvaiy7GTSVRkmRLwCiygBZuR9Q
        PPv8eiAaQ0qiBy6qJMkm+sEOZtfbsKk2bKo4pqSzyYwss0x4nOWH8sqUZ7tA
        UldBMmc/SbF4RD77tZhnYu3p0WMV8abxW9f9ppS0ghiKYqAdyjRGjUTi/Px8
        eOwyhLfsGr2s0zumZew2RbgiziSBaNNxo+mOKe794MCeeWjpbtMhf+JF+yQr
        Wy2Tm/TzZufQYshd/fXi6LmUFt+zm07V2DYF+h1/jtaDhwIkhCE+EdyhK0p3
        63S3RXGJbHohk3iDodDawh/4gOF3sUmxQWOUKpYRwxPyxygm/HGCYGIS4siS
        /dzLjmHzIh/4gq4YrRkGyBGMEz7jM0oVOz+92GZcXwxkHPQYJyn1nNGTibu4
        55XR5mY+94c93AMivcN+32f/zl+H9FKbfWMpkH3EY1+g1HP20BX2B+R11kDy
        dUz26EiEvAkdJVO+kgNKj5BVlttKHoWXA6QMUGlPvL90EfLbUoR+5UKKciFF
        QY48yfOEqJAvarpH1GB7K3TLmvFl7fntGV3IzJGsfk1KUS3nTUp1NWkUs5j3
        4EcvNemjXh2Sr6A9SrQdxbiGL8nWKLpAyhb3ESpjqYxlGvGwDBX5Mm3o4j4Y
        xwpW9zHMEeH4mCPO8YgjyvEJx12OCY5POR5w3Of4jCPHMcXxmGOWo+R9Z/4D
        oSNh5zILAAA=
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXFJYNLnV5afr4Ql1t+fnBJYkmqd4kSgxYD
        AK/uqdJsAAAA
        """
    )

    val MutableState: TestFile = compiledStub(
        filename = "MutableState.kt",
        filepath = "androidx/compose/runtime",
        source = """
        package androidx.compose.runtime

        fun <T> mutableStateOf(value: T) = MutableState<T>()

        class MutableState<T>

        fun <T> mutableStateListOf() = SnapshotStateList<T>()
        class SnapshotStateList<T>

        fun <K, V> mutableStateMapOf() = SnapshotStateMap<K, V>()
        class SnapshotStateMap<K, V>
        """,
"""
        androidx/compose/runtime/MutableState.class:
        H4sIAAAAAAAAAI1QTUsbQRh+ZjbZ6Jrq+tXG2tpT8ePgqghCK0JbKATWFpqQ
        S06T7KBjkhnZmRWP+1v8B54ED7L06I8qfSd6KO2lc3je93nm4f16/HX/AOAQ
        7xjeC53lRmXXydBMLo2VSV5opyYyOS2cGIxlxwknG2AMW8fdD+mFuBLJWOiz
        5PvgQg7dx5N/JYb4b62BGkN4rLRyJwzB1naviRCNCHXMMNTcubIMm+l/DUP1
        F9ORcWOlk1PpRCacII1PrgLainmoM7ARSdfKsz3Ksn1aoCqbEW/xqCojHhNU
        Zasqd2ozVRmzA77HP9d/3oQ8Drz/gEp0GdXDwp/Nd0eO5v1iMkkfqdLyWzEZ
        yLzrDQxLqRmKcU/kyvNncbajzrRwRU551DFFPpRflf9Y+/G0XU9ZRc5PWhtq
        oYy22Aen0/hHY/hLEa4RS6acFty5w+wtJRyvCcOpGGKdsPlkQIQ5igHeTF0B
        3k5jCxsUj8jTJM+LPoI25ttYIETsYbGNJSz3wSxWsNpHzWLO4qXFK4vGb2l1
        Fhc6AgAA
        """,
        """
        androidx/compose/runtime/MutableStateKt.class:
        H4sIAAAAAAAAAKVUXU8TQRQ9s223ZUG6VBFaFBBEWr624MeDbUiMidq0hcQ2
        TQwPZtquuNDukp1pwyO/xV/gm4kmhvjojzLeaResVE2Jm+y9d+6cc+7e2b37
        /cfnrwAe4QnDKnebvuc0T62G1z7xhG35HVc6bdsqdySvt+yK5NIuyigYg3nE
        u9xqcffQ2q8f2Q3Khhgm2wPI/XcMuXTpKjCXKY1UKMfwMl99OszfTVero4rk
        CbpLSsslzz+0jmxZ97njCou7rkf7jkfxnif3Oq0WoVZG0oxijEHPO64jdxlC
        6UxtAuOYMGDgBkOky1sdmyEx/OCUHDyfkiOkOqOt9D+aqbj8RLz35CWDVAp/
        OZbryFwczNrolChuGZhWPU4NtlHmJ6qLzZHLE4EKv80X/9BE7b8aI+V8tZir
        1nqtZUYmRZEyMNfrrHTsyZbjWmVb8iaXnHS0djdEM8KUiTCwYxVolD91VJSl
        qLnNcHp+ljLOzwxtVjO0mBb40MU6lTAJEEuEE9orLcuWwrHzM1NLzZuh1GQv
        GelZPRvub0XIMVNPLZjRS1YsYO3o5liKEt8+6JppqPo7DBvX+4hYlWH9Wm+M
        Femu0X9i5PmND663jiVD+LnXpOGIlxzX3uu067ZfVQA1Ll6Dt2rcd9Q6SI5V
        nEOXy45P8dzrfo2C23WEQ9vPfo0wg1HxOn7DfuEoWjKA1oaA2IaGMNQVQhIR
        6OQtWu1QnpGPfYHx5hMmP6pXjCxZvZcPExOY6GMQh9njKEwUDwNUjHwSU0gE
        mou0i77mNGneVppsQDMWKNy8ojCD2WGFOVK4c1VhPFBI/qag0e9c2S08Jl+i
        7F3qdP4AoQIWClgki3sFLGG5gPtYOQATeIDVA4wLTAmkBTICMwJrAusCGwIR
        gU2BuID+E+SdSmEvBgAA
        """,
        """
        androidx/compose/runtime/SnapshotStateList.class:
        H4sIAAAAAAAAAI1QTUsbQRh+ZjYfZo111bbGautVg7gqhUIrQhUKgbUFE3LJ
        aZIddEwyIzuz4nF/i/+gp0IPZemxP6r0ndWTvTiH532fZx7erz9/f/4C8B7b
        DF2h08yo9C6emPmNsTLOcu3UXMZ9LW7slXF9J5xMlHVNMIad48HH5Frcingm
        9GX8bXwtJ+7Tyf8SQ/RUa6LG0DhWWrkThmBnd9hGA80QdSww1NyVsgx7yfMn
        oiYrydS4mdLxuXQiFU6Qxue3Ae3HPNQZ2JSkO+XZAWXpIW1RFu2Qd3hYFiGP
        CMqiUxbd2kJZROyIH/DT+u/7Bo8C7z+iEgNG9bB8njsxnslqgP2po6HPTCrp
        I1Fafs3nY5kNvIFhNTETMRuKTHn+KLb66lILl2eUh32TZxP5RfmPjYuHFYfK
        KnJ+1tpQC2W0xSE43cc/GsOfi3CDWFxxWrD7A63vlHC8IWxUYgubhO0HA0Is
        UgywVbkCvK1iB+8ofiBPmzxLIwQ9vOhhmRCRh5UeVrE2ArN4iVcj1CwWLV5b
        rFs0/wH+GL11RAIAAA==
        """,
        """
        androidx/compose/runtime/SnapshotStateMap.class:
        H4sIAAAAAAAAAI1QTW8TMRB99uar29BuWz5SvsqRFoltK04lqgRISFE3IBG0
        l5ycrNW6Sexo7a163N/Sf8AJqYdqxZEfVXW8cAIOWJo3M2+exzP+eXt9A+AN
        XjDsCp3lRmWX8dQslsbKOC+0UwsZj7RY2jPjRk44ORTLNhhDv39ylJyLCxHP
        hT6NP0/O5dS9Tf/BHf9NMUR/cm00GFp9pZU7Zghe7qZdtNAO0USHoeHOlGV4
        lfz3kPTGRjIzbq50PJROZMIJ4vjiIqCNmYcmA5sRdal8tk9RdsAQV+VayHs8
        5B2yqCrDquxV5V6jU5URI8cifsj3g/fNH1ctHjX8tUPqdEKWMmqN9WHhxGQu
        61FezxyN/8FkkgqJ0vJTsZjI/KsXMGwmZirmqciVz3+TKyN1qoUrcorDkSny
        qfyofGH7y69lU2UVKd9pbegJZbTFATj9lD80hv84wseUxXVOu+59x8o3Cjie
        ELZqsounNdYChFglH+BZrQrwvPbb2CF/RJouae6NEQywNsA6ISIPGwNsYmsM
        ZnEfD8ZoWqxaPLR4ZNGzaN8Bn285EWQCAAA=
        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3AZcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsTnW1qSmJSTGlySWJLqXSLEFZSam5qblFrkXcKlzCWDS59eWn6+
        EHO8d4kSgxYDAFRZLFp1AAAA
        """
    )
}

/**
 * Utility for creating a [kotlin] and corresponding [compiled] stub, to try and make it easier to
 * configure everything correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should
 * be unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g.
 * if the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint
 * will not be able to match the generated classes with the source file, and so won't print them
 * to console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 * bytecode for [source].
 *
 * @return a pair of kotlin test file, to compiled test file
 */
fun kotlinAndCompiledStub(
    filename: String,
    filepath: String,
    @Language("kotlin") source: String,
    vararg bytecode: String
): KotlinAndCompiledStub {
    val filenameWithoutExtension = filename.substringBefore(".").lowercase(Locale.ROOT)
    val kotlin = kotlin(source).to("$filepath/$filename")
    @Suppress("DEPRECATION") // b/193244821
    val compiled = compiled(
        "libs/$filenameWithoutExtension.jar",
        kotlin,
        // Hacky hack - duplicate kotlin_module files will cause errors, so instead let's just
        // rename them and hope that nothing breaks (!?)
        *bytecode.map {
            it.replace(
                "main.kotlin_module",
                "$filenameWithoutExtension.kotlin_module"
            )
        }.toTypedArray()
    )
    return KotlinAndCompiledStub(kotlin, compiled)
}

class KotlinAndCompiledStub(
    val kotlin: TestFile,
    val compiled: TestFile
)

/**
 * Utility for creating a [compiled] stub, to try and make it easier to configure everything
 * correctly.
 *
 * @param filename name of the Kotlin source file, with extension - e.g. "Test.kt". These should
 * be unique across a test.
 * @param filepath directory structure matching the package name of the Kotlin source file. E.g.
 * if the source has `package foo.bar`, this should be `foo/bar`. If this does _not_ match, lint
 * will not be able to match the generated classes with the source file, and so won't print them
 * to console.
 * @param source Kotlin source for the bytecode
 * @param bytecode generated bytecode that will be used in tests. Leave empty to generate the
 * bytecode for [source].
 */
fun compiledStub(
    filename: String,
    filepath: String,
    @Language("kotlin") source: String,
    vararg bytecode: String
): TestFile = kotlinAndCompiledStub(filename, filepath, source, *bytecode).compiled
