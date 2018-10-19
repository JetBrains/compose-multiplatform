package org.jetbrains.kotlin.r4a


class KtxTypeResolutionTests : AbstractR4aDiagnosticsTest() {

    fun testThatUnresolvedTagDiagnosticIsOnlyOnTagName() = doTest(
        """
            import com.google.r4a.*

            class Foo : Component() {
                override fun compose() {}
            }

            @Composable fun test() {
                <<!UNRESOLVED_TAG!>SomeNameThatWillNotResolve<!> foo=123>
                    <Foo />
                </<!UNRESOLVED_TAG!>SomeNameThatWillNotResolve<!>>
            }
        """
    )

    fun testMissingAttributes() = doTest(
        """
            import com.google.r4a.*

            data class Foo(val value: Int)

            @Composable fun A(x: Foo) { println(x) }
            class B(var x: Foo) : Component() { override fun compose() { println(x) } }
            class C(x: Foo) : Component() { init { println(x) } override fun compose() { } }
            class D(val x: Foo) : Component() { override fun compose() { println(x) } }
            class E : Component() {
                lateinit var x: Foo
                override fun compose() { println(x) }
            }

            // NOTE: It's important that the diagnostic be only over the tag target, and not the entire element
            // so that a single error doesn't end up making a huge part of an otherwise correct file "red".
            @Composable fun Test(F: @Composable() (x: Foo) -> Unit) {
                // NOTE: constructor attributes and fn params get a "missing parameter" diagnostic
                <<!NO_VALUE_FOR_PARAMETER!>A<!> />
                <<!NO_VALUE_FOR_PARAMETER!>B<!> />
                <<!NO_VALUE_FOR_PARAMETER!>C<!> />
                <<!NO_VALUE_FOR_PARAMETER!>D<!> />

                // NOTE: lateinit attributes get a custom "missing required attribute" diagnostic
                <<!MISSING_REQUIRED_ATTRIBUTES!>E<!> />

                // local
                <<!NO_VALUE_FOR_PARAMETER!>F<!> />

                val x = Foo(123)

                <A x />
                <B x />
                <C x />
                <D x />
                <E x />
                <F x />
            }

        """.trimIndent()
    )

    fun testDuplicateAttributes() = doTest(
        """
            import com.google.r4a.*

            data class Foo(val value: Int)

            @Composable fun A(x: Foo) { println(x) }
            class B(var x: Foo) : Component() { override fun compose() { println(x) } }
            class C(x: Foo) : Component() { init { println(x) } override fun compose() { } }
            class D(val x: Foo) : Component() { override fun compose() { println(x) } }
            class E : Component() {
                lateinit var x: Foo
                override fun compose() { println(x) }
            }

            @Composable fun Test() {
                val x = Foo(123)

                // NOTE: It's important that the diagnostic be only over the attribute key, so that
                // we don't make a large part of the elements red when the type is otherwise correct
                <A x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <B x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <C x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <D x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
                <E x=x <!DUPLICATE_ATTRIBUTE!>x<!>=x />
            }

        """.trimIndent()
    )

    fun testChildrenNamedAndBodyDuplicate() = doTest(
        """
            import com.google.r4a.*

            @Composable fun A(@Children children: @Composable() () -> Unit) { <children /> }
            class B(@Children var children: @Composable() () -> Unit) : Component() { override fun compose() { <children /> } }
            class C: Component() {
                @Children var children: @Composable() () -> Unit = {}
                override fun compose() { <children /> }
            }
            class D: Component() {
                @Children lateinit var children: @Composable() () -> Unit
                override fun compose() { <children /> }
            }

            @Composable fun Test() {
                <A <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></A>
                <B <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></B>
                <C <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></C>
                <D <!CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE!>children<!>={}></D>
            }

        """.trimIndent()
    )

    fun testAbstractClassTags() = doTest(
        """
            import com.google.r4a.*
            import android.content.Context
            import android.widget.LinearLayout

            abstract class <!OPEN_COMPONENT!>Foo<!> : Component() {}

            abstract class Bar(context: Context) : LinearLayout(context) {}

            @Composable fun Test() {
                <<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Foo<!> />
                <<!CREATING_AN_INSTANCE_OF_ABSTRACT_CLASS!>Bar<!> />
            }

        """.trimIndent()
    )


    fun testOverloadedTargets() = doTest(
        """
            import com.google.r4a.*

            data class FooModel(val x: Int, val y: Double)

            class Foo(model: FooModel) : Component() {
                init { println(model) }
                constructor(x: Int, y: Double) : this(FooModel(x, y))

                override fun compose() {}
            }


            @Composable fun Bar(x: Int, y: Double) { <Bar model=FooModel(x, y) /> }
            @Composable fun Bar(model: FooModel) { println(model) }

            @Composable fun Test() {
                val x = 1
                val y = 1.0
                val model = FooModel(x, y)

                <Foo x y />
                <Foo model />
                <Foo x y <!UNRESOLVED_ATTRIBUTE_KEY!>model<!> />

                <Bar x y />
                <Bar model />
                <Bar x y <!UNRESOLVED_ATTRIBUTE_KEY!>model<!> />
            }

        """.trimIndent()
    )

