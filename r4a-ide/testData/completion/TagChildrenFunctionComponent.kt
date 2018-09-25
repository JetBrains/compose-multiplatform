import com.google.r4a.*

@Composable fun FunComponent(@Children children: () -> Unit) {}

fun f(){
    <<caret>
}

// EXIST: { lookupString: "FunComponent", itemText: "<FunComponent>...</FunComponent>" }