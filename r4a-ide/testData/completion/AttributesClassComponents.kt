import com.google.r4a.*

data class SomeData(val bar: Int)

fun ClassComponent.setH(h: Int) {}

class ClassComponent(
    a: Int,
    val b: Double,
    var c: SomeData,
    @Children var d: () -> Unit,
    e: Double = 1.0,
): Component() {
    lateinit var f: SomeData
    var g: String = "not required"
    var otherProperty: String = "not required"
    fun setOtherProperty(x: Int){}
    fun setOtherProperty(x: Double){}
    fun setI(i: Int){}
    val j: Int = 123 // not a prop
    private var k: Int = 245
    override fun compose() {}
}

fun f(){
    <ClassComponent <caret> />
}


// EXIST: { lookupString: "a", tailText: "=... (required) (pivotal)", typeText: "Int" }
// EXIST: { lookupString: "b", tailText: "=... (required) (pivotal)", typeText: "Double" }
// EXIST: { lookupString: "c", tailText: "=... (required)", typeText: "SomeData" }
// EXIST: { lookupString: "d", tailText: "=... (@Children) (required)", typeText: "() -> Unit" }
// EXIST: { lookupString: "e", tailText: "=... (pivotal)", typeText: "Double" }
// EXIST: { lookupString: "f", tailText: "=... (required)", typeText: "SomeData" }
// EXIST: { lookupString: "g", tailText: "=...", typeText: "String" }
// EXIST: { lookupString: "h", tailText: "=... (extension on ClassComponent)", typeText: "Int" }
// EXIST: { lookupString: "i", tailText: "=...", typeText: "Int" }
// EXIST: { lookupString: "otherProperty", tailText: "=...", typeText: "Int" }
// EXIST: { lookupString: "otherProperty", tailText: "=...", typeText: "String" }
// EXIST: { lookupString: "otherProperty", tailText: "=...", typeText: "Double" }
// EXIST: { lookupString: "key" }
// ABSENT: { lookupString: "j" }
// ABSENT: { lookupString: "k" }
// NOTHING_ELSE