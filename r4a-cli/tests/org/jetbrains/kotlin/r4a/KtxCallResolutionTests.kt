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
                  composerCall = fun call(Any, crossinline () -> Foo.Bar, crossinline ViewValidator.(Foo.Bar) -> Boolean, crossinline (Foo.Bar) -> Unit)
                  pivotals = <empty>
                  joinKeyCall = fun joinKey(Any, Any?): Any
                  ctorCall = Bar()
                  ctorParams = <empty>
                  validations = <empty>
                call = NonMemoizedCallNode:
                  resolvedCall = fun invoke()
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
                resolvedCall = fun Foo.invoke()
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
                  composerCall = fun call(Any, crossinline () -> Bar, crossinline ViewValidator.(Bar) -> Boolean, crossinline (Bar) -> Unit)
                  pivotals = <empty>
                  joinKeyCall = fun joinKey(Any, Any?): Any
                  ctorCall = Bar(Int)
                  ctorParams = y
                  validations =
                    - ValidatedAssignment(UPDATE):
                        validationCall = fun update(Int, crossinline (Int) -> Unit): Boolean
                        assignment = fun <set-y>(Int)
                        attribute = y
                    - ValidatedAssignment(CHANGED):
                        validationCall = fun changed(Int): Boolean
                        assignment = <null>
                        attribute = z
                call = NonMemoizedCallNode:
                  resolvedCall = fun invoke(Int)
                  params = z
              usedAttributes = z, y
              unusedAttributes = <children>
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
                  composerCall = fun call(Any, crossinline () -> (Int) -> Bar, crossinline ViewValidator.((Int) -> Bar) -> Boolean, crossinline ((Int) -> Bar) -> Unit)
                  pivotals = <empty>
                  joinKeyCall = fun joinKey(Any, Any?): Any
                  ctorCall = fun Foo(Int): (Int) -> Bar
                  ctorParams = a
                  validations =
                    - ValidatedAssignment(CHANGED):
                        validationCall = fun changed(Int): Boolean
                        assignment = <null>
                        attribute = y
                    - ValidatedAssignment(CHANGED):
                        validationCall = fun changed(Int): Boolean
                        assignment = <null>
                        attribute = z
                call = MemoizedCallNode:
                  memoize = ComposerCallInfo:
                    composerCall = fun call(Any, crossinline () -> Bar, crossinline ViewValidator.(Bar) -> Boolean, crossinline (Bar) -> Unit)
                    pivotals = <empty>
                    joinKeyCall = fun joinKey(Any, Any?): Any
                    ctorCall = fun invoke(Int): Bar
                    ctorParams = y
                    validations =
                      - ValidatedAssignment(UPDATE):
                          validationCall = fun update(Int, crossinline (Int) -> Unit): Boolean
                          assignment = fun <set-y>(Int)
                          attribute = y
                      - ValidatedAssignment(CHANGED):
                          validationCall = fun changed(Int): Boolean
                          assignment = <null>
                          attribute = z
                  call = NonMemoizedCallNode:
                    resolvedCall = fun invoke(Int)
                    params = z
              usedAttributes = z, y, a
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
                resolvedCall = fun invoke(Int)
                params = z
              usedAttributes = z
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
                resolvedCall = fun invoke(Int)
                params = z
              usedAttributes = z
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
                  composerCall = fun call(Any, crossinline () -> Bar, crossinline ViewValidator.(Bar) -> Boolean, crossinline (Bar) -> Unit)
                  pivotals = <empty>
                  joinKeyCall = fun joinKey(Any, Any?): Any
                  ctorCall = Bar(Int)
                  ctorParams = y
                  validations =
                    - ValidatedAssignment(UPDATE):
                        validationCall = fun update(Int, crossinline (Int) -> Unit): Boolean
                        assignment = fun <set-y>(Int)
                        attribute = y
                    - ValidatedAssignment(SET):
                        validationCall = fun set(() -> Unit, crossinline (() -> Unit) -> Unit): Boolean
                        assignment = fun <set-children>(() -> Unit)
                        attribute = <children>
                    - ValidatedAssignment(CHANGED):
                        validationCall = fun changed(Int): Boolean
                        assignment = <null>
                        attribute = z
                call = NonMemoizedCallNode:
                  resolvedCall = fun invoke(Int)
                  params = z
              usedAttributes = z, y, <children>
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
                  composerCall = fun emit(Any, crossinline (Context) -> Button, ViewUpdater<Button>.() -> Unit)
                  pivotals = <empty>
                  joinKeyCall = fun joinKey(Any, Any?): Any
                  ctorCall = Button(Context!)
                  ctorParams = (implicit)context
                  validations =
                    - ValidatedAssignment(SET):
                        validationCall = fun set(String, crossinline Button.(String) -> Unit)
                        assignment = fun setText(CharSequence!)
                        attribute = text
                    - ValidatedAssignment(SET):
                        validationCall = fun set(Boolean, crossinline Button.(Boolean) -> Unit)
                        assignment = fun setEnabled(Boolean)
                        attribute = enabled
              usedAttributes = text, enabled
              unusedAttributes = <empty>
        """
    )

    fun testComposerExtensionMethods() = doTest(
        """
            import com.google.r4a.*
            import android.content.Context

            class C {
                fun joinKey(left: Any, right: Any?): Any = left
            }

            val composer = C()

            open class Foo {}

            class Updater {}

            inline fun <T : Foo> C.emit(
                key: Any,
                crossinline ctor: (Context) -> T,
                update: Updater.() -> Unit
            ) {}

            inline fun <T : Foo> C.emit(
                key: Any,
                crossinline ctor: (Context) -> T,
                update: Updater.() -> Unit,
                children: () -> Unit
            ) {}

            class Bar(context: Context) : Foo

            @Composable
            fun test() {
                <caret><Bar />
            }
        """,
        """
            ResolvedKtxElementCall:
              emitOrCall = EmitCallNode:
                memoize = ComposerCallInfo:
                  composerCall = <null>
                  pivotals = <empty>
                  joinKeyCall = fun joinKey(Any, Any?): Any
                  ctorCall = Bar(Context)
                  ctorParams = (implicit)context
                  validations = <empty>
              usedAttributes = <empty>
              unusedAttributes = <empty>
        """
    )
}

