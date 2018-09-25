import com.google.r4a.*

class ClassComponent: Component() {
    @Children lateinit var children: () -> Unit
    override fun compose() {}
}

fun f(){
    <<caret>
}

// EXIST: { lookupString: "ClassComponent", itemText: "<ClassComponent>...</ClassComponent>" }

