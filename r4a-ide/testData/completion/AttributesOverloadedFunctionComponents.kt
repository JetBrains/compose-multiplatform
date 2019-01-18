import com.google.r4a.*

@Composable fun Foo(abc: Int, xyz: String) {

}

@Composable fun Foo(def: String) {

}

fun f(){
    <Foo <caret> />
}

// EXIST: { lookupString: "abc", tailText: "=... (required)", typeText: "Int" }
// EXIST: { lookupString: "def", tailText: "=... (required)", typeText: "String" }
// EXIST: { lookupString: "xyz", tailText: "=... (required)", typeText: "String" }
// NOTHING_ELSE
