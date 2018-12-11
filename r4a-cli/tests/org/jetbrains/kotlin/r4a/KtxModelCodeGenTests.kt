package org.jetbrains.kotlin.r4a

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.r4a.Component
import com.google.r4a.CompositionContext
import com.google.r4a.isolated
import org.jetbrains.kotlin.extensions.KtxControlFlowExtension
import org.jetbrains.kotlin.extensions.KtxTypeResolutionExtension
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.TypeResolutionInterceptorExtension
import org.jetbrains.kotlin.parsing.KtxParsingExtension
import org.jetbrains.kotlin.psi2ir.extensions.SyntheticIrExtension
import org.jetbrains.kotlin.r4a.frames.FrameTransformExtension
import org.jetbrains.kotlin.r4a.frames.analysis.FrameModelChecker
import org.jetbrains.kotlin.r4a.frames.analysis.PackageAnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config


val PRESIDENT_NAME_1 = "George Washington"
val PRESIDENT_AGE_1 = 57
val PRESIDENT_NAME_16 = "Abraham Lincoln"
val PRESIDENT_AGE_16 = 52

@RunWith(RobolectricTestRunner::class)
@Config(
    manifest = Config.NONE,
    minSdk = 23,
    maxSdk = 23
)
class KtxModelCodeGenTests : AbstractCodeGenTest() {

    @Before
    fun before() {
        val scheduler = RuntimeEnvironment.getMasterScheduler()
        scheduler.pause()
    }

    override fun setUp() {
        isSetup = true
        super.setUp()
        KtxTypeResolutionExtension.registerExtension(myEnvironment.project, R4aKtxTypeResolutionExtension())
        KtxControlFlowExtension.registerExtension(myEnvironment.project, R4aKtxControlFlowExtension())
        StorageComponentContainerContributor.registerExtension(myEnvironment.project, ComposableAnnotationChecker())
        TypeResolutionInterceptorExtension.registerExtension(myEnvironment.project, R4aTypeResolutionInterceptorExtension())
        SyntheticIrExtension.registerExtension(myEnvironment.project, R4ASyntheticIrExtension())
        KtxParsingExtension.registerExtension(myEnvironment.project, R4aKtxParsingExtension())
        AnalysisHandlerExtension.registerExtension(myEnvironment.project, PackageAnalysisHandlerExtension())
        SyntheticIrExtension.registerExtension(myEnvironment.project, FrameTransformExtension())
        StorageComponentContainerContributor.registerExtension(myEnvironment.project, FrameModelChecker())
    }

    private var isSetup = false
    private inline fun <T> ensureSetup(crossinline block: () -> T): T {
        if (!isSetup) setUp()
        return isolated { block() }
    }

    @Test
    fun testCGModelView_PersonModel(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            class Person(var name: String, var age: Int)

            @Composable
            fun PersonView(person: Person) {
              <Observe>
                <TextView text=person.name id=$tvNameId />
                <TextView text=person.age.toString() id=$tvAgeId />
              </Observe>
            }

            val president = Person("$PRESIDENT_NAME_1", $PRESIDENT_AGE_1)
            """, { mapOf("name" to name, "age" to age) }, """
               president.name = name
               president.age = age
            """, """
                <PersonView person=president />
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }

    @Test // b/120836313
    fun testCGModelView_DataModel(): Unit = ensureSetup {
        val tvNameId = 384
        val tvAgeId = 385

        var name = PRESIDENT_NAME_1
        var age = PRESIDENT_AGE_1
        compose(
            """
            @Model
            data class PersonB(var name: String, var age: Int)

            @Composable
            fun PersonView(person: PersonB) {
              <Observe>
                <TextView text=person.name id=$tvNameId />
                <TextView text=person.age.toString() id=$tvAgeId />
              </Observe>
            }

            val president = PersonB("$PRESIDENT_NAME_1", $PRESIDENT_AGE_1)
            """, { mapOf("name" to name, "age" to age) }, """
               president.name = name
               president.age = age
            """, """
                <PersonView person=president />
            """).then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_1, tvName.text)
            assertEquals(PRESIDENT_AGE_1.toString(), tvAge.text)

