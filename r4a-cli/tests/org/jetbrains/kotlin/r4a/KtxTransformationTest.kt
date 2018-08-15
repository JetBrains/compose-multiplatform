package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class KtxTransformationTest: AbstractCodeGenTest() {

    override fun setUp() {
        super.setUp()
        KtxTypeResolutionExtension.registerExtension(myEnvironment.project, R4aKtxTypeResolutionExtension())
        KtxControlFlowExtension.registerExtension(myEnvironment.project, R4aKtxControlFlowExtension())
        StorageComponentContainerContributor.registerExtension(myEnvironment.project, ComposableAnnotationChecker())
        TypeResolutionInterceptorExtension.registerExtension(myEnvironment.project, R4aTypeResolutionInterceptorExtension())
        SyntheticIrExtension.registerExtension(myEnvironment.project, R4ASyntheticIrExtension())
//        SyntheticResolveExtension.registerExtension(myEnvironment.project, StaticWrapperCreatorFunctionResolveExtension())
//        SyntheticResolveExtension.registerExtension(myEnvironment.project, WrapperViewSettersGettersResolveExtension())
    }


    fun testEmptyComposeFunction() = testCompile(
        """
        import com.google.r4a.Component

        class Foo : Component() {
            override fun compose() {}
        }
        """
    )

    fun testSingleViewCompose() = testCompile(
        """
        import com.google.r4a.Component
        import android.widget.*

        class Foo : Component() {
            override fun compose() {
                <TextView />
            }
        }
        """
    )

    fun testMultipleRootViewCompose() = testCompile(
        """
        import com.google.r4a.Component
        import android.widget.*

        class Foo : Component() {
            override fun compose() {
                <TextView />
                <TextView />
                <TextView />
            }
        }
        """
    )

    fun testNestedViewCompose() = testCompile(
        """
        import com.google.r4a.Component
        import android.widget.*

        class Foo : Component() {
            override fun compose() {
                <LinearLayout>
                    <TextView />
                    <LinearLayout>
                        <TextView />
                        <TextView />
                    </LinearLayout>
                </LinearLayout>
            }
        }
        """
    )

    fun testSingleComposite() = testCompile(
        """
        import com.google.r4a.Component

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                <Bar />
            }
        }
        """
    )

    fun testMultipleRootComposite() = testCompile(
        """
        import com.google.r4a.Component

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                <Bar />
                <Bar />
                <Bar />
            }
        }
        """
    )

    fun testViewAndComposites() = testCompile(
        """
        import com.google.r4a.Component
        import android.widget.*

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                <LinearLayout>
                    <Bar />
                </LinearLayout>
            }
        }
        """
    )

    fun testAttributes() = testCompile(
        """
        import com.google.r4a.Component
        import android.widget.*

        class Bar : Component() {
            var num: Int = 0
            var a: String = ""
            var b: String = ""
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                val s = "foo" + "bar"
                <LinearLayout orientation=LinearLayout.VERTICAL>
                    <Bar num=123 a=s b="const" />
                </LinearLayout>
            }
        }
        """
    )


    // NOTE: test the key attribute separately as it receives different handling.
    // TODO(lmr): add test in r4a-runtime around behavior of this attribute
    fun testKeyAttributes() = testCompile(
        """
        import com.google.r4a.Component

        class Foo : Component() {
            var key: Int = 0
            override fun compose() {
                <Foo key=123 />
            }
        }
        """
    )

    fun testForEach() = testCompile(
        """
        import com.google.r4a.Component

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                listOf(1, 2, 3).forEach {
                    <Bar />
                }
            }
        }
        """
    )

    fun testForLoop() = testCompile(
        """
        import com.google.r4a.Component

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                for (i in listOf(1, 2, 3)) {
                    <Bar />
                }
            }
        }
        """
    )

    fun testEarlyReturns() = testCompile(
        """
        import com.google.r4a.Component

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            var visible: Boolean = false
            override fun compose() {
                if (!visible) return
                else "" // TODO: Remove this line when fixed upstream
                <Bar />
            }
        }
        """
    )

    fun testConditionalRendering() = testCompile(
        """
        import com.google.r4a.Component
        import java.util.Random

        class Bar : Component() {
            override fun compose() {}
        }

        class Bam : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            var visible: Boolean = false
            override fun compose() {
                if (!visible) {
                    <Bar />
                } else {
                    <Bam />
                }
            }
        }
        """
    )

    fun testFunctionInstanceZeroArgs() = testCompile(
        """
        import com.google.r4a.*

        class Bar : Component() {
            override fun compose() {}
        }
        class Foo: Component() {
            override fun compose() {
                val foo = object: Function0<Unit> {
                    override fun invoke() {
                        <Bar />
                    }
                }
                <foo />
            }
        }
        """
    )

    fun testFunctionInstanceMultipleArgs() = testCompile(
        """
        import com.google.r4a.*

        class Bar : Component() {
            override fun compose() {}
        }
        class Foo: Component() {
            override fun compose() {
                val foo = object: Function2<@kotlin.ParameterName("x") String, @kotlin.ParameterName("y")Int, Unit> {
                    override fun invoke(x: String, y: Int) {
                        <Bar />
                    }
                }
                <foo x="foo" y=123 />
            }
        }
        """
    )

    fun testComposeAttribute() = testCompile(
        """
        import com.google.r4a.*

        class Bar : Component() {
            override fun compose() {}
        }
        class Foo: Component() {
            lateinit var children: () -> Unit
            override fun compose() {
                val children = children
                <children />
            }
        }
        """
    )

    fun testComposeWithParamsAttribute() = testCompile(
        """
        import com.google.r4a.*

        class Bar : Component() {
            override fun compose() {}
        }
        class Foo: Component() {
            lateinit var children: (x: Int) -> Unit
            override fun compose() {
                val children = children
                <children x=123 />
            }
        }
        """
    )


    fun testComposeAttributeFunctionType() = testCompile(
        """
        class X {
            lateinit var composeItem: Function1<@kotlin.ParameterName("arg0") Int, Unit>
            fun fn() {
                val composeItem = composeItem
                <composeItem arg0=123 />
            }
        }
        """
    )

    fun testExtensionFunctions() = testCompile(
        """

        import com.google.r4a.*
        import android.widget.*

        fun LinearLayout.setSomeExtension(x: Int) {
        }
        class X : Component() {
            override fun compose() {
                <LinearLayout someExtension=123 />
            }
        }
        """
    )

    fun testChildrenOfComponent() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class HelperComponent : Component() {
            private lateinit var children: () -> Unit

            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            override fun compose() {
                <children />
            }
        }

        class MainComponent : Component() {
            var name = "World"
            override fun compose() {
                <HelperComponent>
                    <TextView text="some child content2!" />
                    <TextView text="some child content!3" />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenWithTypedParameters() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class HelperComponent : Component() {
            private lateinit var children: (title: String, rating: Int) -> Unit
            @Children fun setChildren2(x: (title: String, rating: Int) -> Unit) { children = x }

            override fun compose() {
                val children = this.children
                <children title="Hello World!" rating=5 />
                <children title="Kompose is awesome!" rating=5 />
                <children title="Bitcoin!" rating=4 />
            }
        }

        class MainComponent : Component() {
            var name = "World"
            override fun compose() {
                <HelperComponent> title: String, rating: Int ->
                    <TextView text=(title+" ("+rating+" stars)") />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenWithUntypedParameters() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class HelperComponent : Component() {
            private lateinit var children: (title: String, rating: Int) -> Unit

            @Children
            fun setChildren2(x: (title: String, rating: Int) -> Unit) { children = x }
            override fun compose() {
                <children title="Hello World!" rating=5 />
                <children title="Kompose is awesome!" rating=5 />
                <children title="Bitcoin!" rating=4 />
            }
        }

        class MainComponent : Component() {
            var name = "World"
            override fun compose() {
                <HelperComponent> title, rating ->
                    <TextView text=(title+" ("+rating+" stars)") />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenCaptureVariables() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class HelperComponent : Component() {
            lateinit private var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            override fun compose() {
                val children = this.children
            }
        }

        class MainComponent : Component() {
            var name = "World"
            override fun compose() {
                val childText = "Hello World!"
                <HelperComponent>
                    <TextView text=childText />
                </HelperComponent>
            }
        }
        """
    )

    fun testChildrenDeepCaptureVariables() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class A : Component() {
            lateinit private var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            override fun compose() {
                val children = this.children
            }
        }

        class B : Component() {
            lateinit private var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            override fun compose() {
                val children = this.children
            }
        }

        class MainComponent : Component() {
            var name = "World"
            override fun compose() {
                val childText = "Hello World!"
                <A>
                    <B>
                        println(childText)
                    </B>
                </A>
            }
        }
        """
    )

    fun testChildrenDeepCaptureVariablesWithParameters() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class A : Component() {
            lateinit private var children: (String) -> Unit
            @Children
            fun setChildren2(x: (String) -> Unit) { children = x }
            override fun compose() {
                val children = this.children
            }
        }

        class B : Component() {
            lateinit private var children: (String) -> Unit
            @Children
            fun setChildren2(x: (String) -> Unit) { children = x }
            override fun compose() {
                val children = this.children
            }
        }

        class MainComponent : Component() {
            var name = "World"
            override fun compose() {
                val childText = "Hello World!"
                <A> x ->
                    <B> y ->
                        println(childText + x + y)
                    </B>
                </A>
            }
        }
        """
    )

    fun testChildrenOfNativeView() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class MainComponent : Component() {
            override fun compose() {
                <LinearLayout>
                    <TextView text="some child content2!" />
                    <TextView text="some child content!3" />
                </LinearLayout>
            }
        }
        """
    )

    fun testIr() = testCompile(
        """
        import android.widget.*
        import com.google.r4a.*

        class HelperComponent : Component() {
            private lateinit var children: () -> Unit
            @Children
            fun setChildren2(x: () -> Unit) { children = x }
            override fun compose() {
                val children = this.children
            }
        }

        class MainComponent : Component() {
            override fun compose() {
                val x = "Hello"
                val y = "World"
                <HelperComponent>
                    for(i in 1..100) {
                        <TextView text=(x+y+i) />
                    }
                </HelperComponent>
            }
        }
        """
    )


    fun testGenericsInnerClass() = testCompile(
        """
        import com.google.r4a.*

        class A<T>(val value: T) {
            inner class Getter : Component() {
                var x: T? = null
                override fun compose() {}
            }
        }

        fun doStuff() {
            val a = A(123)

            // a.Getter() here has a bound type argument through A
            <a.Getter x=456 />
        }
        """
    )

    fun testXGenericInnerClassConstructor() = testCompile(
        """
        import com.google.r4a.*

        class A<T>(val value: T) {
            inner class C : Component() {
                override fun compose() {}
            }
        }

        fun doStuff() {
            val B = A(123)

            <B.C />
        }
        """
    )


    fun testXGenericConstructorParams() = testCompile(
        """
        import com.google.r4a.*

        class A<T>(
            val value: T
        ): Component() {
            var list2: List<T>? = null
            fun setList(list: List<T>) {}
            override fun compose() {}
        }

        fun doStuff() {
            val x = 123

            // we can create element with just value, no list
            <A value=x />

            // if we add a list, it can infer the type
            <A
                value=x
                list=listOf(234, x)
                list2=listOf(234, x)
            />
        }
        """
    )

    fun testSimpleNoArgsComponent() = testCompile(
        """
        import com.google.r4a.*

        class Simple : Component() {
            override fun compose() {}
        }

        fun run() {
            <Simple />
        }
        """
    )

    fun testSimpleVarInConstructor() = testCompile(
        """
        import com.google.r4a.*

        class SimpleConstructorArg(var foo: String) : Component() {
            override fun compose() {}
        }

        fun run() {
            <SimpleConstructorArg foo="string" />
        }
        """
    )

    fun testLateInitProp() = testCompile(
        """
        import com.google.r4a.*

        class SimpleLateInitArg : Component() {
            lateinit var foo: String
            override fun compose() {}
        }

        fun run() {
            <SimpleLateInitArg foo="string" />
        }
        """
    )

    fun testGenericCtorArg() = testCompile(
        """
        import com.google.r4a.*

        class GenericCtorArg<T>(var foo: T) : Component() {
            override fun compose() {}
        }

        fun run() {
            <GenericCtorArg foo="string" />
            <GenericCtorArg foo=123 />
        }
        """
    )

    fun testPropsAndSettersAndExtensionSetters() = testCompile(
        """
        import com.google.r4a.*

        class OneArg : Component() {
            var bar: String? = null
            var baz: String? = null
            fun setBam(bam: String) {
                bar = bam
            }
            override fun compose() {}
        }

        fun OneArg.setJazz(x: String) {}

        fun OneArg.setJazz(y: Int) {}

        fun run() {
            <OneArg bar="string" />
            val bar = "string"
            val num = 123
            <OneArg
                bar
                baz=bar
                bam=bar
                jazz=num
            />
        }
        """
    )

    fun testGenericAttribute() = testCompile(
        """
        import com.google.r4a.*

        class Simple : Component() {
            override fun compose() {}
        }

        class Ref<T> {
            var value: T? = null
        }

        fun <T: Any> T.setRef(ref: Ref<T>) {

        }

        fun run() {
            val ref = Ref<Simple>()
            <Simple ref=ref />
        }
        """
    )

    fun testSimpleFunctionComponent() = testCompile(
        """
        import com.google.r4a.*

        fun OneArg(foo: String) {}

        fun run() {
            <OneArg foo="string" />
        }
        """
    )

    fun testOverloadedFunctions() = testCompile(
        """
        import com.google.r4a.*

        fun OneArg(foo: String) {}
        fun OneArg(foo: Int) {}

        fun run() {
            <OneArg foo=("string") />
            <OneArg foo=123 />
        }
        """
    )

    fun testConstructorVal() = testCompile(
        """
        import com.google.r4a.*

        class Foo(val x: Int): Component() {
            override fun compose() {}
        }

        fun run() {
            <Foo x=123 />
        }
        """
    )

    fun testConstructorNonPropertyParam() = testCompile(
        """
        import com.google.r4a.*

        class Foo(x: Int): Component() {
            override fun compose() {}
        }

        fun run() {
            <Foo x=123 />
        }
        """
    )

    fun testDotQualifiedObjectToClass() = testCompile(
        """
        import com.google.r4a.*

        object Obj {
            class B : Component() {
                override fun compose() {}
            }
        }

        fun run() {
            <Obj.B />
        }
        """
    )

    fun testPackageQualifiedTags() = testCompile(
        """
        fun run() {
            <android.widget.TextView text="bar" />
        }
        """
    )

    fun testDotQualifiedClassToClass() = testCompile(
        """
        import com.google.r4a.*

        class Y {
            class Z {
                class F : Component() {
                    override fun compose() {}
                }
            }
        }

        fun run() {
            <Y.Z.F />
        }
        """
    )

    fun testInnerClass() = testCompile(
        """
        import com.google.r4a.*

        class A(var foo: String) {
            inner class B(var bar: String) : Component() {
                override fun compose() {}
            }
        }

        fun run() {
            val X = A("string")
            <X.B bar="string" />
        }
        """
    )

    fun testGenericInnerClass() = testCompile(
        """
        import com.google.r4a.*

        class A<T>(var foo: T) {
            inner class B(var bar: T) : Component() {
                override fun compose() {}
            }
        }

        fun run() {
            val X = A("string")
            val Y = A(123)
            <X.B bar="string" />
            <Y.B bar=123 />
        }
        """
    )

    fun testLocalLambda() = testCompile(
        """
        import com.google.r4a.*

        class Simple : Component() {
            override fun compose() {}
        }

        fun run() {
            val foo = { <Simple /> }
            <foo />
        }
        """
    )

    fun testPropertyLambda() = testCompile(
        """
        import com.google.r4a.*

        class Test(var children: () -> Unit) : Component() {
            override fun compose() {
                <children />
            }
        }
        """
    )

    fun testLambdaWithArgs() = testCompile(
        """
        import com.google.r4a.*

        class Test(var children: (x: Int) -> Unit) : Component() {
            override fun compose() {
                <children x=123 />
            }
        }
        """
    )

    fun testLocalMethod() = testCompile(
        """
        import com.google.r4a.*

        class Test : Component() {
            fun doStuff() {}
            override fun compose() {
                <doStuff />
            }
        }
        """
    )

    fun testPunningProperty() = testCompile(
        """
        import com.google.r4a.*

        class Simple(var foo: String) : Component() {
            fun setBar(bar: String) {}
            override fun compose() {}
        }

        class Test(var foo: String, var bar: String) : Component() {
            override fun compose() {
                <Simple foo bar />
            }
        }
        """
    )

    fun testPunningLocalVar() = testCompile(
        """
        import com.google.r4a.*

        class Simple() : Component() {
            var bar: String? = null
            fun setFoo(foo: String) {}
            override fun compose() {}
        }

        class Test : Component() {
            override fun compose() {
                val foo = "string"
                val bar = "other"
                <Simple foo bar />
            }
        }
        """
    )

    fun testSimpleLambdaChildrenSetter() = testCompile(
        """
        import com.google.r4a.*
        import android.widget.*
        import android.content.*

        class Example: Component() {
            @Children
            fun setChildren(fn: () -> Unit) {}
            override fun compose() {}
        }

        fun run(text: String) {
            <Example>
                println("hello ${"$"}text")
            </Example>
        }
        """
    )

    fun testBlockChildrenForViews() = testCompile(
        """
        import com.google.r4a.*
        import android.widget.*

        fun run(text: String) {
            <LinearLayout>
                println("hello ${"$"}text")
            </LinearLayout>
        }
        """
    )

    fun testChildrenLambdaWithSingleParam() = testCompile(
        """
        import com.google.r4a.*

        class Example: Component() {
            @Children
            fun setChildren(fn: (x: Int) -> Unit) {}
            override fun compose() {}
        }

        fun run(text: String) {
            <Example> x ->
                println("hello ${"$"}x")
            </Example>
        }
        """
    )

    fun testGenericChildrenArg() = testCompile(
        """
        import com.google.r4a.*

        class Example<T>(var value: T): Component() {
            @Children
            fun setChildren(fn: (x: T) -> Unit) {}
            override fun compose() {}
        }

        fun run(text: String) {
            <Example value="string"> x ->
                println("hello ${"$"}x")
            </Example>
            <Example value=123> x ->
                println("hello ${"$"}{x + 1}")
            </Example>
        }
        """
    )

    fun testFunctionComponentsWithChildrenSimple() = testCompile(
        """
        import com.google.r4a.*

        fun Example(@Children children: () -> Unit) {}

        fun run(text: String) {
            <Example>
                println("hello ${"$"}text")
            </Example>
        }
        """
    )

    fun testFunctionComponentWithChildrenOneArg() = testCompile(
        """
        import com.google.r4a.*

        fun Example(@Children children: (String) -> Unit) {}

        fun run(text: String) {
            <Example> x ->
                println("hello ${"$"}x")
            </Example>
        }
        """
    )

    fun testFunctionComponentWithGenericChildren() = testCompile(
        """
        import com.google.r4a.*

        fun <T> Example(foo: T, @Children children: (T) -> Unit) {}

        fun run(text: String) {
            <Example foo="string"> x ->
                println("hello ${"$"}x")
            </Example>
            <Example foo=123> x ->
                println("hello ${"$"}{x + 1}")
            </Example>
        }
        """
    )

    fun testKtxLambdaInForLoop() = testCompile(
        """
        import com.google.r4a.*
        import android.widget.TextView

        fun foo() {
            val lambda = @Composable {  }
            for(x in 1..5) {
                <lambda />
                <lambda />
            }
        }
        """
    )

    fun testKtxLambdaInIfElse() = testCompile(
        """
        import com.google.r4a.*
        import android.widget.TextView

        fun foo(x: Boolean) {
            val lambda = @Composable { <TextView text="Hello World" /> }
            if(true) {
                <lambda />
                <lambda />
                <lambda />
            } else {
                <lambda />
            }
        }
        """
    )

    fun testLateUsingObjectLiteral() = testCompile(
        """
        import com.google.r4a.*

         class Example: Component() {
             lateinit var callback: (Int) -> Unit
             var index = 0
             override fun compose() {
               <Example callback=(object : Function1<Int, Unit> {
                    override fun invoke(p1: Int) {
                        index = p1
                        recompose()
                    }
                }) />
             }
         }
        """
    )
}