package org.jetbrains.kotlin.r4a

import junit.framework.TestCase
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class GenerateWrapperViewTest : AbstractCodeGenTest() {
    override fun setUp() {
        super.setUp()
        KtxTypeResolutionExtension.registerExtension(myEnvironment.project, R4aKtxTypeResolutionExtension())
        SyntheticIrExtension.registerExtension(myEnvironment.project, R4ASyntheticIrExtension())
        SyntheticResolveExtension.registerExtension(myEnvironment.project, StaticWrapperCreatorFunctionResolveExtension())
        SyntheticResolveExtension.registerExtension(myEnvironment.project, WrapperViewSettersGettersResolveExtension())
    }

    fun testKeyAttributes() {
        val klass = loadClass("Foo", """
            import com.google.r4a.Component

            class Foo : Component() {
                var key: Int = 0
                override fun compose() {
                    <Foo key={123} />
                }
            }
        """)
    }

    fun testWrapperViewGeneration() {

        val klass = loadClass("MainComponent", """
            import android.app.Activity
            import android.os.Bundle
            import com.google.r4a.Component
            import com.google.r4a.CompositionContext

            class MainActivity : Activity() {
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    val inst = MainComponent.createInstance(this)
                    inst.setFoo("string")
                    setContentView(inst)
                }
            }

            class MainComponent : Component() {
                lateinit var foo: String
                override fun compose() {}
            }
        """)

        val wrapperClass = klass.declaredClasses.find { it.name == "MainComponent\$MainComponentWrapperView" }
        TestCase.assertNotNull("wrapper view gets generated", wrapperClass)
        if (wrapperClass == null) return
        TestCase.assertEquals("Wrapper view subclasses LinearLayout", "android.widget.LinearLayout", wrapperClass.superclass.name)
        val setFoo = wrapperClass.declaredMethods.find { it.name == "setFoo" }
        TestCase.assertNotNull("has a setter method for properties", setFoo)

        val companionClass = klass.declaredClasses.find { it.name == "MainComponent\$R4H-StaticRenderCompanion" }
        TestCase.assertNotNull("companion class gets generated", companionClass)
        if (companionClass == null) return
        val createInstanceFn = companionClass.declaredMethods.find { it.name == "createInstance" }
        TestCase.assertNotNull("createInstance function gets generated", createInstanceFn)
    }
}