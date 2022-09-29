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
import org.intellij.lang.annotations.Language
import java.util.Locale

/**
 * Common Compose-related bytecode lint stubs used for testing
 */
object Stubs {
    val Color: TestFile = compiledStub(
        filename = "Color.kt",
        filepath = "androidx/compose/ui/graphics",
        checksum = 0x2a148ced,
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
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcolmZiXUpSfmVKhl5yfW5BfnKqX
        m1iSWpSZmCPE4Zyfk19U7F3Cpc4li1OZXlp+vhBbSGpxCVihDIbC0ky99KLE
        gozM5GIhdrCR3iVKDFoMAMec7K6RAAAA
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
        UC9ZO4QPAAA=
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
        """
    )

    val Composable: TestFile = compiledStub(
        filename = "Composable.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0x12c49724,
        source = """
        package androidx.compose.runtime

        @MustBeDocumented
        @Retention(AnnotationRetention.BINARY)
        @Target(
            AnnotationTarget.FUNCTION,
            AnnotationTarget.TYPE,
            AnnotationTarget.TYPE_PARAMETER,
            AnnotationTarget.PROPERTY_GETTER
        )
        annotation class Composable
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXJJYyhrjRTSMgZwk7xzU/JTMsEK+XjYilJ
        LS4RYgsBkt4lSgxaDACMRj6sewAAAA==
        """,
        """
        androidx/compose/runtime/Composable.class:
        H4sIAAAAAAAAAI1STW/aQBB9ayBQ2ibQT0ia5pu0PdRp1FtPQJwWiS8ZJxLi
        EG3sVeRg7AivaXLjUKn/qYcK9dgfVXUWVKCSpdaW3s7OvNnZeTs/f337DuA9
        3jDscd8ZBq5zq9vB4CYIhT6MfOkOhF6d7vmlJ9JgDLlrPuK6x/0rvXV5LWyZ
        RoJha+Hlvh9ILt3A18tzM40Uw369H0jP9ZcpjSiUFXES2NFA+FI4Hxg2Y2im
        kBQmi+KpEfciwXAYw1tUXM5YqdSaZbPLsB6TYvHhlZDEWuWeF3wWzswRxt93
        UWCelzk9a1atWqvJkLS6bYNOUstFu2yWG4ZlmAxrbbPVNkyre/HRsKaenXqs
        Yn8JsR3PWe6s9A9KO/Bc+06JVq2XOx0lbmzCvJnd+LjhCXUt6+5GKD2prU+t
        E2p92uhZh3rO/xGrISR3uOTE0wajBA0YU0Dvz/rkunXV7ogs5x1DcTLOZLWC
        ltVyG5kfX7XCZHysHbHKZKwIxwwH9f8YTCoFhocLx9u+ZMh2gmhoi1PXo2Ep
        mrOsczd0ibB4xrBElZCk/BWoT8OrKR7iNa1fkKYfyFD8nkAW9/FAleohKbCK
        NQU5BXkFj/CYuE9m3Kd4hufK7CEhUEBRQV7BOjaQwgvy17BZw0tCbCnYrmEH
        uz2wEHvY70ELcRCi9BuuoX9IqAMAAA==
        """
    )

    val Modifier: TestFile = compiledStub(
        filename = "Modifier.kt",
        filepath = "androidx/compose/ui",
        checksum = 0xe49bcfc1,
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
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXJJYyhrjRTSMgZwk7xzU/JTMsEK+XjYilJ
        LS4RYgsBkt4lSgxaDACMRj6sewAAAA==
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
        """
    )

    val PaddingValues: TestFile = compiledStub(
        filename = "Padding.kt",
        filepath = "androidx/compose/foundation/layout",
        checksum = 0xeedd3f96,
        """

            package androidx.compose.foundation.layout

            import androidx.compose.ui.Modifier

            interface PaddingValues

        """,
        """
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGI2BijgUueSTMxLKcrPTKnQS87PLcgvTtXL
        TSxJLcpMzBHiCk5OTEvLz0nxLuHi5WJOy88XYgtJLS7xLlFi0GIAACJwI+tQ
        AAAA
        """,
        """
        androidx/compose/foundation/layout/PaddingValues.class:
        H4sIAAAAAAAAAJVOTUvDQBB9s9Gkxq9ULdQ/YdrizZMXIVBRFHrJaZtsyzbp
        rnQ3pd76uzxIz/4ocVL9A87Amzfz4L35+v74BHCLHmEgTbmyutykhV2+WafS
        mW1MKb22Jq3lu218+izLUpv5RNaNchGIkCzkWrJs5unTdKEKHyEgdMeV9bU2
        6aPyki3kHUEs1wFnUQthCyBQxfeNbrcBs3JI6O22nVj0RSwSZrP+bjsSA2rF
        EWE0/u+THMw58d/tpvK8vNpmVagHXSvC9UtjvF6qiXZ6Wqt7Y6zfu7mQM3GA
        3xK43OMFrngO2fKQO8wRZIgydDIcIWaK4wwnOM1BDmc4zyEcEofuD692uKBp
        AQAA
        """
    )

