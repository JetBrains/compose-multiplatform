import com.google.r4a.*

class ClassComponent : Component() {
    var a = 123
    var b = 123
    var c = 123
    @Children
    var children: (() -> Unit)? = null
    override fun compose() {}
}

fun f(){
    <ClassComponent key=123 a=1 b=2 <caret> ></ClassComponent>
}

// EXIST: { lookupString: "c" }
// NOTHING_ELSE
