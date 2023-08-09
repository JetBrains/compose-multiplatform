import androidx.compose.runtime.Composable
import com.example.common.TextLeafNode

val funInterfaceWithComposable = FunInterfaceWithComposable {
    TextLeafNode("FunInterfaceWithComposable")
}

val funInterfaceReturnComposable = FunInterfaceReturnComposable {
    { TextLeafNode("FunInterfaceReturnComposable") }
}


class ClassImplementingFunInterface : FunInterfaceWithComposable {
    @Composable
    override fun content() {
        TextLeafNode("ClassImplementingFunInterface")
    }
}

class ClassImplementingFunInterface2 : FunInterfaceReturnComposable {
    override fun getContent() : @Composable () -> Unit {
        return { TextLeafNode("ClassImplementingFunInterface2") }
    }
}

class ClassImplFunInterfaceComposableReturnsComposable : FunInterfaceComposableReturnsComposable {
    @Composable
    override fun getContent() : @Composable () -> Unit {
        return { TextLeafNode("ClassImplFunInterfaceComposableReturnsComposable") }
    }
}
