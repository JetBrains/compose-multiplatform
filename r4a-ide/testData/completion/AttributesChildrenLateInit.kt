import com.google.r4a.*

class ClassComponent: Component() {
    lateinit @Children var children: () -> Unit
    override fun compose() {}
}

fun f(){
    <ClassComponent <caret> />
}

// EXIST: { lookupString: "children", tailText: "=... (@Children) (required)", typeText: "() -> Unit" }
