package org.jetbrains.kotlin.r4a


class KtxTypeResolutionTests: AbstractR4aDiagnosticsTest() {

    fun testThatUnresolvedTagDiagnosticIsOnlyOnTagName() = doTestWithContext(
        """
            <<!UNRESOLVED_REFERENCE!>SomeNameThatWillNotResolve<!> foo=123>
                <Foo />
            </<!UNRESOLVED_REFERENCE!>SomeNameThatWillNotResolve<!>>
        """
    )


    fun doTestWithContext(src: String) = doTest(
        """
        // ---- [context]
        import com.google.r4a.*

        class Foo : Component() {
            override fun compose() {}
        }

        // ---- [/context]

        // ---- [test]
        @Composable fun method() {
            ${src.trimIndent()}
        }
        // ---- [/test]
        """.trimIndent()
    )

}

