import com.google.r4a.*

class ClassComponent: Component() {
    @Children var children: (() -> Unit)? = null
    override fun compose() {}
}

fun f(){
    <ClassComponent <caret> />
}

// EXIST: { lookupString: "children", tailText: "=... (@Children)", typeText: "(() -> Unit)?" }
