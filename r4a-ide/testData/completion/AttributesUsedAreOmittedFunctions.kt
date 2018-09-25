import com.google.r4a.*

@Composable fun FunComponent(a: Int, b: Int, c: Int) {}

fun f(){
    <FunComponent a=1 b=2 <caret> />
}

// EXIST: { lookupString: "c" }
// NOTHING_ELSE
