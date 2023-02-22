import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import com.example.common.TextLeafNode
import com.example.common.composeText
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class Tests {

    @Test
    fun testClassTakesComposablePrivateVal() = runTest {
        val impl = ClassTakesComposablePrivateVal {
            TextLeafNode("ClassTakesComposablePrivateVal")
        }

        val root = composeText {
            impl.callPrivateComposablePassedIntoConstructor()
        }

        assertEquals("root:{ClassTakesComposablePrivateVal}", root.dump())
    }

    @Test
    fun testImplementsHasComposable() = runTest {
        val impl = ImplementsHasComposable {
            TextLeafNode("ImplementsHasComposable")
        }

        val root = composeText {
            impl.composable()
        }

        assertEquals("root:{ImplementsHasComposable}", root.dump())
    }

    @Test
    fun testImplementsHasComposableTyped() = runTest {
        val impl = ImplementsHasComposableTyped<String> {
            TextLeafNode("ImplementsHasComposableTyped-$it")
        }

        val root = composeText {
            impl.composable("Hello")
        }

        assertEquals("root:{ImplementsHasComposableTyped-Hello}", root.dump())
    }

    @Test
    fun testClassSavesComposableIntoVar() = runTest {
        val impl = ClassSavesComposableIntoVar {
            TextLeafNode("ClassSavesComposableIntoVar")
        }

        val job = Job()
        var scope: RecomposeScope? = null
        val root = composeText(coroutineContext + job) {
            scope = currentRecomposeScope
            impl.composableVar()
        }

        assertEquals("root:{ClassSavesComposableIntoVar}", root.dump())

        impl.composableVar = {
            TextLeafNode("NewClassSavesComposableIntoVar")
        }
        scope!!.invalidate()

        testScheduler.advanceUntilIdle()
        assertEquals("root:{NewClassSavesComposableIntoVar}", root.dump())
        job.cancel()
    }

    @Test
    fun testClassSavesComposableIntoLateinitVar() = runTest {
        val impl = ClassSavesComposableIntoLateinitVar {
            TextLeafNode("ClassSavesComposableIntoLateinitVar")
        }

        var scope: RecomposeScope? = null
        val job = Job()
        val root = composeText(coroutineContext + job) {
            scope = currentRecomposeScope
            impl.composableVar()
        }

        assertEquals("root:{ClassSavesComposableIntoLateinitVar}", root.dump())

        impl.composableVar = {
            TextLeafNode("NewClassSavesComposableIntoLateinitVar")
        }
        scope!!.invalidate()

        testScheduler.advanceUntilIdle()
        assertEquals("root:{NewClassSavesComposableIntoLateinitVar}", root.dump())
        job.cancel()
    }

    @Test
    fun testClassSavesComposableIntoNullableVar() = runTest {
        val impl = ClassSavesComposableIntoNullableVar {
            TextLeafNode("ClassSavesComposableIntoNullableVar")
        }

        var scope: RecomposeScope? = null
        val job = Job()
        val root = composeText(coroutineContext + job) {
            scope = currentRecomposeScope
            impl.composableVar?.invoke()
        }

        assertEquals("root:{ClassSavesComposableIntoNullableVar}", root.dump())

        impl.composableVar = null
        scope!!.invalidate()

        testScheduler.advanceUntilIdle()
        assertEquals("root:{}", root.dump())
        job.cancel()
    }

    @Test
    fun testClassSavesTypedComposableIntoVar() = runTest {
        val impl = ClassSavesTypedComposableIntoVar<String> {
            TextLeafNode("ClassSavesTypedComposableIntoVar-$it")
        }

        var scope: RecomposeScope? = null
        val job = Job()
        val root = composeText(coroutineContext + job) {
            scope = currentRecomposeScope
            impl.composableVar("abc")
        }

        assertEquals("root:{ClassSavesTypedComposableIntoVar-abc}", root.dump())

        impl.composableVar = {
            TextLeafNode("recomposed-$it")
        }
        scope!!.invalidate()

        testScheduler.advanceUntilIdle()
        assertEquals("root:{recomposed-abc}", root.dump())
        job.cancel()
    }

    @Test
    fun testClassSavesTypedComposableIntoLateinitVar() = runTest {
        val impl = ClassSavesTypedComposableIntoLateinitVar<String> {
            TextLeafNode("ClassSavesTypedComposableIntoLateinitVar-$it")
        }

        var scope: RecomposeScope? = null
        val job = Job()
        val root = composeText(coroutineContext + job) {
            scope = currentRecomposeScope
            impl.composableVar("abc")
        }

        assertEquals("root:{ClassSavesTypedComposableIntoLateinitVar-abc}", root.dump())

        impl.composableVar = {
            TextLeafNode("recomposed-$it")
        }
        scope!!.invalidate()

        testScheduler.advanceUntilIdle()
        assertEquals("root:{recomposed-abc}", root.dump())
        job.cancel()
    }

    @Test
    fun testClassWithSecondaryConstructorSavesComposable() = runTest {
        val impl = ClassWithSecondaryConstructorSavesComposable()

        val root = composeText {
            impl.c()
        }

        assertEquals("root:{SecondaryConstructor}", root.dump())
    }

    @Test
    fun testDataClassTakesValComposable() = runTest {
        val impl = DataClassTakesValComposable {
            TextLeafNode("DataClassTakesValComposable")
        }

        val root = composeText {
            impl.c()
        }

        assertEquals("root:{DataClassTakesValComposable}", root.dump())
    }

    @Test
    fun testDataClassTakesValComposableTyped() = runTest {
        val impl = DataClassTakesValComposableTyped<String> {
            TextLeafNode("DataClassTakesValComposableTyped-$it")
        }

        val root = composeText {
            impl.c("abc")
        }

        assertEquals("root:{DataClassTakesValComposableTyped-abc}", root.dump())
    }

    @Test
    fun testDataClassTakesVarComposable() = runTest {
        val impl = DataClassTakesVarComposable {
            TextLeafNode("DataClassTakesVarComposable")
        }

        val root = composeText {
            impl.c()
        }

        assertEquals("root:{DataClassTakesVarComposable}", root.dump())
    }

    @Test
    fun testClassTakesValComposable() = runTest {
        val impl = ClassTakesValComposable {
            TextLeafNode("ClassTakesValComposable")
        }

        val root = composeText {
            impl.c()
        }

        assertEquals("root:{ClassTakesValComposable}", root.dump())
    }

    @Test
    fun testClassTakesValComposableTyped() = runTest {
        val impl = ClassTakesValComposableTyped<Int> {
            TextLeafNode("ClassTakesValComposableTyped-$it")
        }

        val root = composeText {
            impl.c(100)
        }

        assertEquals("root:{ClassTakesValComposableTyped-100}", root.dump())
    }

    @Test
    fun testClassTakesVarComposable() = runTest {
        val impl = ClassTakesVarComposable {
            TextLeafNode("ClassTakesVarComposable")
        }

        val root = composeText {
            impl.c()
        }

        assertEquals("root:{ClassTakesVarComposable}", root.dump())
    }

    @Test
    fun testDataClassTakesValStringAndComposable() = runTest {
        val impl = DataClassTakesValStringAndComposable("Abc") {
            TextLeafNode("DataClassTakesValStringAndComposable-$s")
        }

        val root = composeText {
            with(impl) {
                c()
            }
        }

        assertEquals("root:{DataClassTakesValStringAndComposable-Abc}", root.dump())
    }

    @Test
    fun testClassTakesValStringAndComposable() = runTest {
        val impl = ClassTakesValStringAndComposable("Abc2") {
            TextLeafNode("ClassTakesValStringAndComposable-$s")
        }

        val root = composeText {
            with(impl) {
                c()
            }
        }

        assertEquals("root:{ClassTakesValStringAndComposable-Abc2}", root.dump())
    }

    @Test
    fun testClassSavesStringAndComposableIntoVar() = runTest {
        val impl = ClassSavesStringAndComposableIntoVar("Abc3") {
            TextLeafNode("ClassSavesStringAndComposableIntoVar-${this.stringVar}")
        }

        val root = composeText {
            with(impl) { impl.composableVar() }
        }

        assertEquals("root:{ClassSavesStringAndComposableIntoVar-Abc3}", root.dump())
    }

    @Test
    fun testGlobalComposableLambdaToShowText() = runTest {
        val root = composeText {
            GlobalComposableLambdaToShowText {
                "TextReturnedFromALambda"
            }
        }
        assertEquals("root:{TextReturnedFromALambda}", root.dump())
    }

    @Test
    @Ignore // compilation fails on desktop only
    fun testValueClass() = runTest {
//        val impl = ComposableContent { TextLeafNode("ValueClassComposableContent") }
//
//        val root = composeText {
//            impl.content()
//        }
//
//        assertEquals("root:{ValueClassComposableContent}", root.dump())
    }
}
