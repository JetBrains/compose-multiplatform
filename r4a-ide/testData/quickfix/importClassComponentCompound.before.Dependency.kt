package foobar

import com.google.r4a.*

class MyComponent(@Children var children: @Composable() () -> Unit) : Component() {
    override fun compose() {}
}
