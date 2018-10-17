package foobar

import com.google.r4a.*

object MyNamespace {
    class MyComponent(@Children var children: @Composable() () -> Unit) : Component() {
        override fun compose() {}
    }
}
