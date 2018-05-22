package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import java.io.File

class KtxTransformationTest: AbstractCodeGenTest() {

    override fun setUp() {
        super.setUp()
        additionalDependencies = additionalDependencies + listOf(File("dependencies/android.jar"))

        KtxTypeResolutionExtension.registerExtension(myEnvironment.project, R4aKtxTypeResolutionExtension())
        SyntheticIrExtension.registerExtension(myEnvironment.project, R4ASyntheticIrExtension())
        SyntheticResolveExtension.registerExtension(myEnvironment.project, StaticWrapperCreatorFunctionResolveExtension())
        SyntheticResolveExtension.registerExtension(myEnvironment.project, WrapperViewSettersGettersResolveExtension())
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
                <LinearLayout orientation={LinearLayout.VERTICAL}>
                    <Bar num={123} a={s} b="const" />
                </LinearLayout>
            }
        }
        """
    )

    fun testAttributeAdapters() = testCompile(
        """
        import com.google.r4a.*
        import com.google.r4a.adapters.*
        import android.widget.*

        object MyAttributeAdapter : AttributeAdapter() {
            fun setSomethingNew(view: LinearLayout, x: String) {}
            fun setSomethingElseNew(component: Bar, x: String) {}
        }

        class Bar : Component() {
            override fun compose() {}
        }

        class Foo : Component() {
            override fun compose() {
                <LinearLayout somethingNew="foo">
                    <Bar somethingElseNew="bar" />
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
                <Foo key={123} />
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
                val foo = object: Function2<String, Int, Unit> {
                    override fun invoke(p1: String, p2: Int) {
                        <Bar />
                    }
                }
                <foo arg0="foo" arg1={123} />
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
            lateinit var children: (Int) -> Unit
            override fun compose() {
                val children = children
                <children arg0={123} />
            }
        }
        """
    )


    fun testComposeAttributeFunctionType() = testCompile(
        """
        class X {
            lateinit var composeItem: Function1<Int, Unit>
            fun fn() {
                val composeItem = composeItem
                <composeItem arg0={123} />
            }
        }
        """
    )

}