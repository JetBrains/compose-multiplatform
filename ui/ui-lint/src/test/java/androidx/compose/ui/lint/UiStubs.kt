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

package androidx.compose.ui.lint

import androidx.compose.lint.test.compiledStub
import com.android.tools.lint.checks.infrastructure.TestFile

object UiStubs {

    val Density: TestFile = compiledStub(
        filename = "Density.kt",
        filepath = "androidx/compose/ui/unit",
        checksum = 0x8c5922ca,
        """
            package androidx.compose.ui.unit

            interface Density
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAH2NSwrCMBCGR0SErCToVrC4UsghtFakG0EvEJqxBmomJBOo
                tzfS7gQXw//6YABgCgCTfPNRQaDYamcCWdOrhl6eIqoHJWc0W3KqxcgpYJSr
                Muj2PKQSGRumULNcVBROunmOS26Wd+1/OLEXxb83nX5TYjk7UJ/ho9j8wMkq
                63xi5ck6xiDXtxQ9OmNdex2qy3evbJdtzQXs4AM20YY08QAAAA==
                """,
        """
                androidx/compose/ui/unit/Density.class:
                H4sIAAAAAAAAAIVOTUvDQBB9s9Gmxq/UD6g38Qe4benNkyBCoCIoeMlpm6wy
                Tbor3U2pt/4uD9KzP0rcqHdn4M17M/DefH69fwAY44Rwrky5sFyuZGHnr9Zp
                2bBsDHt5o41j/xaDCOlMLZWslXmR99OZLnyMiNCbVNbXbOSd9qpUXl0RxHwZ
                BW9qISZQFVYrbtUgsHJION2su4noi0SkgT33N+uRGFB7HBEuJv/9EzJASP7U
                ZeWDeLTNotC3XGvC2UNjPM/1Ezue1vraGOuVZ2tcJ2RgC78lcPSDPRyHOQyW
                26E7OaIMcYZuQOy0kGTYxV4OctjHQQ7hcOiQfgOBbqTCRAEAAA==
                """
    )

