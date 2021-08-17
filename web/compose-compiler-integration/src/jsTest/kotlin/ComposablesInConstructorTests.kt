import androidx.compose.runtime.SideEffect
import kotlinx.browser.document
import org.jetbrains.compose.web.renderComposableInBody
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Classes under tests are defined in jsMain intentionally.
 * The reason is to test cross-module compilation + runtime behaviour.
 *
 * Reporeted Issues:
 * https://github.com/JetBrains/compose-jb/issues/746
 * https://github.com/JetBrains/compose-jb/issues/1052
 */
class ComposablesInConstructorTests {

    @Test
    fun valComposableInDataClass() {
        val d = DataClassTakesValComposable {
            SideEffect {
                document.body!!.innerText = "DataClassTakesValComposable"
            }
        }

        renderComposableInBody {
            d.c()
        }

        assertEquals("DataClassTakesValComposable", document.body!!.innerText)
    }

    @Test
    fun valTypedComposableInDataClass() {
        val d = DataClassTakesValComposableTyped<String> {
            SideEffect {
                document.body!!.innerText = "DataClassTakesValComposableTyped-$it"
            }
        }

        renderComposableInBody {
            d.c("WORKS")
        }

        assertEquals("DataClassTakesValComposableTyped-WORKS", document.body!!.innerText)
    }

    @Test
    fun varComposableInDataClass() {
        val d = DataClassTakesVarComposable {
            SideEffect {
                document.body!!.innerText = "DataClassTakesVarComposable"
            }
        }

        renderComposableInBody {
            d.c()
        }

        assertEquals("DataClassTakesVarComposable", document.body!!.innerText)
    }

    @Test
    fun valComposableInClass() {
        val d = ClassTakesValComposable {
            SideEffect {
                document.body!!.innerText = "ClassTakesValComposable"
            }
        }

        renderComposableInBody {
            d.c()
        }

        assertEquals("ClassTakesValComposable", document.body!!.innerText)
    }

    @Test
    fun valTypedComposableInClass() {
        val d = ClassTakesValComposableTyped<Int> {
            SideEffect {
                document.body!!.innerText = "ClassTakesValComposableTyped-$it"
            }
        }

        renderComposableInBody {
            d.c(100500)
        }

        assertEquals("ClassTakesValComposableTyped-100500", document.body!!.innerText)
    }

    @Test
    fun varComposableInClass() {
        val d = ClassTakesVarComposable {
            SideEffect {
                document.body!!.innerText = "ClassTakesVarComposable"
            }
        }

        renderComposableInBody {
            d.c()
        }

        assertEquals("ClassTakesVarComposable", document.body!!.innerText)
    }

    @Test
    fun implementsHasComposable() {
        val d: HasComposable = ImplementsHasComposable {
            SideEffect {
                document.body!!.innerText = "ImplementsHasComposable"
            }
        }

        renderComposableInBody {
            d.composable()
        }

        assertEquals("ImplementsHasComposable", document.body!!.innerText)
    }

    @Test
    fun implementsHasComposableTyped() {
        val d: HasComposableTyped<Int> = ImplementsHasComposableTyped {
            SideEffect {
                document.body!!.innerText = "ImplementsHasComposableTyped-$it"
            }
        }

        renderComposableInBody {
            d.composable(123456)
        }

        assertEquals("ImplementsHasComposableTyped-123456", document.body!!.innerText)
    }

    @Test
    fun classSavesComposableIntoVar() {
        val d = ClassSavesComposableIntoVar {
            SideEffect {
                document.body!!.innerText = "ClassSavesComposableIntoVar"
            }
        }

        renderComposableInBody {
            d.composableVar()
        }

        assertEquals("ClassSavesComposableIntoVar", document.body!!.innerText)
    }

    @Test
    fun classSavesComposableIntoLateinitVar() {
        val d = ClassSavesComposableIntoLateinitVar {
            SideEffect {
                document.body!!.innerText = "ClassSavesComposableIntoLateinitVar"
            }
        }

        renderComposableInBody {
            d.composableVar()
        }

        assertEquals("ClassSavesComposableIntoLateinitVar", document.body!!.innerText)
    }

    @Test
    fun classSavesComposableIntoNullableVar() {
        val d = ClassSavesComposableIntoNullableVar {
            SideEffect {
                document.body!!.innerText = "ClassSavesComposableIntoNullableVar"
            }
        }

        renderComposableInBody {
            d.composableVar!!.invoke()
        }

        assertEquals("ClassSavesComposableIntoNullableVar", document.body!!.innerText)
    }


    @Test
    fun classSavesTypedComposableIntoVar() {
        val d = ClassSavesTypedComposableIntoVar<String> {
            SideEffect {
                document.body!!.innerText = "ClassSavesTypedComposableIntoVar-$it"
            }
        }

        renderComposableInBody {
            d.composableVar("ABC")
        }

        assertEquals("ClassSavesTypedComposableIntoVar-ABC", document.body!!.innerText)
    }

    @Test
    fun classSavesTypedComposableIntoLateinitVar() {
        val d = ClassSavesTypedComposableIntoLateinitVar<String> {
            SideEffect {
                document.body!!.innerText = "ClassSavesTypedComposableIntoLateinitVar-$it"
            }
        }

        renderComposableInBody {
            d.composableVar("ABC")
        }

        assertEquals("ClassSavesTypedComposableIntoLateinitVar-ABC", document.body!!.innerText)
    }

    @Test
    fun classWithSecondaryConstructorSavesComposable() {
        val d = ClassWithSecondaryConstructorSavesComposable()

        renderComposableInBody {
            d.c()
        }

        assertEquals("Secondary constructor composable content", document.body!!.innerText)
    }

    @Test
    fun dataClassTakesValStringAndComposable() {
        val d = DataClassTakesValStringAndComposable("String1") {
            SideEffect {
                document.body!!.innerText = "DataClassTakesValStringAndComposable-${this.s}"
            }
        }

        renderComposableInBody {
            d.c(d)
        }

        assertEquals("DataClassTakesValStringAndComposable-String1", document.body!!.innerText)
    }

    @Test
    fun classTakesValStringAndComposable() {
        val d = ClassTakesValStringAndComposable("123123") {
            SideEffect {
                document.body!!.innerText = "ClassTakesValStringAndComposable-${this.s}"
            }
        }

        renderComposableInBody {
            d.c(d)
        }

        assertEquals("ClassTakesValStringAndComposable-123123", document.body!!.innerText)
    }

    @Test
    fun classSavesStringAndComposableIntoVar() {
        val d = ClassSavesStringAndComposableIntoVar("098765") {
            SideEffect {
                document.body!!.innerText = "ClassSavesStringAndComposableIntoVar-${this.stringVar}"
            }
        }

        renderComposableInBody {
            d.composableVar(d)
        }

        assertEquals("ClassSavesStringAndComposableIntoVar-098765", document.body!!.innerText)
    }

    @Test
    fun classTakesComposablePrivateVal() {
        val d = ClassTakesComposablePrivateVal {
            SideEffect {
                document.body!!.innerText = "ClassTakesComposablePrivateVal"
            }
        }

        renderComposableInBody {
            d.callPrivateComposablePassedIntoConstructor()
        }

        assertEquals("ClassTakesComposablePrivateVal", document.body!!.innerText)
    }
}
