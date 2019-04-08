package com.google.r4a

class Recompose : Component() {
    @Children lateinit var body: @Composable() (recompose: () -> Unit) -> Unit

    // TODO(lmr): fix this before merging
    private val localRecompose: () -> Unit = { recompose() }

    @Suppress("PLUGIN_ERROR")
    override fun compose() {
        body(localRecompose)
    }
}
