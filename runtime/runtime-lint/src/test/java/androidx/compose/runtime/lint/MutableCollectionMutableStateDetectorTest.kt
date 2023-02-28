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

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import androidx.compose.lint.test.kotlinAndBytecodeStub
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestMode
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/* ktlint-disable max-line-length */
@RunWith(JUnit4::class)

/**
 * Test for [MutableCollectionMutableStateDetector].
 */
class MutableCollectionMutableStateDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = MutableCollectionMutableStateDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(MutableCollectionMutableStateDetector.MutableCollectionMutableState)

    /**
     * Extensions / subclasses around Kotlin mutable collections, both in source and compiled form.
     */
    private val KotlinMutableCollectionExtensions = kotlinAndBytecodeStub(
        filename = "MutableCollectionExtensions.kt",
        filepath = "stubs",
        checksum = 0x90938fc8,
        """
            package stubs

            fun mutableList(): MutableList<Int> = mutableListOf()
            val MutableList: MutableList<Int> = mutableListOf()
            object MutableListObject : MutableList<Int> by mutableListOf()
            class MutableListSubclass : MutableList<Int> by mutableListOf()

            fun mutableSet(): MutableSet<Int> = mutableSetOf()
            val MutableSet: MutableSet<Int> = mutableSetOf()
            object MutableSetObject : MutableSet<Int> by mutableSetOf()
            class MutableSetSubclass : MutableSet<Int> by mutableSetOf()

            fun mutableMap(): MutableMap<Int, Int> = mutableMapOf()
            val MutableMap: MutableMap<Int, Int> = mutableMapOf()
            object MutableMapObject : MutableMap<Int, Int> by mutableMapOf()
            class MutableMapSubclass : MutableMap<Int, Int> by mutableMapOf()

            fun mutableCollection(): MutableCollection<Int> = mutableListOf()
            val MutableCollection: MutableCollection<Int> = mutableListOf()
            object MutableCollectionObject : MutableCollection<Int> by mutableListOf()
            class MutableCollectionSubclass : MutableCollection<Int> by mutableListOf()
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAGNgYGBmYGBgBGJWKM3Apc0lkZiXUpSfmVKhl5yfW5BfnKpX
        VJpXkpmbKsQfnJdYUJyRXxJckliS6l3CpcbFWlxSmlQsJOtbWpKYlJPqnJ+T
        k5pckpmf51pRkppXDGQUA9XxcbGUpBaXCLGFAEnvEiUGLQYASQTUpX0AAAA=
        """,
        """
        stubs/MutableCollectionExtensionsKt.class:
        H4sIAAAAAAAAAJ2Wa2/bVBjH/yc3O26aOEkva7q1LL2v7ZyNXSgNZaVsLCzp
        YEWTUF85rRXcJg7KOanGu34WPgHQF0ggoYiXfAW+C+Kxa9nOWYI2pNjnnOfy
        P7/H56L89c9vfwB4gFcMS1z0m9xo9IXZbFsH3XbbOhF213n6RlgOpw5/IRQw
        Bv3MvDCNtum0jJfNMwpSEGeY6Fwn1m0uGArrG3UvrC/stuHadhnW3jJW66FW
        zRFWy+rt7lHkUr3baxlnlmj2TNvhhuk4XWG6NNw47IrDfrtNUcVQbL/XM39w
        FRWkGVJV27HFHkN8feN1BhPIaNAwyZAdnl5BjmwtSzSi7BNDI12uI4MCimnE
        MMWg+UUfWRSZHyqPTIS4KtvGVTwbncU5t06fm/w7ilcwp6Hksk8O6Si4SaYQ
        3SPQooOcBJPBIj5wuW+H3A3ze5mbTISzI9tGcb9PKSShYFXDmlSKZ98YKsWD
        0qKDnMSXwRa23VLuEn1H3rAMN4boQw/BbY9xjVuXqVHBCj4kT0gcnTs/wjYz
        miaDR3jslvERg1o9afubduUdD8nyu+2sx/97ITff60vl6+ddQTUYDUuYp6Yw
        yRbrXMTpfmHuK8nAzt1OjOxvbLdXod7pPYa/B5er2uBSi+mL9KhaTI1Te9tv
        V/x2idpUKaPnS2ohUYhVYhVGo0IwSpUKerGUvR75No0ipoKIbHlWH1yG6X/+
        mFJp3lJCjesJ8iVDrdCl6Gr5lp6WhcOACT1DuZPhLKErp+tuifcZfQMs/MfV
        evecTmz6yG45puj3LIb5V31H2B2r5lzY3Kas/fAKZEgcdE8t91zYjnXY7zSt
        3jeuMh2bo26/d2I9s93BnK/x+i0F3KNdl6AVSNDdQtcZjeruymAXDWpTRKtS
        W3KvjMBXlXx0BgPfJ5KPNnbg2xvypTGHJI3iOKTRBvnp42Ayvvs7tG9/RXYA
        /Sd3y+AlvTVyu5AZeq7FlCGxOeQDqZueEEX/gulQIeVZk2OyZzA7CqREIPMD
        3JJB0gSS9qVuSFILgVQEpCyDaGOyl7A8CmSNQNYHuCODZAkk60utSFKbgVQE
        xJBBcmOyK7Q1rrO3fBA9ujQDPJBZiqRV9NXuS2oPA7UIy47MMjUyW8XHVCXz
        soXP8iTKcoXp6GJdoRz9ZFcwJPAr7PwczFz25kjSDEni1zBPzwL17tCzTURV
        emL4yuN6ga+pPaPMTyn6yTHiNezX8Bm9cVDD53hawzN8cQzG8Ry1Y0xzPOL4
        0vs9pMPGUfD6eY4Ux5bX3+RY5lj0+gscsxwVjiSnv2GY+RfzLRDjlwkAAA==
        """,
        """
        stubs/MutableCollectionObject.class:
        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
        A/Z2yETWRM7Ehf8A1Au5SlEPAAA=
        """,
        """
        stubs/MutableCollectionSubclass.class:
        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
        PrD+CPEOAAA=
        """,
        """
        stubs/MutableListObject.class:
        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
        /x8chhqA3RgAAA==
        """,
        """
        stubs/MutableListSubclass.class:
        H4sIAAAAAAAAAKVYi1sU1xX/zSzsc5BhFRAw+MK4sOoSk5pGiAaNJquoUSg1
        0jYddkcc2AfdmaWaNgm2TdMmfT9sa59J07dttU1QkjYl9vn1j+rXc+5cht1l
        FvczHzBz995zfud3Hvfcy/73f++8B+AR/EdBl+2Up+zUqbJjTOXMUct2xspT
        mZxh2yEoCozRGWPeSOWMwnTqzNSMmXGG3JmyY+VSLD5cIZEuOOa0WRo6NDQ6
        W3RyViE1M59PWTRbKhi5VN4ozZolO3WywtqQAr3WRAhNCjZUmwkhqCDRKGoI
        YQXBYatgOYcUBBL9ExqiiEXRDE3BxlXokVLJuOJqbIiilVe1vr6smTOnDcd8
        bpDY1fg7pKEN8QhUbFTQ5FyybAU9o3WjSP4FjGyWRBPp/gsK2tZEK4QuBaF5
        I1c2z1xU0EFyPiHV0IMtUXTjAQXtibVZ6b+goRNbYwhhG8GRA3mz4ChQ0gqa
        E+l0/4SA9lGcYMVdrPggiVqFrHmZYkecR3I5qo9Ehf9Hi7kc6VjFwhD7kvJf
        G076lgRr7BwtlqZTM6YzVTKsgp0yCoWiY7CSnTpddE6XczkKWFiyt8PYq6DX
        L+kEWyIAK0NlmqKIZC6ZmVmJ8IxRMvImCSrY7ROpipkxBpkWIXgI+6MYxMMa
        +jHAwfgQxcvfdQXdK4H0i8pgncW6YWGTH2aTByn+mZxplDQMI8Yzj1MwMsWC
        w8HScNjN7xMKYiuTIkf7GsiDdN61d8R18UmqzWnT4YKqqjkpq+E4nmK5p6mg
        RF1wfXIdp+vUYFrDCYyyyilWsY/l55wrYv+R0TN4hlfOkksWJcdwiiUuyf4K
        7mk5TyFO+i74NxsNYxhnbKrxGG06J+2S1XDeZfMs7eocbcq0Z7e7Cn60Ym2I
        67reYj37k/gY2/kEtQIvlH7Ig/VX60N/kqGnaE+WzHxx3uSg+chy6fWtt71o
        Z3BzIsisW0fT1Al9gBREXENUWxpm3FKZpazJWYcBRF0UhCjXoRCdc0U/RQm3
        uao6ue34Gtjt24j8KtBGmTHnqZzs8hTHS8GmVdzVrqyg32e6XlAv4wrDfoYc
        uFgq5tNu0ws5xZURbYsx63lT1C5Ve5NNHzRcxQKrfa7qzDpdzk9xF/8CF3bB
        meBGruGLWIjiZbzCzXVzlE6LLyuI+3nYiT5e/gpvbyH4Nd53W3j0Dd5OJ3n0
        LS5lMfoOR9/k0TUNeVfu+3wuvMILQuSHHDaHRz/W8BLzUPFT4Z047egYTUz6
        hN9nTsG24fGDPl0kMTk+Thr0oJZlMGoYb1Jm/NpQCL9UsMuvia/KjLvUQvi1
        gkN1TpwGOWt4Hb+N4jf4HZW3r0/tCX9fk/WOujpW/shWbpJvDRUdHf0rl6JT
        tGeyhmPQnJqfD9BVTOFHM53WtM/UyxZ/oquHmn1IUY8uLxyIqpvVqKpvp79w
        VA030zsWXV6gF02HA/TupTct6f30ZtEkvaOblxf2h+NNcXVQHVSONN99I6jq
        gRNteqhbHQzvD+oReqtP372mnOgSc1Gai/Fc1dpWXZPy7pqmt3SvoG4QEp1S
        Yu1KRG9lXDFu0/UauwHSbKuj2aHHJZPYirTgv5FwNq3hGNHbGUeMW/UOD6tT
        WGnTN6/RaNW7PKlu6UXFTIVd5tJTGzHBZQvpPFCXf28Nf0Xy79S31tHp1bdV
        6lRGSuhup1zs8HQUWt8p5fpWbJyL6008Pn/3Guc72N0UbtaDXEr7FSoz9Mor
        6mp5H7vsmHSRoiNi3yw118iYNV0wnHKJel/T0WKWXq2jVsF0u9w463IjK2aM
        3IRRsviznOw7Vy44Vt5MF+Yt26Ip7x42snoM0QFZK1a12jLmGJnZU8acBI2O
        FculjHnc4g9dUnVijSJd31S626vUnCNQ9Dhf0cnbv9E+UvEn7KQxXf/puUwz
        RxGgWaB94DZaBgLD76L12dvQlxF6G5tuCZX36RkVQiGCCuEujTRSJyX66eAd
        y31dAh4gSZbdMPAWNvXcQe8Stqu4IaRYs8NdlZo82iEoMUafxBghUrztWwXG
        FgGyO4BbHsg2d9kDaUVCgPCI4VQa09WVpBjuMUmpLRnfdwePMGhyCQfWsmrz
        ANuwF496QUvi7+w9k9wjVOiOKrGflFTjezzsnj1LGApUgLts4x543GMbF2ZU
        aeYxYSaiCkXX0LCXqISIEWWCTSyB/pNbDYebjaiXjcPrZWOksWwcuUf4jjUe
        vqO14aMjnabX8ltCmqe7b67Dzw3dP2gc9PDoYlDf39ON+Us3cv9In1Mq9Gsj
        TXdtGaUarY8quFlHyw3KR2p8OL+eDxca84Fu3v5snrsXm4/XsKGLtkR6WLKJ
        yQxlVNSmJ+ZRicn0uKBGDSjd1WTaq0CpmC6t9a8SdAcsWvOK6KIQnblHgeY+
        QIHm6xZo8b4KdO4eXEsfgCtdbyVXrxdVtM1PB6r5rteL3M5ZzZz+OZDMD0l0
        zUVfwmcDqMXVPFwNL0hcDS8KXJf+8zX4dCX333afr7vt9BH+ckdqPSHjqQ0k
        l/HyIr60iFdvYOXYCgq9UEVYKxnuwGt0PjLaLol2UvrYMtCzZwXuq7d84VyH
        Wzy4Fi+QLQJYxVn+jkICn5M025Pvo/s6mgM3Vvl+nfkG8E/xVCJ1iLd7lto9
        4sfxlMQ/KPGDAz2L+ObNdQIQ9HCCkvFZ/p6iDk+1gue3748n4Z9vCP+7942f
        9eJ8RuLHq+LcvYjvNQReuS9WwVca8+MSPMJB/sEirq9XaBEPKuLGmfKV9fJV
        BfUj2qnrpcwPykZ5nZr9iT9cIzV71duQKbkhmwcW8TN/V9192ewdh6/jDdmL
        HpUOtibjv+A+t4xfJe/gRm3jrLzCvYnfe13u5wLsD5LJg5JJmGHu4FbtWRaW
        KAGpHcC/xPs9/Jve10nmz0TyrUkE0ng7jUV64jY/7qSxhHcmodh4F3+ZxC4b
        HTb+Kn5jNq7aeMnGgo1OMbNDPBM2+m3sFeNhG4dtHLFx3MYWGydsnLExJpbO
        25gUg6wN08aMjbyNORs29Wsbl228YOPF/wOZ1FsqfRgAAA==
        """,
        """
        stubs/MutableMapObject.class:
        H4sIAAAAAAAAAJ1X63cT1xH/3dXT6wXW8hPxMAkmtqUkMpQkEBs3xnWMbGP8
        ICbBeXQtL2atlUR0Vw7kSfNOkzbP0jYf+5kPcNqSkJyT49Jv/aN6MrO7liV5
        cRyO0d65c2d+M3fuzNzL//7/w08AjuNbgQ7pVJZk5mzFMZZs86xx5dzSqplz
        YhAC5alVY83I2EZxJeOxBz1OxbHsDMkO1Qhki465YpYHA1jDg1P5kmNbxczq
        WiFjEbdcNOxMwSjnzbLMTG4aHxTQG23GEBbYVWc3hqhA7w4xY4gLRIesouUM
        C4T6+hc0qGhWEYEm0LkJPGUV8+byGUNedrV2q9jDElpPz7JpmyuGY74yILCn
        PgSDGlqQaIKCVoGwc9mSAl1TwUGl3UVytmmUNXSiuRkxdAk050pFx7CKctK8
        Rgh92f6LAi1bghjDfoHYmmFXzHOX6NhILiDSGg6iW8UBHBJo79t6fP0XNSTx
        MJs+TLHIs0mRpfBuOLHABjT0ejJ95LBrkmRXTEegZ6pUXsmsms5SmaUzRrFY
        cgzHKhE9XbFt3i7t8pEg01tZGtJ4jO08Tluz5FjhinPNPSHycgBHeeUYza9U
        yHJnXzZwxwInAozt0PxxPMlGnqL8ICMjti2Q6Gs43/4FgaEG5lA6wJMg3rCr
        fni7qJUcDhztI3ypXCrEcUrgYFBmE2SZlK2cjOG3dLq5y2Yu72vPGGWjYJIg
        VcX20ZhnkJVBroERnFbxDEY1nMTTHIYxCkPZLJTWKAHGvYM5I6DSwY+xbZMy
        u6WvvyYS8yan9Ewjb6g+WD2sfW3HrWKYIOMmqxCUhrOYZEemKUPIEaoRyekV
        bLEBibZDCe6izHko8x7KvPW66eYZZX5Y0kTDBSywwPMCTSTgVgEXcp2h0ZJt
        Uzjp2Aj6sfss3dcVt46khpewyJZerut005XCEtf472nvdOJ+FS5hQYWBHNfs
        PpVaDHmdCMrjXm/5cvDxBzeKNDcKBXmmVpkqcD08wVSJz99dfZUpd5U8n8Ak
        UxUNMx71mobn2EUF1zRcxCJTb9AOhnK22215Tg00np2ePz8yPTqm4R10cKu8
        LvDUA94klIMbl8lZ0zGWDccgnlJYC9GNJvgToZaWJ9ZVi2fUsZXlo0J8tX79
        hKp0KaqiH6FfXFXiTB+mXy/RNNe7aQzRqKjr12lopqlKYw9N4//9UOlav35s
        dyKcUAYU7zsgTsfiyr1/RBU9NNGkJ5LKQOuZezfERIveRnT7sajeQaPi8zp9
        XleV16XvTYa7xIBSIxly8ZKEt4/lXblD+v56uVoUV/7ARLd+kO0fa9O7k3oi
        Ghc1fh7yrT20xZqnnZzr0cPJtoSeqN9fdCDy/L0bEZKJJsPxmB6f69KbknF/
        tXZNpbWE3syoNVxN30Uau6sae2rWdL2FT4b6O6gozY0GE867FX7Qvzo362rs
        qmNS96OW+XieroN9c5WiYxXMbHHNkhZJjmz2VKrheWulaDiVMhVMeLS0bPK1
        bRVNr8zOMzJXUiln2AtG2eK5z+xpxK221joDu+YdI5envPXV1PlSpZwzn7V4
        stfHWNjiGY5S8kfoFwOXwV6uB9r+l5SnURqHAD3BzwmivyKegndJRuHXCnG+
        Js4oQjQH2lPfYVcqNPQj9rzwHfR1xP6Nttuuyjf0VV0hnaB0/IUojdRJif46
        uEr49eED9tHIa2rqX2i7i70Ct12JTS21qkVNyNd6kvDZwm7W2vc9HrqLHgU3
        q5od3qqvydQRPOJa6t0Oo397jJSPQZ2L1hjjpI/RsomRUbCOA7caYFqqMC2+
        Kxsxf9SVpNdGcEB+I2pcagwItUvfkXE6l5B7Lr4j+11nToTqnTnkCVWdafed
        YYp3pzS4RRczadTtNJ0Y/h6/YzPpu3hWqTmurTs9VZNJg7hBY0x4aUbf8e2i
        mH2gKNIF4bvbEMVzAptY9VH0vJuqrQH6ztwP6fwvIc02INEFFXyyL2x3snSZ
        Bdt/5Zfsv1hnf5Zf2779OT/M7en/4MC3iIRuptLrMO5g+Q4usSsh/NX9iia3
        iKMuul4T//rEWaH1WX6p7wDf+tX4KR+fHgf3wRe3NvHtWw/kvz7C/wPw8Z/2
        8aOp9B0Ub2GjmW3FiVZxolUcesr7OLN+LbZuuPfohpdXgkG9qmytgrb6wWWK
        w8BJNb6jMJQfOAzjwWFwflUYyE96LvtVnfETN5K6g7VgGC9/Iw2VOMuv5QCQ
        qzsHmaW/C9XCqwV5/eYOQGb5kRzgwZs79+BFxPFW9ZbrdOMFNP8Ihe7Lt/+J
        P9x2GSFfPIS/ueMX+DuNX5LaewT4PjWBLD7I4kP64iP+fJzFJ/jjIoTEp/hs
        EQ9LdEj8yf3XLPHnDXpCYlLic4kZiQsSz0ksSLzkLl2UWJTodOmk+z0i0SuR
        kkhLdEsMSByXeELipMQpV2D8Z8BtlfOyEQAA
        """,
        """
        stubs/MutableMapSubclass.class:
        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
        yzD2KyjpAQxSEQAA
        """,
        """
        stubs/MutableSetObject.class:
        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
        j/0mBkycMDFiv46bOG0ibWLqfxuJKgUvDwAA
        """,
        """
        stubs/MutableSetSubclass.class:
        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
        kXLPDgAA
        """
    )

    /**
     * Extensions / subclasses around Kotlin immutable collections, both in source and compiled
     * form.
     */
    private val KotlinImmutableCollectionExtensions = kotlinAndBytecodeStub(
        filename = "ImmutableCollectionExtensions.kt",
        filepath = "stubs",
        checksum = 0xf50711c2,
        """
            package stubs

            fun list(): List<Int> = listOf()
            val List: List<Int> = listOf()
            object ListObject : List<Int> by listOf()
            class ListSubclass : List<Int> by listOf()

            fun set(): Set<Int> = setOf()
            val Set: Set<Int> = setOf()
            object SetObject : Set<Int> by setOf()
            class SetSubclass : Set<Int> by setOf()

            fun map(): Map<Int, Int> = mapOf()
            val Map: Map<Int, Int> = mapOf()
            object MapObject : Map<Int, Int> by mapOf()
            class MapSubclass : Map<Int, Int> by mapOf()

            fun collection(): Collection<Int> = listOf()
            val Collection: Collection<Int> = listOf()
            object CollectionObject : Collection<Int> by listOf()
            class CollectionSubclass : Collection<Int> by listOf()
        """,
"""
        META-INF/main.kotlin_module:
        H4sIAAAAAAAAAC2KMQrCQBBFRzQWUyhsZSdYiYFcQiwkZbzAJhlwYXcnZP5C
        ju8WFv+9V3wi2hPRrq75m7jli8/zqmHeuknToibdWjJCEncesl/sqxjgIT34
        zo2hjOau75QK/BjlqTHKhKD5tUGy1bD6PPEBYnDHT2WPGz3oB8hyC49/AAAA
        """,
        """
        stubs/CollectionObject.class:
        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
        yT4bnTa6bRy1kSfJ/3e4UsLJDgAA
        """,
        """
        stubs/CollectionSubclass.class:
        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
        DgAA
        """,
        """
        stubs/ImmutableCollectionExtensionsKt.class:
        H4sIAAAAAAAAAJ2V3VLbRhiG37X8IyvCFrZJYkMS4wCBJCAgadIUSprSJHVj
        SCZ0Mp3hSAaNR2DLjHfNJGdcS6+gLSedHnSYHvYWei+dfrsoklHsTpIBaXe/
        n/d79kfrv//9408A9/GKYZaLfpPb9U6nL5xm293sttvunvC6/tO3wvU5dfgL
        kQFjsA6cY8duO37Lftk8oKAMNIZk2+OCoTC/0FD+vvDadoNsawy3PjCuNyKR
        ui/clttb26DIm41ur2UfuKLZczyf247vd4UjMbi93RXb/XaboqqHXdH2fHsv
        hOR2BKw4swxZt3Mk3slqJi4hbcCAyZBpuaKhUJPnjRXnNZGHlUUC4wwadylk
        /AL/jivnNBe3jZpSeQgshSvKywy6oiSDiasoGbiCMkOaIHdkZU2987HqJqZw
        TRJep4iOcxQn3HKOqPCjuG0Y4SdAk4KCvvkemgwmZjFtYAZz59BbEkZT73wM
        yMQCbkvoOwxGpMtw9QJntI+EsTjCNWqtS8OCM1hmGCO4zYGixuDg8vD6Ju7h
        viT+gma8vkfr4YkNmt78whv6YD7yRM983Cl5+NlbdeeTVmi8EezsliucfUc4
        ZEt0jjW6B5h8pRjYoewkyP7Wkz1avcT+CsM/ZydzxtmJkbCu06MbCV2jthq0
        M0FbozZdMa3xil5IFhLLiWVGo0I4SlcKVrGSOx8FNoMiSmFErnbFOjuJ0v/6
        Oa1T3UpS16wk+VKRVuTKWHrtmpWNC0cBlyyTcseiKpErb1lyiquM1gDV/70C
        lw7pc8zueC3fEf2eyzD5uu8Lr+PW/WOPe5T1JLqx6IrZ7O678lPwfHe732m6
        vR+lMh2/nW6/t+c+8+SgHGi8+UABK3T6krQHSVTkrUSjH+Te4BFeUJsmXp3a
        irwPQt9XMR99dqFvLeajAx761i/4sigjRSMNDRrVyMqkV1v7HWO/yNOCLXob
        lCfpUvScq2QuqJSRCzWmlAJF/4ZCpJBW1tSI7CJKwwgqcYIsKWQDjYmYxmSo
        MUBwI05gjMiuYnoYwa04QY4IcoFGLaYxH2oMENyNE+RHZC9iKcieDQiyahfO
        sBKHKFJKMZCxYzKrocwAxIM4RGloto6H+JL6MvunAGJGQZyioDbkFDfUspzi
        7nu4Uzz4NVSvKZ2U+tOJdowei6Y8QQ/97tEyl6jqtqpdx0tqDyjza4re2IVW
        x+M6vqE3ntTxLTbr+A5Pd8E4nuH5LiY47nF8r/5XOZY48qqf40hzLKj+PMc0
        x5TqT3KUOBY5UhxVjuJ/gOL8xQkJAAA=
        """,
        """
        stubs/ListObject.class:
        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
        qIOnBVF1MCkWZxycczD3Pw/jQRHOFwAA
        """,
        """
        stubs/ListSubclass.class:
        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
        15BHbhcAAA==
        """,
        """
        stubs/MapObject.class:
        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
        GwAA
        """,
        """
        stubs/MapSubclass.class:
        H4sIAAAAAAAAALVY+1/b1hX/XvkFRhDZBAJkNCwliYFQk7Rpk9rLChkkDpAE
        aElD1qTCCCqwJU+SWehe2aPt3u9lXffu3lu2sa0l7fb5bFl+3B+1z86VZNkY
        GQzJPh9burr33O8593vOPedK//nv3/8J4CncY4iZVnHeTE7KhZnifDYnm2YE
        jMGaWJZX5WRO1paSl+eXlayVcnqKlprj0ukKgYxmKUuKkfLpOpuaWNGtnKol
        l1fzSZV6DU3OJfOysaIYZnKckArKwqT9mGKQqrVGEGRo3qQ5gjBDX92oETQw
        hNOqplpnGQKJvlkRUTRFEYLI0OniZPVcjtSpumZzYY6T5haGBiVfsNaog4hK
        9G1mICVCQiyKfYgziL29C0pOWZIt5eYQw74tovvR1ggB7QxB6xXVZGid2MI8
        EdCU1TVLVjVzXFkj0USmb45Ub+E1gg8wRFblXFG5vMjQTnI+5It4DIei6EYP
        Q1tiq0f75kR04XATInicqFnhKlmG+C4ZMcsViDjmyCQYQrZKkl1SLIbeCd1Y
        Si4r1rzBpZOypumW7JB4qZjLyfM5hdZ01E/11i4RAxjkep6gpanmKGfedhhZ
        OYQTfOQkQ5Q0j2qWoSpmtU9mFIu0XanuS2/2RS+fvVZ3+J4lyMe3W6du8aWm
        eKxwYFIo4jROcXPP0ELIXHKlyVnwt6tKH8Uq+cFG+ZCDctZBmVFfVWw6yEFB
        kx5EjGCYC5xjaCQB21mkqGOTonNeYBP0YI2hmqbY7jZFXMAY10SqQ9mcIhsM
        x8ozXtDMYqGgG5aycLmgGDYxo7eySoE3IphgeNLr71HNHiKvx5vRs6gbPYYi
        LwzqWm6tp7wPG3CpKmpnyOvaUsrZwFeimMQUUZPV84WiRdScT9T0qb3cxaJm
        AydH1DG3mfLbNQyv7R4pPegzxa9vwJdqfzuEwhDPE/5DJxgO7bA6ykMuO5nF
        4XmT4pNhtN617cCRvlscX4Z2w0b3tlZS7fDWesVQnMWG8oqxRKExV9vYh4+W
        Nx8J+P8pgChjBogUhgOJjG+NoE1O48O5HEM8UVW1+mYZ0lWdaT/9NWyi6cFF
        Q89TWSMd5RhM1clYDXsNJa+vkleP7VBXylPiWwX9p/vVSMoxhlLIyVmlzEZ9
        vt68GAKKukA23YcTO4Ub8ffyDkIPGzazm85cl4r5eX64IB810KnKLf+rGI6i
        iI/zw8LBKB1i1viRwG59ghftQ7z1Kd7K8dZnRDpZnuKtz4pIOa3Pi3iOowh4
        TcQoxnjrjapktPWYudMe9A4OM3ieI36Z4VydiNvjzeE6x/uaiBsO8jcYrtYV
        Lnux/iZe5jq+zXC6vpD0A8ligYN8T4SKZd76vogVxyE/YDizC+OrT4g6Chzl
        R7xlI/+E4Zk9vgzQka30PjCpWPKCbMk8TeVXA/Q+wvglREfQFeq6pfInqn/C
        wgnGpPu3j0eFDiEqSL30b4gKDbx9mP5HqU3P0mN0D9C9ie7hjvu3T7bEg3Fh
        SHCuQ2wk9ODtsCAFLsakeJcw1HoyLO2nu3DhwR1GfW1uX7vX1yEd6Ap2sCGh
        QjJgY3RcbJQ6uTyXm+6Vgl3741J8s77wUOjFB3e4znBXsCEiNUx3SI1dDe5o
        5ViUxuJSE9dQ0StKzTSjxZuxr2JMkmKcF8rtoOSklE7DwRX7oNmTyeeLFj97
        lw94o7csRTP5gfWJFdrcjTPqkiZbRYNSWvCcvqDwNxZVU5z9/zyfy5OmnpVz
        s7Kh8me38+B0UbPUvJLRVlVTpa7h8lmYXhxmLDm7QgHhSkdn9KKRVcZU/tDp
        Tp3dMhEnKKpC9I+gEUyK85clWtmfKQAE/IxyDeNvbHRdp5409Ql0b+m/h+b+
        QPo9tL6LA3+xZfk1ao/G6B7DX6kl0jySRgc6eYzxHOYiPe0hvYMDB9/DB99H
        r4C7thSf2e6MujN56wiO0hjj2a82Rt/2GP0uBuVNGuMYZ1yMWBkjKeA+uter
        YGIeTMw15W/UDtP9uC1Jb0muWQlbBZHBId/Hk6zCJIeQqEcIJWsE/GY962D5
        zXJc87Sn/xlbLlUL6cM7IaWrkKhY+K/kI9uthAqLv/6LO+k/X6V/3Au3BCHa
        8v/A5LX45XuY/ldN/VSGXJemaVagxqweZ8TzZRSzuOoivYhrhODY9AL1USF6
        aMSXPMSPUt+NR24jlS8XkbMfrIHY74zUQLxqx3cU84QdcLFlGqWq5m373Vjr
        7A7e4ttNoLaCRdfKkyQt1EBqd+0oI71SkYqWqEXVdU8WVa+W8ersIu3OoiPI
        29G9Am2P3OQ9bvK2JVThXaRH4cGr9o7RHxlPH4OxJ8/NUiyVPGdiin/pci2a
        dhNu28C/0f0WQoG7/QP3UdzArQ28ytNLAO/YV9Zol5SwbVGsQk+bp6fN5XOK
        fyWrA/+Tu8bvd/EHvHipxmfrZfxPr+/JfmmYf31z8Z918cP9Axu4vY5Sad2K
        E/Zwwh4Pp6kKOP5Kulk41L+Bz/nDOCk0VFVWpvi3Lx+QL9QPkiaQEa+KVIK8
        frcOkCn+ycvHgi/Wb8F54nQGX3ItuFDaBdxV3cc38BV/JL8dUdqvduYl1Dl8
        tRbq1/eC+pKNeqO2rd/cu6038S0XdZbyB88gLQ4q/Qc38B1/ZCfXVB6bSsgt
        hJx3D1V2tSAdWXzX1THlWt5a2hDHS/viznZLaPUUtXrpsdVJjwSvevDjLnyz
        t4QNvLkdcLMH3OytoNlZAQGv+O+4t3a14whHxw9dnGsux7Eyx7aRP/aPeYfm
        ymNlyciYR3OMaM7bNOvb8vDTPfLg1fsA3rXvf8IG3ddI5ue0o96+jkAGv8jg
        l3TFr/jl1xn8Br+9Dmbid/j9dXSZ6DTxB/vXZOJuqf2UiVMm/mgiZWLExHMm
        hk1csIdGTYyZfCK1j5g4ZqLfxICJQyaG/gcbVySQHhsAAA==
        """,
        """
        stubs/SetObject.class:
        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
        s3GSFv8BceJNatwNAAA=
        """,
        """
        stubs/SetSubclass.class:
        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
        """
    )

    @Test
    fun mutableCollection_stdlib() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*

                val list = mutableListOf(1)
                val set = mutableSetOf(1)
                val map = mutableMapOf(1 to 1)
                val collection: MutableCollection<Int> = list

                val listFunction = mutableStateOf(mutableListOf(1))
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(mutableSetOf(1))
                val setProperty = mutableStateOf(set)

                val mapFunction = mutableStateOf(mutableMapOf(1 to 1))
                val mapProperty = mutableStateOf(map)

                val collectionProperty = mutableStateOf(collection)

                fun test(
                    listParam: MutableList<Int>,
                    setParam: MutableSet<Int>,
                    mapParam: MutableMap<Int, Int>,
                    collectionParam: MutableCollection<Int>
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val collectionProperty = mutableStateOf(collectionParam)
                }
            """
            ),
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:11: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(mutableListOf(1))
                                   ~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(list)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(mutableSetOf(1))
                                  ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(set)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(mutableMapOf(1 to 1))
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(map)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(collection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:28: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val listParameter = mutableStateOf(listParam)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:29: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val setParameter = mutableStateOf(setParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:30: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val mapParameter = mutableStateOf(mapParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:31: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val collectionProperty = mutableStateOf(collectionParam)
                                             ~~~~~~~~~~~~~~
0 errors, 11 warnings
            """
            )
    }

    @Test
    fun mutableCollection_stdlib_explicitExpressionType_noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*

                val list = mutableListOf(1)
                val set = mutableSetOf(1)
                val map = mutableMapOf(1 to 1)
                val collection: MutableCollection<Int> = list

                val listFunction = mutableStateOf<List<Int>>(mutableListOf(1))
                val listProperty = mutableStateOf<List<Int>>(list)

                val setFunction = mutableStateOf<Set<Int>>(mutableSetOf(1))
                val setProperty = mutableStateOf<Set<Int>>(set)

                val mapFunction = mutableStateOf<Map<Int, Int>>(mutableMapOf(1 to 1))
                val mapProperty = mutableStateOf<Map<Int, Int>>(map)

                val collectionProperty = mutableStateOf<Collection<Int>>(collection)

                fun test(
                    listParam: MutableList<Int>,
                    setParam: MutableSet<Int>,
                    mapParam: MutableMap<Int, Int>,
                    collectionParam: MutableCollection<Int>
                ) {
                    val listParameter = mutableStateOf<List<Int>>(listParam)
                    val setParameter = mutableStateOf<Set<Int>>(setParam)
                    val mapParameter = mutableStateOf<Map<Int, Int>>(mapParam)
                    val collectionProperty = mutableStateOf<Collection<Int>>(collectionParam)
                }
            """
            ),
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun mutableCollection_stdlib_explicitPropertyType_noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*

                val list = mutableListOf(1)
                val set = mutableSetOf(1)
                val map = mutableMapOf(1 to 1)
                val collection: MutableCollection<Int> = list

                val listFunction: MutableState<List<Int>> = mutableStateOf(mutableListOf(1))
                val listProperty: MutableState<List<Int>> = mutableStateOf(list)

                val setFunction: MutableState<Set<Int>> = mutableStateOf(mutableSetOf(1))
                val setProperty: MutableState<Set<Int>> = mutableStateOf(set)

                val mapFunction: MutableState<Map<Int, Int>> = mutableStateOf(mutableMapOf(1 to 1))
                val mapProperty: MutableState<Map<Int, Int>> = mutableStateOf(map)

                val collectionProperty: MutableState<Collection<Int>> = mutableStateOf(collection)

                fun test(
                    listParam: MutableList<Int>,
                    setParam: MutableSet<Int>,
                    mapParam: MutableMap<Int, Int>,
                    collectionParam: MutableCollection<Int>
                ) {
                    val listParameter: MutableState<List<Int>> = mutableStateOf(listParam)
                    val setParameter: MutableState<Set<Int>> = mutableStateOf(setParam)
                    val mapParameter: MutableState<Map<Int, Int>> = mutableStateOf(mapParam)
                    val collectionProperty: MutableState<Collection<Int>> = mutableStateOf(collectionParam)
                }
            """
            ),
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun mutableCollection_java() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*

                val list = java.util.ArrayList<Int>()
                val set = java.util.HashSet<Int>()
                val linkedSet = java.util.LinkedHashSet<Int>()
                val map = java.util.HashMap<Int, Int>()
                val linkedMap = java.util.LinkedHashMap<Int, Int>()
                val collection: java.util.Collection<Int> = list as java.util.Collection<Int>

                val listFunction = mutableStateOf(java.util.ArrayList<Int>())
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(java.util.HashSet<Int>())
                val setProperty = mutableStateOf(set)

                val linkedSetFunction = mutableStateOf(java.util.LinkedHashSet<Int>())
                val linkedSetProperty = mutableStateOf(linkedSet)

                val mapFunction = mutableStateOf(java.util.HashMap<Int, Int>())
                val mapProperty = mutableStateOf(map)

                val linkedMapFunction = mutableStateOf(java.util.LinkedHashMap<Int, Int>())
                val linkedMapProperty = mutableStateOf(linkedMap)

                val collectionProperty = mutableStateOf(collection)

                fun test(
                    listParam: java.util.ArrayList<Int>,
                    setParam: java.util.HashSet<Int>,
                    linkedSetParam: java.util.LinkedHashSet<Int>,
                    mapParam: java.util.HashMap<Int, Int>,
                    linkedMapParam: java.util.LinkedHashMap<Int, Int>,
                    collectionParam: java.util.Collection<Int>
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                    val collectionProperty = mutableStateOf(collectionParam)
                }
            """
            ),
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(java.util.ArrayList<Int>())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(list)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:16: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(java.util.HashSet<Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(set)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetFunction = mutableStateOf(java.util.LinkedHashSet<Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetProperty = mutableStateOf(linkedSet)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(java.util.HashMap<Int, Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:23: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(map)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapFunction = mutableStateOf(java.util.LinkedHashMap<Int, Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:26: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapProperty = mutableStateOf(linkedMap)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:28: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(collection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:38: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val listParameter = mutableStateOf(listParam)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:39: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val setParameter = mutableStateOf(setParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:40: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                                             ~~~~~~~~~~~~~~
src/test/test.kt:41: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val mapParameter = mutableStateOf(mapParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:42: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                                             ~~~~~~~~~~~~~~
src/test/test.kt:43: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val collectionProperty = mutableStateOf(collectionParam)
                                             ~~~~~~~~~~~~~~
0 errors, 17 warnings
            """
            )
    }

    /**
     * Tests for Kotlin collection types that are actually just aliases for the java classes on JVM
     */
    @Test
    fun mutableCollection_kotlinTypeAliases() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*

                val list = kotlin.collections.ArrayList<Int>()
                val set = kotlin.collections.HashSet<Int>()
                val linkedSet = kotlin.collections.LinkedHashSet<Int>()
                val map = kotlin.collections.HashMap<Int, Int>()
                val linkedMap = kotlin.collections.LinkedHashMap<Int, Int>()

                val listFunction = mutableStateOf(kotlin.collections.ArrayList<Int>())
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(kotlin.collections.HashSet<Int>())
                val setProperty = mutableStateOf(set)

                val linkedSetFunction = mutableStateOf(kotlin.collections.LinkedHashSet<Int>())
                val linkedSetProperty = mutableStateOf(linkedSet)

                val mapFunction = mutableStateOf(kotlin.collections.HashMap<Int, Int>())
                val mapProperty = mutableStateOf(map)

                val linkedMapFunction = mutableStateOf(kotlin.collections.LinkedHashMap<Int, Int>())
                val linkedMapProperty = mutableStateOf(linkedMap)

                fun test(
                    listParam: kotlin.collections.ArrayList<Int>,
                    setParam: kotlin.collections.HashSet<Int>,
                    linkedSetParam: kotlin.collections.LinkedHashSet<Int>,
                    mapParam: kotlin.collections.HashMap<Int, Int>,
                    linkedMapParam: kotlin.collections.LinkedHashMap<Int, Int>,
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                }
            """
            ),
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(kotlin.collections.ArrayList<Int>())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(list)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(kotlin.collections.HashSet<Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:16: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(set)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetFunction = mutableStateOf(kotlin.collections.LinkedHashSet<Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedSetProperty = mutableStateOf(linkedSet)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:21: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(kotlin.collections.HashMap<Int, Int>())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(map)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:24: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapFunction = mutableStateOf(kotlin.collections.LinkedHashMap<Int, Int>())
                                        ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val linkedMapProperty = mutableStateOf(linkedMap)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:34: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val listParameter = mutableStateOf(listParam)
                                        ~~~~~~~~~~~~~~
src/test/test.kt:35: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val setParameter = mutableStateOf(setParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:36: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedSetParameter = mutableStateOf(linkedSetParam)
                                             ~~~~~~~~~~~~~~
src/test/test.kt:37: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val mapParameter = mutableStateOf(mapParam)
                                       ~~~~~~~~~~~~~~
src/test/test.kt:38: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                    val linkedMapParameter = mutableStateOf(linkedMapParam)
                                             ~~~~~~~~~~~~~~
0 errors, 15 warnings
            """
            )
    }

    @Test
    fun mutableCollection_sourceExtensions() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(mutableList())
                val listProperty = mutableStateOf(MutableList)
                val listObjectImplementation = mutableStateOf(MutableListObject)
                val listSubclass = mutableStateOf(MutableListSubclass())

                val setFunction = mutableStateOf(mutableSet())
                val setProperty = mutableStateOf(MutableSet)
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                val setSubclass = mutableStateOf(MutableSetSubclass())

                val mapFunction = mutableStateOf(mutableMap())
                val mapProperty = mutableStateOf(MutableMap)
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                val mapSubclass = mutableStateOf(MutableMapSubclass())

                val collectionFunction = mutableStateOf(mutableCollection())
                val collectionProperty = mutableStateOf(MutableCollection)
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
            """
            ),
            Stubs.SnapshotState,
            KotlinMutableCollectionExtensions.kotlin
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:7: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(mutableList())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:8: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(MutableList)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:9: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listObjectImplementation = mutableStateOf(MutableListObject)
                                               ~~~~~~~~~~~~~~
src/test/test.kt:10: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listSubclass = mutableStateOf(MutableListSubclass())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(mutableSet())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(MutableSet)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setSubclass = mutableStateOf(MutableSetSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(mutableMap())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(MutableMap)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapSubclass = mutableStateOf(MutableMapSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionFunction = mutableStateOf(mutableCollection())
                                         ~~~~~~~~~~~~~~
src/test/test.kt:23: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(MutableCollection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:24: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                                                     ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
                                         ~~~~~~~~~~~~~~
0 errors, 16 warnings
            """
            )
    }

    @Test
    fun mutableCollection_compiledExtensions() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(mutableList())
                val listProperty = mutableStateOf(MutableList)
                val listObjectImplementation = mutableStateOf(MutableListObject)
                val listSubclass = mutableStateOf(MutableListSubclass())

                val setFunction = mutableStateOf(mutableSet())
                val setProperty = mutableStateOf(MutableSet)
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                val setSubclass = mutableStateOf(MutableSetSubclass())

                val mapFunction = mutableStateOf(mutableMap())
                val mapProperty = mutableStateOf(MutableMap)
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                val mapSubclass = mutableStateOf(MutableMapSubclass())

                val collectionFunction = mutableStateOf(mutableCollection())
                val collectionProperty = mutableStateOf(MutableCollection)
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
            """
            ),
            Stubs.SnapshotState,
            Stubs.Composable,
            KotlinMutableCollectionExtensions.bytecode
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expect(
                """
                    src/test/test.kt:7: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listFunction = mutableStateOf(mutableList())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:8: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listProperty = mutableStateOf(MutableList)
                                   ~~~~~~~~~~~~~~