    val Remember: TestFile = compiledStub(
        filename = "Remember.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0xc78323f1,
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
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3ApcYlkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQVlJqbmpuUWuRdwqXJJYyhrjRTSMgZwk7xzU/JTMsEK+XjYilJ
        LS4RYgsBkt4lSgxaDACMRj6sewAAAA==
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
        """
    )

    val SnapshotState: TestFile = compiledStub(
        filename = "SnapshotState.kt",
        filepath = "androidx/compose/runtime",
        checksum = 0x9907976f,
        source = """
        package androidx.compose.runtime

        import kotlin.reflect.KProperty

        interface State<out T> {
            val value: T
        }

        interface DerivedState<T> : State<T> {
            override var value: T
        }

        private class DerivedStateImpl<T>(override var value: T) : DerivedState<T>

        fun <T> derivedStateOf(value: T): DerivedState<T> = DerivedStateImpl(value)

        interface MutableState<T> : State<T> {
            override var value: T
        }

        private class MutableStateImpl<T>(override var value: T) : MutableState<T>

        fun <T> mutableStateOf(value: T): MutableState<T> = MutableStateImpl(value)

        fun <T> mutableStateListOf() = SnapshotStateList<T>()
        class SnapshotStateList<T>

        fun <K, V> mutableStateMapOf() = SnapshotStateMap<K, V>()
        class SnapshotStateMap<K, V>

        @Composable
        fun <T> produceState(
            initialValue: T,
            key1: Any?,
            producer: suspend ProduceStateScope<T>.() -> Unit
        ): State<T> {
            return object : State<T> {
                override val value = initialValue
            }
        }

        interface ProduceStateScope<T> : MutableState<T> {
            suspend fun awaitDispose(onDispose: () -> Unit): Nothing
        }

        inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

        inline operator fun <T> MutableState<T>.setValue(
            thisObj: Any?,
            property: KProperty<*>,
            value: T
        ) {
            this.value = value
        }
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3Apc8ln5iXUpSfmVKhl5yfW5BfnKqX
                mJeZm1iSmZ8HFClKFeJxBPMTk3JSvUu4zLkkMDQUleaVZOamCnEFpeam5ial
                FnmXCPEH5yUWFGfklwSXJJaANCpg0ViaqVeal1kixOJS4F2ixKDFAAA4Rqdc
                pAAAAA==
                """,
                """
                androidx/compose/runtime/DerivedState.class:
                H4sIAAAAAAAAAIVRTW/TQBB9Yzuxk4bghhbSAKVCQk044FJxQKSqhPgQkVIh
                NVGElNM2XtJtHLvybqIe81s48CM4IKtHfhRinKIKERUuM/Nm37zdffPj57fv
                AF5gh/BExGGaqPAiGCXT80TLIJ3FRk1l8Famai7DnhFGuiDC0UH/VfdMzEUQ
                iXgcfDw5kyPTPlxtdW/UXIod9PvtwzbB/3vQhUPY/vewiyLBG0szENFMEjaa
                rdUHEArNFt/CTH3N3GyuElsDQrHJzLxY704SE6k4OJJGhMIInremc5utojy4
                BJpw60LlaI+r8DnhXbaolq26Vc4Wy2R5Be9zPVs8dbxs4dO+V3Nq1gfas47r
                vt2wXmaLT5dfq5dfipWG4zl+4bHjFX03F9sn7N5s3Z/r4IdRn7DzH6NzH+ZX
                n/d7sTjXp4lZHjybGEKpp8axMLOUj8u9ZJaO5HsVMdg6vhIZKK1OIvk6jhMe
                Ukms2XoLBRBcNsDiZXkoMXqYI5QZr6FyjW/B/l3Z2F7mB3jE+Q0zqqxyewi7
                A7+DdY6o5eFOBxvYHII07uLekLeHusaWRkPjvs5hSWNNo/ILV3pR08ICAAA=
                """,
                """
                androidx/compose/runtime/DerivedStateImpl.class:
                H4sIAAAAAAAAAI1R227TQBA9u3Yc16Spm6ZXyq1QmqQUl4oH1EZBXFQ1UgCp
                iSJEn7aJ1W6T2JXXifqYr+AD+AKQQEg8oKiPfBRi7EQVECTy4Lkcn5kzs/Pj
                57fvAB5jiyEvvGbgy+aF0/A7575ynaDrhbLjOi/dQPbcZjUUoVvunLeTYAy1
                Ym23ciZ6wmkL78R5c3zmNsK90jhUmahvsVbbK+0x2H/XJ6EzrE/UIwmDwShK
                T4Ylhvnc+Cz5OhFyJBUFWi5fT8HENQsJpBgSPdHuugyZ8boU0piZAofNoIen
                UjFsTrZX9F60lnnihvVh+2wuPy5A6rk8zUVMdcVMFineekq7aPGv2UrLD9vS
                c165oWiKUBDGOz2NLsgik2RgLYIuZJRtU9R8xPBu0E9bfIlbg37suGmYfGnQ
                L+jmoG+zHTOjZ/gB2+bPpzOGra3wJ4P+5QeD2/rh6ih9e/k+TZBtcdtc0c2E
                bazpZtLWI4UdEq0xbEz2HNGFq544V6d+GAMPWyHDVFWeeCLsBrS0/sJvkpup
                SM993e0cu0FNHLfjs/gN0a6LQEb5CLSqfjdouPsySpYPh4p1qST9feZ5PklI
                31PYptslQE9KH4+OSX6DHoljGRrFJqLr5ggpkefkrcJXTBc2v2D2U8zLkzWI
                CaoukF0YspDBHBBHv3e1KMpiftTToSyqTBQ+Y/bjP9ulhoRRu2GTBcIWrwbb
                HQ1m/Hco42ooA0t/DKWNIg2bsb+PB+T3ibFC2tePoJWxWsYNsrgZmVtl3Mad
                IzCFNdw9wpTCnMI9hXWFtIrSrMK8wqLCzC+1g+gKTQQAAA==
                """,
                """
                androidx/compose/runtime/MutableState.class:
                H4sIAAAAAAAAAIVRwW7TQBB9s3ZiJw3BDS2kAUqFhEg44FJxQKSqhBCISImQ
                miiqlNM2McGNs66y66hHfwsHPoIDsnrkoxDjFFWIqHCZmbf75s3umx8/v30H
                8BJ7hCdSTRZxOLnwx/H8PNaBv0iUCeeB30uMPI2CvpEmcECE3uHgdfdMLqUf
                STX1P56eBWPTPlo/6t6ouRI7HAzaR22C93ejA5uw++9mB0WCOw3MUEZJQNhq
                ttYfQCg0WzyFmfqaud1cJ7aGhGKTmXmx2Z3FJgqV3wuMnEgjuV/MlxZbRXlw
                CDTjo4swR/tcTV4Q3mVptSzqopylqyTcgvupnqXPbDdLPTpwa3ZNfKB9cVz3
                rIZ4laUnl1+rl1+KlYbt2l7hse0WPScXOyA8vdm6P9fBD6MBYe8/Ruc+LK8+
                7/WVPNefY7O6eD4zhFI/nCppkgVfl/txshgH78OIwc7xlcgw1CFPfKNUzE1h
                rDRbL1AAwWEDBC/LRYnRwxyhzHgDlWt8C9bvysLuKj/AI85vmVFlldsjWB14
                HWxyRC0PdzrYwvYIpHEX90a8PdQ1djQaGvd1DksaGxqVX2k2HnjCAgAA
                """,
                """
                androidx/compose/runtime/MutableStateImpl.class:
                H4sIAAAAAAAAAI1RXU8TQRQ9M7vdLrWUpXwjfqFIW8RF4oOBpkZNjE2KJrRp
                jDwN7QYG2l3SmRIe+yv8Af4CTTQmPpiGR3+U8c62IWpN7MPej7Pn3nPv3B8/
                v30H8BibDHkRNjuRbF74jah9FqnA73RDLduBv9fV4rAVVLXQQbl91kqCMdSK
                tZ3KiTgXfkuER/6bw5OgoXdLo1BlrL7FWm23tMvg/V2fhM2wNlaPJBwGpyhD
                qUsMc7nRWfJ1IuRIygRWLl9Pw8W1FBJIMyTORasbMGRH69LIYGoCHB6DrY+l
                YtgYby/zXrSWexTo+qD9bC4/KkDquTzNRUx1xUwWKd58SrtY8a/pymmkWzL0
                9wItmkILwnj73KILMmOSDOyUoAtpsi2Kmo8Y3vV7mRRf5Kl+L3bcdVy+2O8V
                bLff89i2m7Wz/BXb4s8ns45nLfMn/d7lB4d79v7KMH17+T5DkJfinrtsuwnP
                WbXdpGcbhW0SrTGsj/cc5sLVUJyp40jHwMNTzTBRlUeh0N0OLW2/iJrkpioy
                DF5324dBp2aqzVmihmjVRUeafAimqlG30wheSpMs7Q8U61JJ+vssDCOSkFGo
                sEW3S4CelD5ujkl+nR6JYwkWxS7MdXOElMhz8qnCV0wWNr5g+lPMy5N1iAnM
                oxDbmIUsZoA4+r1riqJZzA17+pSZykThM6Y//rNdekAYths0mSds4WqwneFg
                zn+Hcq6GcrD4x1DWMLKwEfv7eED+JTGWSfv6AawyVsq4QRY3jblVxm3cOQBT
                WMXdA0wozCjcU1hTyCiTzirMKSwoTP0Cc/1T9U0EAAA=
                """,
                """
                androidx/compose/runtime/ProduceStateScope.class:
                H4sIAAAAAAAAAI1T328SQRCeXSh3INUr/gJarVqNSox3Ep+EEI2GFEO1EfSF
                p+U4cOHYJbd72Efin+KDf4PxwRB8848yzkFpY7G2DzezM/PNN/vju1+/v/8A
                gKewQ6DARCeQvHNgu3I4ksqzg1BoPvTs/UB2QtdraKa9hitHngGEQLPcfFbv
                szGzfSZ69tt233N1qbKaqp9KvBdq1vYXxOVms1QpEbBO9hsQJ3DvXBwGJAik
                2SfG9SuuIhhu80F9ILXPhd0fD+1uKFzNpVB29XDllJZ1VwYy1Fx4yn4pkVyE
                LAKUHq4eiUD3LNrysv5ecLyWs6aUC5V/D7pbl0HP7nu6HTCOA5gQUrPFsDeh
                70eHR9jO/2BSR0hEbSx3sedp1mGaYY4OxzHUAImMQYAMMHXAo8jBVecJnnU6
                2UrRLE1NJ0fOIsuIWlHG7Gank0LcnE4sUjQz8QzdJQ59vW3F8tSJF9PWWn6e
                dQwnsTv7+vznNzKdzL4kqGXOPtN4ipq5aFqRwKPT9bIiRNw+aRK4fz6JIRoI
                JKU4EkdmeR/HakAFNgQbqY9Sz5seDzT2NHhPMB0G2LP5bkFdE2OuOHK/OL5q
                fK6T1X0WsKGnveAvWKohw8D1qtxHxtxhz4cVPpQzhTXctBG9EP4HJiQhBjcx
                opCCbfQJrF5Afwu/dYpBOoLO7RIYg9tzfwPuoK9idR1JL7YgVoNLNbDQwkZk
                MjW4DFdaQBRchWstSCq4riCrIKfAVJBXsKlga75I/gEnsHRUOwQAAA==
                """,
                """
                androidx/compose/runtime/SnapshotStateKt＄produceState＄1.class:
                H4sIAAAAAAAAAI1T3U4TURD+znb7w1poqYCAf6gVtkVZICZqCiSGSNJYNaGk
                MeFq2V3Kge1Zsnu24bJP4QP4BJpoTLwwDZc+lHHOtjEoCF7s/GXmm2/OzP74
                +e07gCdYZXhqCzcMuHtiOUHnOIg8K4yF5B3Pagr7ODoIZFPa0nsly8dh4MaO
                l7jllSwYFTcO7a5t+bZoW2/3Dj1H1hr/xlOFazs7tY0aQ/Hvwix0hjuXF2eR
                YcisccHlBsOkeb57pUUJJvVQRsqstPLI4ZqBNPIM6a7txx5D6XxdHmMojEBD
                kUGXBzxieH7JJJe+DE03WlYcue23Bh1zbU8OzQmzcr49cTMrxJo4J3K8cRRI
                nwvrtSdt15Y2xbRON0VLY0pkGdgRhU648pbJclcY1vu9UaPfM7RpzdByepX1
                ezljut9bzZX0kvas31tm21NFbVaZ707f66cfMoahFdOzei5V1BUI3cPcFQsk
                Jub/PkwWZYb82ddh2L9gaxdEhvMfdjvWfiwcyQMRWVtDa7VWuYplHvNYoDP7
                g9HSkWQYafK2sGUcEhl9M3BJFRpceG/izp4X7th7fnIigaO2F3LlD4P5uhBe
                uOnbUeTRgRReCscPIi7atKWDwGUwmkEcOt4WV9kz2wNCLR5xKn8hREAc1BxY
                oUNLg4H+IZTU5ZGu0iI1TNMHOll1iotkbZFWEaP6FaPVxS8Y/5TkPSI5BrX8
                eehYoPx5PCZvapBNqNeBxJo4g26QNZnkKGyLPEY6Xf2M8Y+/YTNJcCGByw8S
                hnADkBvkLyXQLGkGzBAUiMZDmMOcFJYTXaFRgXXKnKGq2V2k6rhZxy2SuK3E
                nTruYm4XLMI93N9FJlLmgwhjESYjTEUo/AJAwj8ArQQAAA==
                """,
                """
                androidx/compose/runtime/SnapshotStateKt.class:
                H4sIAAAAAAAAAKVYW1PbVhD+jm1sIxwQBgKIBEjiBDAXG5ombXBJU3LB5drg
                0KY0TYQtQGBLro5MkzemD33sj+gvaJ+SNDMdJn3rT+mP6HSPLBvb2AZSz0jn
                tvudb/fs2dX473//+BPATRwwjKpGxjL1zMtY2szlTa7FrIJh6zkttm6oeb5r
                2uu2amuLdgCMQd5TD9RYVjV2Yqtbe1qaZr0M7RnN0g+0jCO5us0wO7pUKzg7
                ttRwp/sV6rMMjxKpOyf150ZTqbOCJEh0jpCuLZnWTmxPs7csVTd4TDUMk9Z1
                k/orpr1SyGZJauxMmMlcPhtAK4M/oRu6PcfQU8/KjRDaEJIg4QLD9TMhB9DB
                0HKgZgsaQ/gkJjk4V7DVraz2oQ5erlD/YAdXgpQc3Nh1ldJF112U0NvcKZU6
                AfSTMyrtXtK5LWyfGm1CsipohQaRTDYw9zwwJYOjZ1cJYJDBO1qMiGEJQ7jC
                0Flp0bKaFwZNnpkJKRCH54nFOvZs/C8bCTmRWpxNbZxyrLVKAdyQMCIsC+Ut
                M1NIFy1j2K4ToXVm9k07qxuxvYNcbLtgpItX86Hbm2kWjqVg/qdxMJ97v8Rk
                4/3WKsxbT5t59w5MliDTpmUWbN3QeGzeJBWj4CSaRFngCWUNUhivQ/ZUO0vh
                d72x3LwzFrFFcpFmiY+ynisWdM/MCmKKYbDCObpha5ahZmNJw7YIQU/zAOKU
                89K7WnrfzZ1rqqXmNBJkGGl+3OsCZMfJjjP4SMI0bjLcPmvpiVSGVmQ6gFsS
                botUMtjcawF8SnEpsrWuZjeK+dW3r72aZhg+LfLIOTua7Sq9GD0tDhuHmqVt
                Z2kcW6QAymuW/YoOu06KzzcI49MCY9yJjHNsn4hSuJGSKGT2rs7ngrhXjARn
                OYh5hu7ROhxDmMODNtzBQ4YLET2yHTl2EEtSkYoIuIrJ4dMvb0CoED6D0thf
                xI6XQXebeKSqzJ3nROrVcYYfz30kJ0vkOQ9GVGC6IysItUHBVyVHH5vvOvl4
                YuTMVb+ztPOyZqsZ1VZpzpM78NKXIBOvAB3jvuh4aP6lLnp04T2ZacbeHB2u
                SEeHkqfPI3mCPqctD71u6zluZVosrZce2VOlTnrKEMkpwbAv7FnwxNlVX/Do
                UPbM+GWvQhPvf/V7ZJ8SllvKIn5XRBmUA0q7M9nqvKV4sLjUSg2TJUJuK2uF
                6gFfUJ7J7WWRjmMRWYjMBOVOxdfH4uGZSblLGQuysBQuCfeU+vHe+MWwP+zI
                xbsFbLBvIfDXG3Z06OzRr9yUFQHnokfL2xH+gIsvyZcUf5i8Eb+88P5nyVEc
                VBLykEL4tYqh+ooly8oAw+9/8pCbg/3i/GaaBknNtzdLMUyc7/Nq/FwfL2yR
                HrpcEJ+6bkBWZt0mm5+owLMVebxB+SWRwZLIg5e2RpXMNEr7pV45GHIVzal9
                m6rEvJmhy9WxRIArhdyWZqXEVRKczbSoJZYuxu5k67q+Y6h2waL+wOMi26Rx
                oHOdlu8dl16qy7Wr5RJaJRZKGoZmzWdVzjUaSutmwUprD3WxWb8LsXECnqqq
                Bz6IH309owV+eJGj0S1qyeMIvYP0NPoa7UeQfxMXHQa9/c6aDFNIFOXQiTC1
                eUcmgB9cqSC1/ehC90ncXoHbdwSlFrevIW5PDe4ALrm4w7QqfsF3GHr6GlcF
                JqvAVFyEyzUI1xA5iTBCCKO1CJddhOs1CGOIkgcFwhohiRQYngjH3uDjd7gt
                LPzkCHeqLfTjhmPhcFEas46Fopeghzm9KXxGGsUdx50dJepNCH70WPR0OFkY
                k867SGUOd11jnrnH2hMNf0FUJsL36e2di77FI4ZqNu2IOWyEFe10cgtIOrx6
                8CUWHV49WHJ59WAey2Venzs8urzu/tVcVrDqcsmRaAu1vVVcbvmik2/x2IPf
                y2yEhR0UIRfpS810/mxooXEQ68RIGNOLFJ44jHrLjHpdRqInIsXrcltzuPX4
                6nADCXFnO/qedMb9sB2lLArU/kLzG7T115vwJvFNEk/pjW+T2MR3SfLs95tg
                HM/xYhNXOFo4VI4tjk4OP8cAR5rjGkeGQ+PY5ujieMLRzTHGscgxy5Hg2OGY
                4tjl0Dn2nOE+R5RjjmOJY55jmeMuxwrH6n8UZ3xFehEAAA==
                """,
                """
                androidx/compose/runtime/SnapshotStateList.class:
                H4sIAAAAAAAAAI1QXUsbQRQ9M5tkdY26ftTGj9pXG8Q1UhCsCFYoBLYWmpCX
                PE2yg45JZmRnInnc3+I/6FOhD7L46I8S70ZftC/OwLn3nDncj3l4/HcH4Cs+
                M9SFTlKjkknUN6NrY2WUjrVTIxm1tLi2l8a1nHAyVtb5YAw7x+2j+ErciGgo
                9EX0q3cl++7byf8SQ/hW81FiqBwrrdwJg7fzpVNFBX6AMmYYSu5SWYbd+P0T
                UZOleGDcUOnop3QiEU6Qxkc3Hu3HCvAZ2ICkiSrYPmVJg7bIs2rAazzIs4CH
                BHlWy7N6aSbPQnbA9/n38v1thYde4T+gEm1G9RC+mmBv4GjqM5NIhsVYaXk+
                HvVk2ha9ISnLsemLYUekquAv4mxLXWjhxinlQcuM0778oYqH9d/PO3aUVeQ8
                1dpQC2W0RQOcPqg4NEfxX4TrxKIpB8r1v5j9QwnHBmFlKm7SBarPBgSYo+hh
                a+ry8Gkaa9imeEieKnnmu/CaWGhikRBhAUtNLGOlC2axig9dlCzmLNYsPlr4
                TxVS9+dFAgAA
                """,
                """
                androidx/compose/runtime/SnapshotStateMap.class:
                H4sIAAAAAAAAAI1QTU8bMRB99ibZsKSwQKGBftBjoVIXUE80QqKVkCKWVmqq
                veTkZC0wSexo7SCO+1v4Bz1V6qFa9ciPqjoOvbTlgCW/N/P8bM/M7a/vPwC8
                xUuGHaHzwqj8OhmaydRYmRQz7dREJj0tpvbCuJ4TTp6JaQjG0OmcHqaX4kok
                Y6HPk0+DSzl077J7tKP/JYb4Xy1EjaHRUVq5I4bg1U7WQgNhhDqaDDV3oSzD
                6/TBRdIfK+nIuLHSyZl0IhdOkMYnVwF1zDyEDGxE0rXy2R5F+T5DUpVLEW/z
                iDdpx1UZVWW7KndrzaqMGRGL+QHfC97Xf940eFzz1w7opVPaGaOnEf9Vy5uR
                o/o/mFwyLKdKy4+zyUAWX8RgTMpqaoZinIlC+fyPuNBT51q4WUFx1DOzYihP
                lD/Y/HzXbaasIuex1oa+UEZb7IPTqPyiOvzkCLcoS+Y5UN/9hoWvFHA8JWzM
                xRd4Rti6MyDCInGA53NXQKeeN7FNfEieFnke9RF0sdTFMiFiDytdrGKtD2bx
                GOt91C0WLTYsnli0LcLftbcdUGUCAAA=
                """,
                """
                androidx/compose/runtime/State.class:
                H4sIAAAAAAAAAH1QTUvDQBB9k7RpjF/xu1YQj9WDUfEgfoEXoVARbBGhp7Vd
                69p0I91t8Zjf4sEf4UGCR3+UOFFPKu7hzbw3O7Nv5+39+QXALlYIq0J3Bonq
                PETtpH+fGBkNhtqqvowaVlhZAhGqh839+p0YiSgWuhudX9/Jtj04/i0Rwp9a
                CQWC35X2UsRDSZivrv/VV6yuN5scZ+q9xMZKR2fSio6wgjWnP3LZLuVQIlCP
                pQeVsy3OOtuEwyydCpyyE2Rp4IQ5+K5/U87SDc/P0pDWaMfZci5mQ7fi7GXp
                1etT4fXR8yoFvxAW8xk7hLX6/5tgI9QktoHi6OsrYUOLe3Ob2M/6Zs8Sxhqq
                q4UdDrgcNJLhoC1PVcxk+eJr1qUy6jqWJ1on3KQSbTx+H0Xkh3hVHnjlKDNz
                4MP9zlwsf8YlVDge8Y0x7glacGsYr2GCEZM5TNUwjbAFMpjBbAuewZzBvMGC
                waLJaekDuDg1Tf4BAAA=
                """
    )

    val Dp: TestFile = compiledStub(
        filename = "Dp.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0x9e27930c,
        """
            package androidx.compose.ui.unit

            @kotlin.jvm.JvmInline
            value class Dp(val value: Float) : Comparable<Dp> {
                override operator fun compareTo(other: Dp) = value.compareTo(other.value)
            }

            inline val Int.dp: Dp get() = Dp(value = this.toFloat())
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3ApcAlkZiXUpSfmVKhl5yfW5BfnKpX
                mqlXmpdZIsTiUuBdosSgxQAA2sByTDoAAAA=
                """,
        """
                androidx/compose/ui/unit/Dp.class:
                H4sIAAAAAAAAAH1V3VMbVRT/3c3XZlnKkrZA6KektgGkCVihSsG2UFoQWi2I
                /dDWJdnCQrKbZjdMxyfGF/0L+uCbPnc6OqOUsTMO0jf/IJ8cx3M2NyENkZnk
                fpx7Pn7nd869+9e/v/8B4BI2BU6YTr7s2vlnmZxbLLmelanYmYpj+5npUgxC
                4Ob8urlpZgqms5q5s7Ju5fzxBskUGZllc6VgXZk/xNP45LiA0ewohrDAsVbO
                YogKqKuWv2wWKpZAKN0/IxDZrO7EjA4NbXEo0AXC/prtCZw6NL5AZy7wbi25
                Q9nRy9n1x5fIYXpmpn9WoGMfw0zBNQlYQiAmDXQcQ6eGozhOsczyapbsXH/N
                KtM2TeY6DD5X0CvQ7ruLftl2VofsYqkgcJwUGtiqnhGY7mbZ9YpdyFvlGE4L
                RK/YhHoyyHpZx1m8o+EM+gTi06V0QMGEinOkZ5ZKlpMXGEofjHEwrAwxruM8
                LrDHtMDJVvgaFQdYcZAVpw5XHGLFi1S0GgNU2XSL3HVkMcy6IzpO4hTzRoVo
                XzO9tSk3b0ne1NpexxiSTP7lgA4ie5T3CojENutpxSx40qQrPXOwU/sfCGgV
                Z8V9Fmjp+Bhxtr5K5Q2YvPOEcb5FQtABhPM6pjjwtMDpDdcv2E5mfbOYsR3f
                KjtmITPrcEKenfNioN5UqVNuMB6BC+lDr0wdmo5bmNVwE3MCiYMKVOBqgtxG
                re0nMMnZ3Kl24rKGCLeJkXMdzy9Xcr5bluTwMYOsESFwlpM+7MZw433G3r+g
                C0cPhd7AdlZeHWJXKQ3zMELdWb9g/wM4uClB8b6mcs03kDq3WZx1aGPxPa0d
                LFi+mTd9k2RKcTNED5bgIUZwNkj0zOYdIVHyhODv3a2LmtKjaIrRoe1u0RTT
                FDVCs0pzmOY2+vOBGqXFEZoVde+7qz27WyNqIpxQsrtbWXE9kYgaSq+SDb3Z
                Ebtbez9Fw2rYiMydNlQSxkeihtbLmrf2niukIfa12gx9rtdop9MjIypphXtE
                tuPWm+eh4NQwOucMI8E+SCYC2VHjGMmOk6yrLus2eu52VgHQXqVEesNq1Ijt
                fS+UaqxvFcpDTWoRde/H01nB2RP1oHpMly5u+FRpvjX0os0Tm7crxRWrvMQP
                KneYmzMLy2bZ5r0Uti/6Zm5jwSzJfXzRXnVMv1KmtbboVso5a8bmg+TdiuPb
                RWvZ9mzSvOY4rm/6NjUahqmgEUJAXwsk+F2msnSgEyriJHFpl6GZQCIy8Cva
                X9JCQYnGaFWIpzTqcn2ETEGG9KRK449IW2HtvtQOul40WUcD666qBrqD4Lzq
                oRUHpYaTfiaknziDIFcnDnMVl0B4VXUV59dKuhojHbZIvsaZ+6+QSry7jf6+
                bbxn9G8js433f+ZmbcgrKZEJfvykk/OSFJXx7OCDZhu1zsVoPYdUjci+HXz4
                oskgUg8yRqS1DHKl2WY/CD0l0uYeZcdXKzX4J5QfEAm9GNyFso1rNyjqjXP0
                38EngTxcpbDMtxJK/B90Kw0cpurlSBGH8wGSBdyWUYYbyzG4g0/3obUqAZkb
                Cr9I0nxSmmsDr3B3IPUb2n9p2VdVX1rdlxY0KJdzEUvS11lJktL3sokepdrO
                RhKfY1lqXyBy+Cz+Gsr9vle411y4OO4HRp38sWkuXO0GiBZdn8QDPJQGl2R+
                OnOeqnLezJCOLyXBOr7irIxreITH0sPVmocBWb5tmM0tH37LW40jXZYrBC/Q
                D8EPZgcVmr+h1QrNOcKdf4jQLKxZPKERqzyszcLGOqXhYQOFh0h66PBQ9KAF
                46KHJQ+qh7iHR4Gkx4PhodPDQrCl34SHSQ9jHkY9Ns8GwpMeTv0H1Ho+rbgK
                AAA=
                """,
        """
                androidx/compose/ui/unit/DpKt.class:
                H4sIAAAAAAAAAH1Qz0/UQBT+ZtrtlopQUJBdXH9gD0CiXYwHg1yMm00aV02U
                cNnTbFtx2LbTtFPCccOBP8Q/wDPxYDZw848yviGcmcP3vve9N9/Me3///f4D
                4A0Chp4okkrJ5CyMVV6qOg0bGTaF1OGg/KjbYAz+iTgVYSaK4/DL5CSNSbUY
                WsepHpQM9na0M2TYvMOnjTa5xKqoddXEWlUvZV5m5upwZ7iIBXgeXNxj8AIZ
                fA9ujVnE4Ab6h6yDhNKV0VTpTBbhp1SLRGjxjoHnpxYNwgzQE2xqCCf9TBrW
                J5bsMezPZ0vefObxDe5x3/W4y7c6/nzW5X22y/v87fUFv75k89nVT8fp2q7l
                21fn3Ka+jnF4bYB+0BvdMSP9BrSUQflqqmmyDypJGZZHskg/N/kkrQ7FJCNl
                daRikR2JSpr8VvS+qaaK06E0SedrU2iZp0eyllR9XxRKCy1pedgDhw1zqA0t
                OBSfUnZAkVN0rYPuxSUWf5kt4Bmhc1NZwHPi68Q4KfexRCp1Y5mY6dy6wSd4
                QXGfaj55r4xhRViN8IAQDyOsYT3CI2yMwWp6vTtGq8Zmjcc1erXhzn8XAJq+
                VwIAAA==
                """
    )

    val Animatable: TestFile = compiledStub(
        filename = "Animatable.kt",
        filepath = "androidx/compose/animation/core",
        checksum = 0x68ff47da,
        """
            package androidx.compose.animation.core

            import androidx.compose.runtime.mutableStateOf

            class Animatable<T, V>(
                initialValue: T,
                val typeConverter: V? = null
            ){
                private var internalState = mutableStateOf(initialValue)
                val value: T
                    get() = internalState.value
            }

            fun Animatable(initialValue: Float): Animatable<Float, Any> = Animatable(initialValue)
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3Apc8ln5iXUpSfmVKhl5yfW5BfnKqX
                mJeZm1iSmZ8HFClKFeJxBPMTk3JSvUu4tLkkMDQUleaVZOamCvEH5yUWFGfk
                lwSXJJYAFSsxaDEAAKFdFhZ2AAAA
                """,
        """
                androidx/compose/animation/core/Animatable.class:
                H4sIAAAAAAAAAI1VXW8aRxQ9syywrAEv1EkwidvEcRrATtZ20tYNlNZxGgkF
                ksimqJLzsoaNswZ2rZ0BpS8V6m/oS1/7C1qpUdo+VCiP/VFR7yzEMYZUfti5
                M/fj3DP33oF/3/79D4C7+JahYLkt33NaL82m1z32uG1artO1hOO5pPFtczs4
                WgcdOwrGUCrV71WPrL5ldiz30HxycGQ3RbExQ1eeVjEYZ3VRqAyRkuM6osxw
                MzcdNK3JNxhiuXq9WG8E+5Wq5x+aR7Y48C3H5XQD1xPBFbj5uNfpSPKUO5TL
                N+KIQNcRxhxDQvxwbO94bt/2he0zpKczxZFAMgYF8wy5qUr5PVc4Xdvcc61j
                /sITe5TUfkRXSjEku72gaIHuyXOG4oyr5asfxKydCicaH2FBRxoXiLXjElvX
                6gQmKtm5MS4hI6+yyKCKFw5nWJsO/WDzqX5x2STH6jSsTo8SN87Vq0q17YmO
                45pH/a75jrr5wH5u9TqCis+F32sKz69Zftv2i6MORXXi+QlNy6Et6pNNWsjl
                Z81VOJenYWDQKGLMT6rqpLpxrvpE8WkcK1iew03kaFYCtBkFmhVbojxlcr/+
                f3PoCTmK5JV6V5GaLawWVZd0SrcfohfJ5BJlYG1SvXTkaZ12rQ2Gn4eDjK5k
                FF3R6DOGA9qoY0U4MxwUVG04MBgJZiibynro/mI6YqhZZWs4SOuaYoSzaoZt
                sTe/RhQjsrtgRLNaWk1L87r2/ZufktKgDwe7F0556pRpLqtqMUPfTRnxAGzr
                IRkipEwYuuS2SXTr9NE7BBW9Pyp+4v3k3G4Leq57zqFriZ5Ptsu7oxpW3L7D
                HfLYfl8nms0dr0VO81XHtR/3uge2X5co8nl6TTl8viPPY+XKWaynlm91bRqV
                CdAE9anZrlnH4zB9z+v5TfuhIw+LY4zGFBts0ByGqQsKvT36JSBZCk4FfEUy
                QheOBWd6Wie21RPbGkmVJA00QijT6RnZZVfThdeIF9ZewSis/omLr5D9PYj9
                WtooRsar0AhdR4r235Dm6igSl3EFCHZLxIgFu9PcNGyTjClyniQ9QxL4mPaS
                QIlA5IXml8I//oIwqxVW117j6ij7fVpDYNoEjQgBarQmKUkK17A8vopJiDJ5
                uPAHjN9O2EcCpRYwjo8cxoxH7K5PVC4lnxxpy8Hf0QhQJ8DsX8gznEWNn0LV
                J1BvEKnRLoSdQBbxgOR35HuLGNzeR6gCs4J1WrEhl80K7uDuPhjHZ/h8H0mO
                KxxfcGxxfMmxxJHgiHJc4shwXONY5tLnHsfKfxpFtNlABwAA
                """,
        """
                androidx/compose/animation/core/AnimatableKt.class:
                H4sIAAAAAAAAAJVRTW/TQBB9a+eDuqFxw1ebAoVyoRJi08KJIKQKKZKFCRKt
                eslpE6+iTezdyl5HPeYncUQcUM78KMSsGykSXKi1np33/GY8H79+//gJ4C2O
                GF4JneRGJdd8YrIrU0gutMqEVUYTk0t+VkExTuUn2wRjCGdiIXgq9JR/Gc/k
                hFifIdjoGPjLwXH8/4n7DBe3i3gfb4oYpEbYfvx3Vf0PlPZFbPIpn0k7zoXS
                BSXUxlYZCz40dlimab+FOhoBPGwxtJRWVon0UqQl9cEGDLvx3NhUaf5ZWpHQ
                7ymtly18GiBzpkmyuXM84q+V83rkJScMb1bLMFgtA2/PC7ywSS+B1bLbpbu7
                06l1vJ5XWb/HThthrUvYhZ7SWm43PjDc3RCv55ah9tEk1EI7VloOy2ws84ub
                5XRiM3Ed5srhNbl1rqZa2DIn/+Brqa3KZKQXqlD0+WwzNVr0uSnziRwoF7a/
                ll7+I8QJTbQG95DMjRg+DghxwlQw6kffEXxzc8Njso2KbOMJ2daNANvkAU8r
                TROHa9WdCj+rbBfP6X7n2if9zgh+hHaEkCx2I3RwL8J9PBiBFXiIRyPUC3f2
                CuxXZ/sPX0TSqAsDAAA=
                """
    )

    val IntOffset: TestFile = compiledStub(
        filename = "IntOffset.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0xfd7af994,
        """
            package androidx.compose.ui.unit

            class IntOffset(val x: Int, val y: Int)
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAGNgYGBmYGBgBGJ2KM3ApcAlkZiXUpSfmVKhl5yfW5BfnKpX
                mqlXmpdZIsTiUuBdosSgxQAA2sByTDoAAAA=
                """,
        """
                androidx/compose/ui/unit/IntOffset.class:
                H4sIAAAAAAAAAI1QTWsTURQ9781HJuO0mUwbTdOqtVZts3DS4k4RVCgMpBZq
                CUo2TpJpfU0yI3kvJe7yW1y7ESyCCwku/VHifZMgCAWFmXPvuffcj3d//vr2
                HcAjPGDYitPeKBO9SdjNhu8zmYRjEY5TocIoVUenpzJRBTAG/zy+iMNBnJ6F
                R53zpEtRg8F+Ikj6lMHaiaLdFoOxs9vyYKHgwoTDwCb0Rx5cXCuCwyP2wcPy
                nJUYTPVOSIbt5r/XeEzqs0S9zodEc/KGodzsZ2og0vAwUXEvVjHp+PDCoAcy
                DQUa2afQRGjWIK+3x/B2Ng1cXuUu92dTlz7uOy53rOpsus8b7HklsH1e4w2D
                rKntj482963j8jxKzKGqmunYfoGC5t9Bxy/oOfuMdoD35wUP+4rWfpH1EoZS
                U6TJy/Gwk4xO4s6AIkEz68aDVjwSmi+C7qtsPOomB0KTteNxqsQwaQkpKPss
                TTMVK5GlEnt0TpPexxHoW5MX6CuTNWgFCzbhFrFDUug7lOpfUayvX2KpvnEJ
                /3NeepdQC0Gttgk351KUqRlyT7dmuaebc8qsYHXROiSrc1b9C5Y+XdnQmwsW
                DcuoXFns/08xx70c7+A+2QPKXafcjTaMCNUIa4SoaViPsIGbbTCJW7jdRlEi
                kNiUcHNclrAlViRWJSq/AaJQvyMZAwAA
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
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): KotlinAndCompiledStub {
    val filenameWithoutExtension = filename.substringBefore(".").lowercase(Locale.ROOT)
    val kotlin = kotlin(source).to("$filepath/$filename")
    val compiled = compiled(
        "libs/$filenameWithoutExtension.jar",
        kotlin,
        checksum,
        *bytecode
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
    checksum: Long,
    @Language("kotlin") source: String,
    vararg bytecode: String
): TestFile = kotlinAndCompiledStub(filename, filepath, checksum, source, *bytecode).compiled