    val PointerEvent: TestFile = compiledStub(
        filename = "PointerEvent.kt",
        filepath = "androidx/compose/ui/input/pointer",
        checksum = 0xbe2705da,
        """
            package androidx.compose.ui.input.pointer

            import androidx.compose.ui.unit.Density

            interface AwaitPointerEventScope : Density

            class PointerId(val value: Long)

            class PointerInputChange(
                val id: PointerId
            )
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAH2NSwrCMBCGR0SErCToVrC4UsghtFakG0EvEJqxBmomJBOo
                tzfS7gQXw//6YABgCgCTfPNRQaDYamcCWdOrhl6eIqoHJWc0W3KqxcgpYJSr
                Muj2PKQSGRumULNcVBROunmOS26Wd+1/OLEXxb83nX5TYjk7UJ/ho9j8wMkq
                63xi5ck6xiDXtxQ9OmNdex2qy3evbJdtzQXs4AM20YY08QAAAA==
                """,
        """
                androidx/compose/ui/input/pointer/AwaitPointerEventScope.class:
                H4sIAAAAAAAAAJ1QTU8CMRB9syiL+AEoGrwZf4AFwsHoiURNSDAaSbxwKrvV
                FJZ2Q2cRb/wuD4azP8rY1QsHTjbpm77Xzsybfn1/fALo4IRwKU08szpeiMhO
                U+uUyLTQJs1YpFYbVjPRfZOaH//I7VwZHkQ2VSGIUB3LuRSJNK/iYTRWEYco
                EM421cyMZnGjjNP8HmKbUOtPLCfaiHvFMpYsrwnBdF7wziiHkEATLy10zpr+
                FLcI9dWyVA4aQb5LL43Vsh00Kb9rE676/x3Gtz7fmLzu2j+qrGdeTJhQHths
                Fqk7nSjC6VNmWE/Vs3Z6lKiuMZYla2tc0VvEFghF5CtA/RePcOxjy+v+S1Aa
                otDDTg9lj9jNYa+HfRwMQQ4VVIcIHGoOhz/oLRV0wgEAAA==
                """,
        """
                androidx/compose/ui/input/pointer/PointerId.class:
                H4sIAAAAAAAAAJVQTW8SQRh+ZpZdlhVkQa0UP6r20mJ0aeNN06iNJhD8SGu4
                cBrYSTsFZgk7S3rkt3j3YKIx8WCIR3+U8Z2FePJiMvPM+7zz5Hk/fv3+/gPA
                E+wyPBQ6nicqvoxGyXSWpDLKVKT0LDPRLFHayHn0fv124iIYQ3ghFiKaCH0W
                vRteyJEpwmHwnimtzBFDYa+732dw9vb7ZbgoBijAZ3AXYpJJBtYtI8CVEjjK
                JDbnKmV41PuPJp4y+GfS9Nd+VKfLUOuNEzNROnojjYiFESTi04VDQzILRSo8
                ptSlsqxNUXzAcLxa1gPe4AEPV8uADg9LAfedxmp5yNvsZaXuhbzJ287Pjx4P
                Cye1v8wndbPgu6FnrQ4ZlUF10+GrhdTm8djQdMdJTC1We0rLt9l0KOcfxHBC
                mXovGYlJX8yV5ZtkcJpk85F8rSzZPsm0UVPZV6mi3xdaJ0YYlegUB7S6gh0M
                dbtJijjFLjzCu8SO4MBOGbS+odTa+YrK51yzQ2g1gI97hFuUIxWuogrkkXWj
                XSKku/aKcm/AbX1B5dM/bcprwcaG436Od/CA3ud5ky6uDeB0cL2DG4TYsnCz
                gwa2B2Apmrg1QDFFNcXtFEGOXoowRe0P2i862KkCAAA=
                """,
        """
                androidx/compose/ui/input/pointer/PointerInputChange.class:
                H4sIAAAAAAAAAJVSXU8TURA9d9tul6XItogUUPwApRRxCyG+YIxKNGlSkYDh
                hafb3Zty2+1dsnvb8Mhv8RdootH4YIiP/ijj7HYDgi+SbObMmcycmZ25v35/
                /wFgE2sMm1z5USj9E9cL+8dhLNyBdKU6Hmj3OJRKi8jdHWEzCW4fcdURRTAG
                p8uH3A2Iu+/aXeHpInIM5jOppH7O0Ki1/l/a31o5YFhshVHH7QrdjrhUscuV
                CjXXMiR/J9Q7gyDYYjCkb8FiWOiFOpDK7Q77bqqieOA2lY6oVHpxETbDtHck
                vF5Wu8sj3heUyLBca12dfuuvyH4i0qGZSihhwsY4bjDkagkvwLGRR5lh7Vr/
                V4KFqTEYuMmQ10cyZnh6DYGL3dMGCh2hmz6DW1u51gwM5Va2tLdCc59rnuyz
                P8zRa2CJKTKwHoVOZMIa5PnrDNtnpxXbqBq24Zyd2vSlvpWrnp1uGA32aqJi
                Osac0cj9/GAaTn6vfM4syp7LWwXHTKQ2GLXBZDbQ66FQ+klPM8zvDZSWfdFU
                QxnLdiBeXhyetrUd+oLKWlKJnUG/LaL3nHIYKq3Q48EBj2TCs+DSVa3zq18S
                tffDQeSJNzKpmc1qDv7pjnW6WD7ZDirJAQkfETMJi4QGYYGYgWViLUKD0Fmt
                jH3DZP0rKvXVL5j+lGbWyN5AjrJtJC9qkuwKxW6NaghngNQb9aln50gblVHF
                bNbGRRIFCvXPmP54rm2mwfFUszRKyDQvT7ya2od4TPiConOUN3+IXBO3m7hD
                FguJudvEPdw/BIvxAIuHKMaYibEUw4oxFcOMUY0x+wcqer7sSgQAAA==
                """
    )