    fun testGenerics() = doTest(
        """
            import com.google.r4a.*

            class A { fun a() {} }
            class B { fun b() {} }

            class Foo<T>(var value: T, var f: (T) -> Unit) : Component() {
                override fun compose() {}
            }

            @Composable fun <T> Bar(value: T, f: (T) -> Unit) { println(value); println(f) }

            @Composable fun Test() {

                val fa: (A) -> Unit = { it -> it.a() }
                val fb: (B) -> Unit = { it -> it.b() }

                <Foo value=A() f={ it.a() } />
                <Foo value=B() f={ it.b() } />
                <Foo value=A() f=fa />
                <Foo value=B() f=fb />
                <Foo value=B() f={ it.<!UNRESOLVED_REFERENCE!>a<!>() } />
                <Foo value=A() f={ it.<!UNRESOLVED_REFERENCE!>b<!>() } />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Foo<!> value=A() f=fb />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Foo<!> value=B() f=fa />


                <Bar value=A() f={ it.a() } />
                <Bar value=B() f={ it.b() } />
                <Bar value=A() f=fa />
                <Bar value=B() f=fb />
                <Bar value=B() f={ it.<!UNRESOLVED_REFERENCE!>a<!>() } />
                <Bar value=A() f={ it.<!UNRESOLVED_REFERENCE!>b<!>() } />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Bar<!> value=A() f=fb />
                <<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>Bar<!> value=B() f=fa />
            }

        """.trimIndent()
    )

    fun testValidInvalidAttributes() = doTest(
        """
            import com.google.r4a.*

            class Foo(val a: Int, var b: Int, c: Int, d: Int = 1) : Component() {
                init { println(c); println(d); }
                var e = 1
                var f: Int? = null
                var g: Int
                    get() = 1
                    set(_: Int) {}
                val h: Int get() = 1
                val i = 1

                fun setJ(j: Int) { println(j) }

                val k by lazy { 123 }
                private var l = 1
                private var m: Int
                    get() = 1
                    set(v: Int) { println(v) }
                private val n = 1
                override fun compose() {}
            }

            @Composable fun Test() {
                <Foo
                    a=1
                    b=1
                    c=1
                    d=1
                    e=1
                    f=null
                    g=1
                    <!UNRESOLVED_ATTRIBUTE_KEY!>h<!>=1
                    <!UNRESOLVED_ATTRIBUTE_KEY!>i<!>=1
                    j=1
                    <!UNRESOLVED_ATTRIBUTE_KEY!>k<!>=1
                    <!UNRESOLVED_ATTRIBUTE_KEY!>z<!>=1
                    <!INVISIBLE_MEMBER!>l<!>=1
                    <!INVISIBLE_MEMBER!>m<!>=1
                    <!UNRESOLVED_ATTRIBUTE_KEY!>n<!>=1
                />
            }

        """.trimIndent()
    )


    fun testMismatchedAttributes() = doTest(
        """
            import com.google.r4a.*

            open class A {}
            class B : A() {}

            class Foo() : Component() {
                var x: A = A()
                var y: A = B()
                var z: B = B()
                override fun compose() {}
            }

            @Composable fun Test() {
                <Foo
                    x=A()
                    y=A()
                    <!MISMATCHED_ATTRIBUTE_TYPE!>z<!>=A()
                />
                <Foo
                    x=B()
                    y=B()
                    z=B()
                />
                <Foo
                    <!MISMATCHED_ATTRIBUTE_TYPE!>x<!>=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>y<!>=1
                    <!MISMATCHED_ATTRIBUTE_TYPE!>z<!>=1
                />
            }

        """.trimIndent()
    )


    fun testErrorAttributeValue() = doTest(
        """
            import com.google.r4a.*

            class Foo() : Component() {
                var x: Int = 1
                override fun compose() {}
            }

            @Composable fun Test() {
                <Foo
                    // TODO(lmr): if the attribute value is unresolved but the attribute name is valid, perhaps we should
                    // not mark the key as unresolved?
                    <!UNRESOLVED_REFERENCE!>x<!>=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>
                    <!UNRESOLVED_REFERENCE!>y<!>=<!UNRESOLVED_REFERENCE!>someUnresolvedValue<!>
                />
            }

        """.trimIndent()
    )

