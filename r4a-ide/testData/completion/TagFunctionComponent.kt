import com.google.r4a.*

@Composable fun FunComponent() {}

fun f(){
    <<caret>
}

// EXIST: { lookupString: "FunComponent", itemText: "<FunComponent />" }