src/test/test.kt:9: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listObjectImplementation = mutableStateOf(MutableListObject)
                                               ~~~~~~~~~~~~~~
src/test/test.kt:10: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val listSubclass = mutableStateOf(MutableListSubclass())
                                   ~~~~~~~~~~~~~~
src/test/test.kt:12: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setFunction = mutableStateOf(mutableSet())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:13: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setProperty = mutableStateOf(MutableSet)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:14: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setObjectImplementation = mutableStateOf(MutableSetObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:15: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val setSubclass = mutableStateOf(MutableSetSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:17: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapFunction = mutableStateOf(mutableMap())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:18: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapProperty = mutableStateOf(MutableMap)
                                  ~~~~~~~~~~~~~~
src/test/test.kt:19: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapObjectImplementation = mutableStateOf(MutableMapObject)
                                              ~~~~~~~~~~~~~~
src/test/test.kt:20: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val mapSubclass = mutableStateOf(MutableMapSubclass())
                                  ~~~~~~~~~~~~~~
src/test/test.kt:22: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionFunction = mutableStateOf(mutableCollection())
                                         ~~~~~~~~~~~~~~
src/test/test.kt:23: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionProperty = mutableStateOf(MutableCollection)
                                         ~~~~~~~~~~~~~~
src/test/test.kt:24: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionObjectImplementation = mutableStateOf(MutableCollectionObject)
                                                     ~~~~~~~~~~~~~~
src/test/test.kt:25: Warning: Creating a MutableState object with a mutable collection type [MutableCollectionMutableState]
                val collectionSubclass = mutableStateOf(MutableCollectionSubclass())
                                         ~~~~~~~~~~~~~~
0 errors, 16 warnings
            """
            )
    }

    @Test
    fun immutableCollection_stdlib_noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*

                val list = listOf(1)
                val set = setOf(1)
                val map = mapOf(1 to 1)
                val collection: Collection<Int> = list

                val listFunction = mutableStateOf(listOf(1))
                val listProperty = mutableStateOf(list)

                val setFunction = mutableStateOf(setOf(1))
                val setProperty = mutableStateOf(set)

                val mapFunction = mutableStateOf(mapOf(1 to 1))
                val mapProperty = mutableStateOf(map)

                val collectionProperty = mutableStateOf(collection)

                fun test(
                    listParam: List<Int>,
                    setParam: Set<Int>,
                    mapParam: Map<Int, Int>,
                    collectionParam: Collection<Int>
                ) {
                    val listParameter = mutableStateOf(listParam)
                    val setParameter = mutableStateOf(setParam)
                    val mapParameter = mutableStateOf(mapParam)
                    val collectionProperty = mutableStateOf(collectionParam)
                }
            """
            ),
            Stubs.SnapshotState
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun immutableCollection_sourceExtensions_noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(list())
                val listProperty = mutableStateOf(List)
                val listObjectImplementation = mutableStateOf(ListObject)
                val listSubclass = mutableStateOf(ListSubclass())

                val setFunction = mutableStateOf(set())
                val setProperty = mutableStateOf(Set)
                val setObjectImplementation = mutableStateOf(SetObject)
                val setSubclass = mutableStateOf(SetSubclass())

                val mapFunction = mutableStateOf(map())
                val mapProperty = mutableStateOf(Map)
                val mapObjectImplementation = mutableStateOf(MapObject)
                val mapSubclass = mutableStateOf(MapSubclass())

                val collectionFunction = mutableStateOf(collection())
                val collectionProperty = mutableStateOf(Collection)
                val collectionObjectImplementation = mutableStateOf(CollectionObject)
                val collectionSubclass = mutableStateOf(CollectionSubclass())
            """
            ),
            Stubs.SnapshotState,
            KotlinImmutableCollectionExtensions.kotlin
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }

    @Test
    fun immutableCollection_compiledExtensions_noErrors() {
        lint().files(
            kotlin(
                """
                package test

                import androidx.compose.runtime.*
                import stubs.*

                val listFunction = mutableStateOf(list())
                val listProperty = mutableStateOf(List)
                val listObjectImplementation = mutableStateOf(ListObject)
                val listSubclass = mutableStateOf(ListSubclass())

                val setFunction = mutableStateOf(set())
                val setProperty = mutableStateOf(Set)
                val setObjectImplementation = mutableStateOf(SetObject)
                val setSubclass = mutableStateOf(SetSubclass())

                val mapFunction = mutableStateOf(map())
                val mapProperty = mutableStateOf(Map)
                val mapObjectImplementation = mutableStateOf(MapObject)
                val mapSubclass = mutableStateOf(MapSubclass())

                val collectionFunction = mutableStateOf(collection())
                val collectionProperty = mutableStateOf(Collection)
                val collectionObjectImplementation = mutableStateOf(CollectionObject)
                val collectionSubclass = mutableStateOf(CollectionSubclass())
            """
            ),
            Stubs.SnapshotState,
            Stubs.Composable,
            KotlinImmutableCollectionExtensions.bytecode
        )
            .skipTestModes(TestMode.TYPE_ALIAS)
            .run()
            .expectClean()
    }
}
/* ktlint-enable max-line-length */