    val PointerInputScope: TestFile = compiledStub(
        filename = "SuspendingPointerInputFilter.kt",
        filepath = "androidx/compose/ui/input/pointer",
        checksum = 0xd7db138c,
        """
            package androidx.compose.ui.input.pointer
            import androidx.compose.ui.unit.Density
            import androidx.compose.ui.Modifier

            interface PointerInputScope : Density {
                suspend fun <R> awaitPointerEventScope(
                    block: suspend AwaitPointerEventScope.() -> R
                ): R
            }

            fun Modifier.pointerInput(
                key1: Any?,
                block: suspend PointerInputScope.() -> Unit
            ): Modifier = Modifier
        """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAH2NSwrCMBCGR0SErCToVrC4UsghtFakG0EvEJqxBmomJBOo
                tzfS7gQXw//6YABgCgCTfPNRQaDYamcCWdOrhl6eIqoHJWc0W3KqxcgpYJSr
                Muj2PKQSGRumULNcVBROunmOS26Wd+1/OLEXxb83nX5TYjk7UJ/ho9j8wMkq
                63xi5ck6xiDXtxQ9OmNdex2qy3evbJdtzQXs4AM20YY08QAAAA==
                """,
        """
                androidx/compose/ui/input/pointer/PointerInputScope.class:
                H4sIAAAAAAAAAJ1T3U4TQRQ+s7vtLkV0WRULKGDBqDG4teoNJQSiEEqqkpZ4
                w9V0uzRTtjPNzmyFu42P4oXPYLwwDd75Ir6F8Wx/AkJjjRdz5syZb84538w3
                P359/QYAL+ABgeeU10PB6ieuJ1ptIX03Yi7j7Ui5bcG48kN3vz+XkmDVE23f
                BELAbtIOdQPKG+67WtP3lAk6gaVR6SLOlPva55KpUxNSBGboB8rUIO92x+f9
                vAQOHpWPhQoYd5udlnsUcU8xwaW7M/AKxeG+J0IRKcZ96b4SHJ2IJoDi4/Ll
                vooEfq5X1q7GN8YVW18tj7+drZFUiqtjGl1fPagUN4pPRrQ1juPg6EimK2UR
                Ntymr2ohZciFci4U7fN6GwUBrQU+wpb/BhMqQSJqetjIG1/ROlUUY1qro6N2
                SGJMAuQYQycsWeXRqz8jEHfjXEbLav1h6ed+Muxu3HO6sXWU7cYFLU/2Nm1t
                TtvVc4bVjW298NA25pYt4hiOlk87GcdKvF09bzppx8iSvJVPnX1Ka9bE7tnn
                ze9fSDdOlnbm7KNmYMHZpI8CgZf/8HhXpI0McyPPXdQwgkiFQKoWCA/5O8Nr
                OlcpgbX/lw7+onEqJ/gCsDBEbZ+opDHBhw0cnPbSLFYj2fZ5nfHGRaI7LED3
                6bEiMFFlDU5VFOLfm69EWKHll3iHSYY62ToXBQrr8u4+DWnLx0R/wDJVEYWe
                jyUw4+zgzPsr+dL4RmAgh3SiJYOACRbosIQrDSbgPs5p3M3gnMMxpeFiMoH2
                rAbLPbsIKziXMXoNUjB1CHoJrpfgBlqwEzNdAgduHgKRcAtuH8KkhBkJdySY
                ErISZiXMSZiXcFfCPQkLEqzfOHmKfRsFAAA=
                """,
        """
                androidx/compose/ui/input/pointer/SuspendingPointerInputFilterKt.class:
                H4sIAAAAAAAAAK1VW08bVxD+ztrYi0OLs5QWDCW0OOGSkHWcSyMtQq1QIllx
                aFqnvPB0vD5xDl6fY+0FkTfUn5JfUPUpykOE0rf+lv6GqHOWhZhAjVTF0s7O
                5Zs5M3Nm1n9/ePsOwD08ZPiRq06oZefA9XV/oCPhJtKVapDE7kBLFYvQbSXR
                QKiOVN1nx5qGMT+WAbFP4iIYQ3mP73M34Krr/tzeEz5pcwwTgyE8w2CledFh
                T3VHvpAi9JqfBvGaPR0HUrl7+333RaL8WGoVuY8zru6tjg7I8M/nPnJj/cKA
                Zxs23KaWrwfCWz8J6+tQJ7FUInK3tCIm4SbuxingNyVjb9O7eT6zzcvLXWrq
                sOvuibgdcklpc6V0zI9L2NbxdhIEhKqOQhGEtwNBsMJG/FJGmzZKDAtDXUlr
                UzxwGyoOyV/6URETDNP+S+H3smOe8ZD3BQEZllcu6PJHTcsE6XqrOxP4EpMl
                fIEyw1g70H7PhsMwP6rmIr5iGN8iA1eUP8Po+66eIr0JfI1vxjGNGQanaiqt
                np3Whcuane+JV3fI+Xx1DIuXTS7D1RPIUxHzDo856az+fo4WkxlSZGA9w1ik
                P5CGqxHXoSMPjw6rpaPDkjVjpa8ykUw8fcpWZq48InPFqrE1eup22arkZ1gt
                V18u5ytLNnPyjlUrOCXHTjm7VnQKToqojb1/XbDs8b/esKNDw5ZL73+38iXL
                njV51BklStVnVQyXdv9/7chQ1/5jSzxzKxnk0UEsaPa0Ojn4+as0xrVRX6vb
                PbrY/JbuCIbJJsXfTvptET43E29q0T4PdngojZwpx1uyq3ichMTP/ZpQMn3R
                UPsykmT+6ePm0Fp9aj3dgTOwUksnoS8oIYo4m/nsnIuHO7CQh/kRDGMokFQj
                6RfSm2GYWnOuvMHVW84U0T8xe4TpP8y0kCMITF9fTKJO/OIxHBXMpeGmMI9v
                yW44BwvkcTf1K9IfwrGnTe/7xp7LBGpDSu0U+CClLn6gd5O01yi7xV3kGviu
                ge+JYqmBKq43cAPLu2ARVrC6iysRxiLMRZiPsBbBiXAzwq0I66l4O0LhX/yB
                0s2YBgAA
                """
    )

    val Alignment: TestFile = compiledStub(
        filename = "Alignment.kt",
        filepath = "androidx/compose/ui",
        checksum = 0xd737b17c,
        """
            package androidx.compose.ui
            class Alignment {
                companion object {
                    val TopStart = Alignment()
                }
            }
            """,
        """
                META-INF/main.kotlin_module:
                H4sIAAAAAAAAAH2NSwrCMBCGR0SErCToVrC4UsghtFakG0EvEJqxBmomJBOo
                tzfS7gQXw//6YABgCgCTfPNRQaDYamcCWdOrhl6eIqoHJWc0W3KqxcgpYJSr
                Muj2PKQSGRumULNcVBROunmOS26Wd+1/OLEXxb83nX5TYjk7UJ/ho9j8wMkq
                63xi5ck6xiDXtxQ9OmNdex2qy3evbJdtzQXs4AM20YY08QAAAA==
                """,
        """
                androidx/compose/ui/Alignment＄Companion.class:
                H4sIAAAAAAAAAJVTTW/TQBB96ziJ6waa9AP6AZTSQJNC67biVoRog5AipUVq
                q1x6QBtnCZs468q7iXrMiR/CL4ATiAOKeuRHIcapaSuQKPgwO+/NvJlZj/39
                x9dvAJ5ijWGFq2YUyuap54fdk1ALrye9nUC2VFcoU6wQyZUMVRaMId/mfe4F
                XLW814228E0WKYbMM6mkec6QKpXrOaSRcWEjy2Cbd1IzlGv/2GObYbwlzFF4
                cmh4ZBiWStdoSbFcC6OW1xamEXGptMeVCg03VE57+6HZ7wUBZd39a5ksbjLM
                cN8XWhevTFD0T3LII+diAgWGzVKtE5pAKq/d73pSGREpHngvxVveC0yFOpqo
                55sw2uNRR0Tb5boLK34RU0X/MvimO4oyrP9fNYbCL8GeMLzJDSfO6vZTtEsW
                G+rEOkSdyhhtkNfcZNgdDqZda9Zyrfxw4FqOdQ4cyzl7n5odDrasDbabdayz
                Dxkrbx0U8ql5a8Mm5LjDwbztpPOZuNIWoy5wLpezeO1qxi42y5C7CKx3SGxX
                wqZgmKhJJfZ73YaIjngjIGayFvo8qPNIxjghFw56ysiuqKq+1JKoncslU+mq
                UiKqBFxrQdA9DHuRL17JWDmXKOt/6LBJu7ERPyny6Kul6z0i5NFJd0V69TOc
                T+RYWCGbGZE2SmRz5wkYg0tnAePEWCPxeiK2v2Dy42/a9BWtnWjLSfQGkGeU
                MZUMsUanlQwxHQ/BRuJb52Qijr0Z4ujPwyohdySaQBFzeDxq/hBP6HxB/G3K
                nT1Gqoq5KubJYiE2d6q4i3vHYBqLuH+MrIarsaSR0XigsawxrpH7CTHvlCUx
                BAAA
                """,
        """
                androidx/compose/ui/Alignment.class:
                H4sIAAAAAAAAAIVSXU8TQRQ9s9uWsqxSqlTKh4BUKagsEBMTISZYY9KkYCKE
                hPA03Y512u0s2ZlteOS3+AtEHkgkMcRHf5TxbikQNIGXe/eeOffcc2f2958f
                PwG8gscwxVUjCmXj0PPDzkGohRdLbyOQTdURygyAMeRavMu9gKum97HeEj6h
                NkNmXSpp3jLY5YVdF2lkHKQwwJAyX6RmmK7dqrzGMMp9X2hdagqzEx5sGx6Z
                kn/AMFteuLM3e9nh4h6cQVi4T+C6H/RNzd8qUKoQyJUM1QBGGFbKtXZoqNVr
                dTueVEZEigfee/GZx4GphEqbKPZNGG3yqC2itYt9HzjI4yHD4JUYwx3Gr+eu
                uSjgUeJ7zKFA1zZXC6Om1xKmHnGptMeVCg03xNXeVmi24iCgvUcunW4Kwxvc
                cMKsTtem52RJICHWJuhQJtUyfTVWGErnR65jjVmOlTs/cqysNXZ+NGOvWsvs
                DbPfpX99zVg5K+GuMtKBe2V4qW0YJj7FysiOqKqu1LIeiI1ra/TclbAhGIZr
                UomtuFMX0Q4nDkO+Fvo82OWRTOo+6FaVElEl4FoLana2wzjyxQeZnBX7c3b/
                m4IVuqMULWShmNwbeSxTlaE8QXk8+Qf+wWzK6V61QJVHmVZDevEU2eOe0GKf
                nFCfU3QvCBgkKeSKGOohSfNk7wRIfcfwt+Sib/RmkbsaswS7xyycIb/HTjF6
                guIZrL1TjJ9g+PhG7xDNsvGCqsR6nhwVaLmXPW/zpAS8JnySWFP7sKt4XMU0
                RcwkYbaKJ5jbB9Mo4ek+UhqOxjONjEbhL7J1D8vfAwAA
                """
    )
}