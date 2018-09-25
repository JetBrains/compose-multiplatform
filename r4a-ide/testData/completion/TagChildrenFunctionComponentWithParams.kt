import com.google.r4a.*

@Composable fun FunComponent(@Children children: (x: Int) -> Unit) {}

fun f(){
    <<caret>
}

// EXIST: { lookupString: "FunComponent", itemText: "<FunComponent> x -> ... </FunComponent>" }