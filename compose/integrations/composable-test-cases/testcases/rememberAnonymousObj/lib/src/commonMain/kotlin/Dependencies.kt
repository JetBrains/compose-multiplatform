import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.example.common.TextLeafNode

interface SomeInterface {
    fun abc()
}

// Known to fail with k/js and decoys enabled
// https://github.com/JetBrains/compose-jb/issues/2615
@Composable
fun CallsRememberAnonymousObject() {
    val obj = remember { object {} }
    TextLeafNode("obj1")
}

@Composable
fun CallsRememberAnonymousObjectImplInterface() {
    val obj = remember {
        object : SomeInterface {
            override fun abc() {}
        }
    }
    TextLeafNode("obj2")
}

@Composable
fun CallsRememberAnonymousObjectExplicitType() {
    val obj: SomeInterface = remember {
        object : SomeInterface {
            override fun abc() {}
        }
    }
    TextLeafNode("obj3")
}

@Composable
fun CallsRememberAnonymousObjectExplicitType2() {
    val obj = remember<SomeInterface> {
        object : SomeInterface {
            override fun abc() {}
        }
    }
    TextLeafNode("obj4")
}

@Composable
fun CallsRememberLocalClass() {
    val obj = remember {
        class Abc {}
        Abc()
    }
    TextLeafNode("AbcLocalClass")
}

@Composable
fun CallsRememberLocalClassImplInterface() {
    val obj = remember {
        class Abc : SomeInterface {
            override fun abc() {}
        }
        Abc()
    }
    TextLeafNode("AbcLocalClassImplInterface")
}

@Composable
fun CallsRememberLocalClassImplInterfaceExplicitType() {
    val obj: SomeInterface = remember {
        class Abc : SomeInterface {
            override fun abc() {}
        }
        Abc()
    }
    TextLeafNode("AbcLocalClassImplInterfaceExplicitType")
}

@Composable
fun CallsRememberLocalClassImplInterfaceExplicitType2() {
    val obj = remember<SomeInterface> {
        class Abc : SomeInterface {
            override fun abc() {}
        }
        Abc()
    }
    TextLeafNode("AbcLocalClassImplInterfaceExplicitType2")
}