    fun testUnresolvedQualifiedTag() = doTest(
        """
            import com.google.r4a.*

            object MyNamespace {
                class Foo() : Component() {
                    @Children var children: @Composable() () -> Unit = {}
                    override fun compose() { <children /> }
                }
                @Composable fun Bar(@Children children: @Composable() () -> Unit = {}) { <children /> }

                var Baz = @Composable { }

                var someString = ""
                class NonComponent {}
            }

            class Boo {
                class Wat : Component() {
                    @Children var children: @Composable() () -> Unit = {}
                    override fun compose() { <children /> }
                }
                inner class Qoo : Component() {
                    @Children var children: @Composable() () -> Unit = {}
                    override fun compose() { <children /> }
                }
            }

            @Composable fun Test() {
                <MyNamespace.Foo />
                <MyNamespace.Bar />
                <MyNamespace.Baz />
                <MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!> />
                <MyNamespace.<!INVALID_TAG_TYPE!>someString<!> />
                <MyNamespace.<!INVALID_TAG_TYPE!>NonComponent<!> />

                <MyNamespace.Foo></MyNamespace.Foo>
                <MyNamespace.Bar></MyNamespace.Bar>
                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>MyNamespace.Baz<!>></MyNamespace.Baz>
                <MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!>></MyNamespace.<!UNRESOLVED_REFERENCE!>Qoo<!>>
                <MyNamespace.<!INVALID_TAG_TYPE!>someString<!>></MyNamespace.<!INVALID_TAG_TYPE!>someString<!>>

                val obj = Boo()
                <obj.Qoo />
                <Boo.Wat />
                <obj.Qoo></obj.Qoo>
                <Boo.Wat></Boo.Wat>

                <obj.<!INVALID_TAG_TYPE!>Wat<!> />
                <obj.<!INVALID_TAG_TYPE!>Wat<!>></obj.<!INVALID_TAG_TYPE!>Wat<!>>

                <<!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo />
                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!><!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo<!>></<!UNRESOLVED_REFERENCE!>SomethingThatDoesntExist<!>.Foo>
                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>MyNamespace.<!INVALID_TAG_TYPE!>NonComponent<!><!>></MyNamespace.<!INVALID_TAG_TYPE!>NonComponent<!>>

                <MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!> />
                <MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!>></MyNamespace.<!UNRESOLVED_REFERENCE!>Bam<!>>
            }

        """.trimIndent()
    )

    fun testExtensionAttributes() = doTest(
        """
            import com.google.r4a.*

            class Foo() : Component() {
                var x: Int = 1
                override fun compose() {}
            }

            fun Foo.setBar(x: Int) { println(x) }

            fun Foo.setX(s: String) { println(s) }

            @Composable fun Test() {
                <Foo
                    x=1
                />

                <Foo
                    x="a"
                />

                <Foo
                    bar=123
                />

                <Foo
                    <!MISMATCHED_ATTRIBUTE_TYPE!>bar<!>=123.0
                    <!MISMATCHED_ATTRIBUTE_TYPE!>x<!>=123.0
                />
            }

        """.trimIndent()
    )

    fun testChildren() = doTest(
        """
            import com.google.r4a.*
            import android.widget.Button
            import android.widget.LinearLayout

            class ChildrenRequired1(@Children var children: @Composable() () -> Unit) : Component() {
                override fun compose() {}
            }

            @Composable fun ChildrenRequired2(@Children children: @Composable() () -> Unit) { <children /> }

            class ChildrenOptional1(@Children var children: @Composable() () -> Unit = {}) : Component() {
                override fun compose() {}
            }
            class ChildrenOptional2() : Component() {
                @Children var children: @Composable() () -> Unit = {}
                override fun compose() {}
            }

            @Composable fun ChildrenOptional3(@Children children: @Composable() () -> Unit = {}) { <children /> }

            class NoChildren1() : Component() {
                override fun compose() {}
            }
            @Composable fun NoChildren2() {}


            class MultiChildren() : Component() {
                @Children var c1: @Composable() () -> Unit = {}
                @Children var c2: @Composable() (x: Int) -> Unit = {}
                @Children var c3: @Composable() (x: Int, y: Int) -> Unit = { x, y -> println(x + y) }

                override fun compose() {}
            }

            @Composable fun Test() {
                <ChildrenRequired1></ChildrenRequired1>
                <ChildrenRequired2></ChildrenRequired2>
                <<!NO_VALUE_FOR_PARAMETER!>ChildrenRequired1<!> />
                <<!NO_VALUE_FOR_PARAMETER, MISSING_REQUIRED_CHILDREN!>ChildrenRequired2<!> />

                <ChildrenOptional1></ChildrenOptional1>
                <ChildrenOptional2></ChildrenOptional2>
                <ChildrenOptional3></ChildrenOptional3>
                <ChildrenOptional1 />
                <ChildrenOptional2 />
                <ChildrenOptional3 />


                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>NoChildren1<!>></NoChildren1>
                <<!CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED!>NoChildren2<!>></NoChildren2>
                <NoChildren1 />
                <NoChildren2 />

                <MultiChildren></MultiChildren>
                <MultiChildren> x ->
                    println(x)
                </MultiChildren>
                <MultiChildren> x, y ->
                    println(x + y)
                </MultiChildren>
                <<!UNRESOLVED_CHILDREN!>MultiChildren<!>> x, y, z ->
                    println(x + y + z)
                </MultiChildren>

                <Button />
                <LinearLayout />

                <LinearLayout>
                </LinearLayout>

                // TODO(lmr) should this be an error???
                <Button></Button>
            }

        """.trimIndent()
    )

}

