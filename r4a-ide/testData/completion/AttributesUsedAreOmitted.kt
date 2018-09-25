import com.google.r4a.*

class ClassComponent: Component() {
    var a = 123
    var b = 234
    var c = 345
    override fun compose() {}
}

fun f(){
    <ClassComponent key=1 a=1 b=2 <caret> />
}

// EXIST: { lookupString: "c" }
// NOTHING_ELSE
