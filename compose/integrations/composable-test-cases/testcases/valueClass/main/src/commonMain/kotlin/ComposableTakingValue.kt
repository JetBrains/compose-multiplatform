import androidx.compose.runtime.Composable
import com.example.common.TextLeafNode
import kotlin.jvm.JvmInline

@Composable
fun TakeVCAllPublic(a: VCAllPublic) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCNestedVCAllPublic(a: VCNestedVCAllPublic) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCInternalVal(a: VCInternalVal) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCPrivateVal(a: VCPrivateVal) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCNestedVCPrivateVal(a: VCNestedVCPrivateVal) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCInternalCtor(a: VCInternalCtor) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCInternalAll(a: VCInternalAll) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCPrivateCtor(a: VCPrivateCtor) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCPrivateCtorInternalVal(a: VCPrivateCtorInternalVal) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCPrivateAll(a: VCPrivateAll) {
    TextLeafNode("Value = $a")
}
@Composable
fun TakeVCPrivateAllNonPrimitive(a: VCPrivateAllNonPrimitive) {
    TextLeafNode("Value = $a")
}

@Composable
fun TakeVCPrivateAllWithDefaultValue(myabcaccc: VCPrivateAll = VCPrivateAll.V1) {
    TextLeafNode("Value = $myabcaccc")
}

@JvmInline
value class SameModuleVCAllPrivate private constructor(private val value: Int) {
    companion object {
        val V1 = SameModuleVCAllPrivate(11011)
        val V2 = SameModuleVCAllPrivate(22022)
    }
}

@Composable
fun TakeSameModuleVCAllPrivate(a: SameModuleVCAllPrivate) {
    TextLeafNode("Value = $a")
}
