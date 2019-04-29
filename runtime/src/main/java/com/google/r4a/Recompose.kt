package com.google.r4a

class RecomposeHelper : Function0<Unit> {

    var isComposing = false
    var recompose: () -> Unit = { throw Error("Recompose not yet initialized") }

    override fun invoke() {
        recompose()
    }
}

@Composable
fun Recompose(@Children body: @Composable() (recompose: () -> Unit) -> Unit) {
    val composer = currentComposerNonNull
    val recomposer = RecomposeHelper()
    val callback = composer.startJoin(false) {
        recomposer.isComposing = true; body(recomposer); recomposer.isComposing = false
    }
    recomposer.recompose = { if (!recomposer.isComposing) callback(false) }
    recomposer.isComposing = true
    body(recomposer)
    recomposer.isComposing = false
    composer.doneJoin(false)
}
