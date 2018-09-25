import com.google.r4a.*

class ClassComponent: Component() {
    override fun compose() {}
}

fun f(){
    <<caret>
}

// EXIST: { lookupString: "ClassComponent", itemText: "<ClassComponent />" }

