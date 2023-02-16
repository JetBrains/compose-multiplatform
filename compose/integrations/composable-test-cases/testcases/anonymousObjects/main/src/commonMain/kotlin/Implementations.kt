import androidx.compose.runtime.Composable
import com.example.common.TextContainerNode
import com.example.common.TextLeafNode

class TestConstructor constructor() {

    var otherComposable: (@Composable () -> Unit)? = null
    constructor(retInt: () -> Int): this() {
        otherComposable = {
            val abc3: HasComposable2 = object : HasComposable2 {
                @Composable
                override fun Abc() {
                    TextContainerNode("div") {
                        val i = retInt()
                        TextLeafNode("Abc-$i")
                    }
                }
            }
            abc3.Abc()
        }
    }
}
