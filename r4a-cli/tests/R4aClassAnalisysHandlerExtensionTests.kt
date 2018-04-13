class R4aClassAnalisysHandlerExtensionTests: AbstractR4aDiagnosticsTest() {

    fun testReportOpen() {
        doTest("""
            import com.google.r4a.Component;

            open class <!OPEN_COMPONENT!>MyComponent<!> : Component() {
               override fun render() { }
            }
        """)
    }

    fun testAllowClosed() {
        doTest("""
            import com.google.r4a.Component;

            class MyComponent: Component() {
               override fun render() { }
            }
        """)
    }

    fun testReportAbstract() {
        doTest("""
            import com.google.r4a.Component;

            abstract class <!OPEN_COMPONENT!>MyComponent<!>: Component() {
               override fun render() { }
            }
        """)
    }
}