            name = PRESIDENT_NAME_16
            age = PRESIDENT_AGE_16
        }.then { activity ->
            val tvName = activity.findViewById(tvNameId) as TextView
            val tvAge = activity.findViewById(tvAgeId) as TextView
            assertEquals(PRESIDENT_NAME_16, tvName.text)
            assertEquals(PRESIDENT_AGE_16.toString(), tvAge.text)
        }
    }


    fun compose(prefix: String, valuesFactory: () -> Map<String, Any>, advance: String, composition: String, dumpClasses: Boolean = false): ModelCompositionTest {
        val className = "Test_${uniqueNumber++}"
        val fileName = "$className.kt"

        val candidateValues = valuesFactory()

        @Suppress("NO_REFLECTION_IN_CLASS_PATH")
        val parameterList = candidateValues.map { "${it.key}: ${it.value::class.qualifiedName}" }.joinToString()
        val parameterTypes = candidateValues.map { it.value::class.javaPrimitiveType ?: it.value::class.javaObjectType }.toTypedArray()

        val compiledClasses = classLoader("""
           import android.content.Context
           import android.widget.*
           import com.google.r4a.*

           $prefix

           class $className {

             fun compose() {
               $composition
             }

             fun advance($parameterList) {
               $advance
             }
           }
        """, fileName, dumpClasses)

        val allClassFiles = compiledClasses.allGeneratedFiles.filter { it.relativePath.endsWith(".class") }

        val instanceClass = run {
            var instanceClass: Class<*>? = null
            var loadedOne = false
            for (outFile in allClassFiles) {
                val bytes = outFile.asByteArray()
                val loadedClass = loadClass(this.javaClass.classLoader, null, bytes)
                if (loadedClass.name == className) instanceClass = loadedClass
                loadedOne = true
            }
            if (!loadedOne) error("No classes loaded")
            instanceClass ?: error("Could not find class $className in loaded classes")
        }

        val instanceOfClass = instanceClass.newInstance()
        val advanceMethod = instanceClass.getMethod("advance", *parameterTypes)
        val composeMethod = instanceClass.getMethod("compose")

        return composeModel({ composeMethod.invoke(instanceOfClass) }) {
            val values = valuesFactory()
            val arguments = values.map { it.value as Any }.toTypedArray()
            advanceMethod.invoke(instanceOfClass, *arguments)
        }
    }
}

private class ModelTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply { id = ROOT_ID })
    }
}

private val Activity.root get() = findViewById(ROOT_ID) as ViewGroup

private class ModelRoot : Component() {
    override fun compose() {}
}


class ModelCompositionTest(val composable: () -> Unit, val advance: () -> Unit) {

    inner class ActiveTest(val activity: Activity) {

        fun then(block: (activity: Activity) -> Unit): ActiveTest {
            advance()
            val scheduler = RuntimeEnvironment.getMasterScheduler()
            scheduler.advanceToLastPostedRunnable()
            block(activity)
            return this
        }
    }

    fun then(block: (activity: Activity) -> Unit): ActiveTest {
        val controller = Robolectric.buildActivity(ModelTestActivity::class.java)

        // Compose the root scope
        val activity = controller.create().get()
        val root = activity.root
        val component = ModelRoot()
        val cc = CompositionContext.create(root.context, root, component, null)
        cc.context = activity
        val previous = CompositionContext.current
        CompositionContext.current = cc
        try {
            cc.startRoot()
            composable()
            cc.endRoot()
            cc.applyChanges()
        } finally {
            CompositionContext.current = previous
        }
        block(activity)
        return ActiveTest(activity).then(block)
    }
}

private fun composeModel(composable: () -> Unit, advance: () -> Unit) = ModelCompositionTest(composable, advance)