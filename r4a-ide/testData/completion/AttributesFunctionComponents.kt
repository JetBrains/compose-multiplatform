import com.google.r4a.*

data class SomeData(val bar: Int)

@Composable
fun FunComponent(
    a: Int,
    b: Double,
    c: SomeData,
    @Children d: () -> Unit,
    e: Double = 1.0
) {}


fun f(){
    <FunComponent <caret> />
}

// EXIST: { lookupString: "a", tailText: "=... (required)", typeText: "Int" }
// EXIST: { lookupString: "b", tailText: "=... (required)", typeText: "Double" }
// EXIST: { lookupString: "c", tailText: "=... (required)", typeText: "SomeData" }
// EXIST: { lookupString: "d", tailText: "=... (@Children) (required)", typeText: "() -> Unit" }
// EXIST: { lookupString: "e", tailText: "=...", typeText: "Double" }
// NOTHING_ELSE
