package org.jetbrains.kotlin.r4a


class KtxCallResolutionTests : AbstractResolvedKtxCallsTest() {

    fun testSomethingQualifiedTag() = doTest(
        """
            import com.google.r4a.*

            object Foo {
                class Bar {
                    @Composable
                    operator fun invoke() {}
                }
            }

            @Composable
            fun test() {
                <caret><Foo.Bar />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = MemoizedCallNode:
                memoize = ComposerCallInfo:
                  pivotals = <empty>
                  joinKeyCall = <null>
                  ctorCall = Bar()
                  ctorParams = <empty>
                  staticAssignments = <empty>
                  validations = <empty>
                call = NonMemoizedCallNode:
                  resolvedCall = operator fun invoke(): Unit
                  params = <empty>
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testReceiverScopeTag() = doTest(
        """
            import com.google.r4a.*

            class Foo {}

            @Composable
            fun test(children: Foo.() -> Unit) {
                val foo = Foo()
                <caret><foo.children />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = NonMemoizedCallNode:
                resolvedCall = operator fun Foo.invoke(): Unit
                params = <empty>
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testSomething() = doTest(
        """
            import com.google.r4a.*

            class Bar(var y: Int = 0) {
                @Composable
                operator fun invoke(z: Int) {

                }
            }

            @Composable
            fun test() {
                <caret><Bar y=2 z=3>
                </Bar>
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = MemoizedCallNode:
                memoize = ComposerCallInfo:
                  pivotals = <empty>
                  joinKeyCall = <null>
                  ctorCall = Bar(y: Int = ...)
                  ctorParams = y
                  staticAssignments = <empty>
                  validations =
                    - ValidatedAssignment(UPDATE):
                        validationCall = <null>
                        assignment = fun <set-y>(<set-?>: Int): Unit
                        attribute = y
                    - ValidatedAssignment(CHANGED):
                        validationCall = <null>
                        assignment = <null>
                        attribute = z
                call = NonMemoizedCallNode:
                  resolvedCall = operator fun invoke(z: Int): Unit
                  params = z
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testNestedCalls() = doTest(
        """
            import com.google.r4a.*

            class Bar(var y: Int = 0) {
                @Composable
                operator fun invoke(z: Int) {

                }
            }

            @Composable
            fun Foo(a: Int): @Composable() (y: Int) -> Bar = { y: Int -> Bar(y) }

            @Composable
            fun test() {
                <caret><Foo a=1 y=2 z=3 />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = MemoizedCallNode:
                memoize = ComposerCallInfo:
                  pivotals = <empty>
                  joinKeyCall = <null>
                  ctorCall = fun Foo(a: Int): (Int) -> Bar
                  ctorParams = a
                  staticAssignments = <empty>
                  validations =
                    - ValidatedAssignment(CHANGED):
                        validationCall = <null>
                        assignment = <null>
                        attribute = y
                    - ValidatedAssignment(CHANGED):
                        validationCall = <null>
                        assignment = <null>
                        attribute = z
                call = MemoizedCallNode:
                  memoize = ComposerCallInfo:
                    pivotals = <empty>
                    joinKeyCall = <null>
                    ctorCall = operator fun invoke(y: Int): Bar
                    ctorParams = y
                    staticAssignments = <empty>
                    validations =
                      - ValidatedAssignment(UPDATE):
                          validationCall = <null>
                          assignment = fun <set-y>(<set-?>: Int): Unit
                          attribute = y
                      - ValidatedAssignment(CHANGED):
                          validationCall = <null>
                          assignment = <null>
                          attribute = z
                  call = NonMemoizedCallNode:
                    resolvedCall = operator fun invoke(z: Int): Unit
                    params = z
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testParameterNamesForInstantiatedClassObjects() = doTest(
        """
            import com.google.r4a.*

            class Bar {
                @Composable
                operator fun invoke(z: Int) {

                }
            }

            @Composable
            fun test() {
                val x = Bar()
                <caret><x z=3 />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = NonMemoizedCallNode:
                resolvedCall = operator fun invoke(z: Int): Unit
                params = z
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testParameterNamesForLambdas() = doTest(
        """
            import com.google.r4a.*

            @Composable
            fun test() {
                val x: (z: Int) -> Unit = { z: Int -> }
                <caret><x z=3 />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = NonMemoizedCallNode:
                resolvedCall = operator fun invoke(z: Int): Unit
                params = z
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testSomethingWithChildren() = doTest(
        """
            import com.google.r4a.*

            class Bar(var y: Int = 0) {
                @Children var children: @Composable() () -> Unit = {}
                @Composable
                operator fun invoke(z: Int) {

                }
            }

            @Composable
            fun test() {
                <caret><Bar y=2 z=3>
                </Bar>
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = MemoizedCallNode:
                memoize = ComposerCallInfo:
                  pivotals = <empty>
                  joinKeyCall = <null>
                  ctorCall = Bar(y: Int = ...)
                  ctorParams = y
                  staticAssignments = <empty>
                  validations =
                    - ValidatedAssignment(UPDATE):
                        validationCall = <null>
                        assignment = fun <set-y>(<set-?>: Int): Unit
                        attribute = y
                    - ValidatedAssignment(SET):
                        validationCall = <null>
                        assignment = fun <set-children>(<set-?>: () -> Unit): Unit
                        attribute = <children>
                    - ValidatedAssignment(CHANGED):
                        validationCall = <null>
                        assignment = <null>
                        attribute = z
                call = NonMemoizedCallNode:
                  resolvedCall = operator fun invoke(z: Int): Unit
                  params = z
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )

    fun testViewResolution() = doTest(
        """
            import com.google.r4a.*
            import android.widget.Button

            @Composable
            fun test() {
                <caret><Button text="some text" enabled=false />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = EmitCallNode:
                memoize = ComposerCallInfo:
                  pivotals = <empty>
                  joinKeyCall = <null>
                  ctorCall = Button(context: Context!)
                  ctorParams = (implicit-<context>)
                  staticAssignments = <empty>
                  validations =
                    - ValidatedAssignment(SET):
                        validationCall = <null>
                        assignment = fun setText(text: CharSequence!): Unit
                        attribute = text
                    - ValidatedAssignment(SET):
                        validationCall = <null>
                        assignment = fun setEnabled(enabled: Boolean): Unit
                        attribute = enabled
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )
}

