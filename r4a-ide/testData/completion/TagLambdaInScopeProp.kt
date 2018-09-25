import com.google.r4a.*

class SomeComponent(@Children var children: @Composable() () -> Unit) {
    override fun compose() {
        <childr<caret>
    }
}

// EXIST: { lookupString: "children", itemText: "<children />" }

