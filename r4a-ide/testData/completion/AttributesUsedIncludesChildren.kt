import com.google.r4a.*

@Composable fun FunComponent(a: Int, b: Int, c: Int, @Children d: () -> Unit) {}

fun f(){
    <FunComponent a=1 b=2 <caret> ></FunComponent>
}

// EXIST: { lookupString: "c" }
// NOTHING_ELSE
