import androidx.compose.runtime.Composable
import com.example.common.TextLeafNode

@Composable
fun TakeVCAllPublic(a: VCAllPublic) {
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
