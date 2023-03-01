import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.common.TextContainerNode
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
    fun testTakeVCAllPublic() = runTest {
        val root = composeText {
            TakeVCAllPublic(VCAllPublic(100))
        }

        assertEquals("root:{Value = VCAllPublic(value=100)}", root.dump())
    }

    @Test
    fun testTakeVCInternalVal() = runTest {
        val root = composeText {
            TakeVCInternalVal(VCInternalVal(200))
        }

        assertEquals("root:{Value = VCInternalVal(value=200)}", root.dump())
    }

    // TODO: testTakeVCPrivateVal is failing with k/native

    // The root cause java.lang.IllegalStateException was thrown at: org.jetbrains.kotlin.backend.konan.BoxingKt.getTypeConversionImpl(Boxing.kt:42)
    //	at org.jetbrains.kotlin.backend.common.CodegenUtil.reportBackendException(CodegenUtil.kt:241)
    // ..
    // Caused by: java.lang.IllegalStateException: actual type is VCPrivateVal, expected kotlin.Int
    //	at org.jetbrains.kotlin.backend.konan.BoxingKt.getTypeConversionImpl(Boxing.kt:42)

//    @Test
//    fun testTakeVCPrivateVal() = runTest {
//        val root = composeText {
//            TakeVCPrivateVal(VCPrivateVal(300))
//        }
//
//        assertEquals("root:{Value = VCPrivateVal(value=300)}", root.dump())
//    }

    @Test
    fun testTakeVCInternalCtor() = runTest {
        val root = composeText {
            TakeVCInternalCtor(VCInternalCtor.V1)
        }

        assertEquals("root:{Value = VCInternalCtor(value=1)}", root.dump())
    }

    @Test
    fun testTakeVCInternalAll() = runTest {
        var v: VCInternalAll by mutableStateOf(VCInternalAll.V1)

        val job = Job()
        val root = composeText(coroutineContext + job) {
            TakeVCInternalAll(v)
        }

        assertEquals("root:{Value = VCInternalAll(value=111)}", root.dump())

        v = VCInternalAll.V2
        testScheduler.advanceUntilIdle()

        assertEquals("root:{Value = VCInternalAll(value=222)}", root.dump())
        job.cancel()
    }

    // TODO: testTakeVCPrivateCtor is failing with k/native

    // Caused by: java.lang.IllegalStateException: actual type is VCPrivateCtor, expected kotlin.Int
    //at org.jetbrains.kotlin.backend.konan.BoxingKt.getTypeConversionImpl(Boxing.kt:42)
//    @Test
//    fun testTakeVCPrivateCtor() = runTest {
//        var v: VCPrivateCtor by mutableStateOf(VCPrivateCtor.V1)
//
//        val job = Job()
//        val root = composeText(coroutineContext + job) {
//            TakeVCPrivateCtor(v)
//        }
//
//        assertEquals("root:{Value = VCPrivateCtor(value=1111)}", root.dump())
//
//        v = VCPrivateCtor.V2
//        testScheduler.advanceUntilIdle()
//
//        assertEquals("root:{Value = VCPrivateCtor(value=2222)}", root.dump())
//        job.cancel()
//    }


    // TODO: testTakeVCPrivateCtorInternalVal is failing with k/native
    // Caused by: java.lang.IllegalStateException: actual type is VCPrivateCtorInternalVal, expected kotlin.Int
//    @Test
//    fun testTakeVCPrivateCtorInternalVal() = runTest {
//        var v: VCPrivateCtorInternalVal by mutableStateOf(VCPrivateCtorInternalVal.V1)
//
//        val job = Job()
//        val root = composeText(coroutineContext + job) {
//            TakeVCPrivateCtorInternalVal(v)
//        }
//
//        assertEquals("root:{Value = VCPrivateCtorInternalVal(value=101)}", root.dump())
//
//        v = VCPrivateCtorInternalVal.V2
//        testScheduler.advanceUntilIdle()
//
//        assertEquals("root:{Value = VCPrivateCtorInternalVal(value=202)}", root.dump())
//        job.cancel()
//    }

    // TODO: testTakeVCPrivateAll is failing with k/native
//    @Test
//    fun testTakeVCPrivateAll() = runTest {
//        var v: VCPrivateAll by mutableStateOf(VCPrivateAll.V1)
//
//        val job = Job()
//        val root = composeText(coroutineContext + job) {
//            TakeVCPrivateAll(v)
//        }
//
//        assertEquals("root:{Value = VCPrivateAll(value=1001)}", root.dump())
//
//        v = VCPrivateAll.V2
//        testScheduler.advanceUntilIdle()
//
//        assertEquals("root:{Value = VCPrivateAll(value=2002)}", root.dump())
//        job.cancel()
//    }

    @Test
    fun testTakeSameModuleVCAllPrivate() = runTest {
        var v: SameModuleVCAllPrivate by mutableStateOf(SameModuleVCAllPrivate.V1)

        val job = Job()
        val root = composeText(coroutineContext + job) {
            TakeSameModuleVCAllPrivate(v)
        }

        assertEquals("root:{Value = SameModuleVCAllPrivate(value=11011)}", root.dump())

        v = SameModuleVCAllPrivate.V2
        testScheduler.advanceUntilIdle()

        assertEquals("root:{Value = SameModuleVCAllPrivate(value=22022)}", root.dump())
        job.cancel()
    }

    @Test
    fun testVCPrivateAllNonPrimitive() = runTest {
        var v: VCPrivateAllNonPrimitive by mutableStateOf(VCPrivateAllNonPrimitive.V1)

        val job = Job()
        val root = composeText(coroutineContext + job) {
            TakeVCPrivateAllNonPrimitive(v)
        }

        assertEquals("root:{Value = VCPrivateAllNonPrimitive(value=V1)}", root.dump())

        v = VCPrivateAllNonPrimitive.V2
        testScheduler.advanceUntilIdle()

        assertEquals("root:{Value = VCPrivateAllNonPrimitive(value=V2)}", root.dump())
        job.cancel()
    }

}
