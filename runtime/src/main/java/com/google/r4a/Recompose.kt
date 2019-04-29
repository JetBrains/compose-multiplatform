package com.google.r4a

private class RecomposeHelper : Function0<Unit> {

    var isComposing = false
    var recompose: () -> Unit = { throw Error("Recompose not yet initialized") }

    override fun invoke() {
        recompose()
    }
}

/**
 * [Recompose] is a component which passes a "recompose" function to its children which, when
 * invoked, will cause its children to recompose. This is useful if you are updating local state
 * and need to cause a recomposition manually.
 *
 * In most cases we recommend using [Model] classes or [state] with immutable types in order to
 * maintain local state inside of composables. For cases where this is impractical, Recompose can
 * help you.
 *
 * Example:
 *
 *     class LoginState(var username: String, var password: String) {
 *       val valid: Boolean get() = username.length > 0 && password.length > 0
 *       fun login() = Api.login(username, password)
 *     }
 *
 *     @Composable
 *     fun LoginScreen() {
 *       val model = +memo { LoginState() }
 *       Recompose { recompose ->
 *         EditText(text=model.username, onTextChange={ model.username = it; recompose() })
 *         EditText(text=model.password, onTextChange={ model.password = it; recompose() })
 *         Button(text="Login", enabled=model.valid, onPress={ model.login() })
 *       }
 *     }
 *
 * Note: The above example can be done without [Recompose] by annotating `LoginState` with [Model].
 *
 * @see Model
 * @see Observe
 * @see invalidate
 */
@Composable
fun Recompose(@Children body: @Composable() (recompose: () -> Unit) -> Unit) {
    val composer = currentComposerNonNull
    val recomposer = RecomposeHelper()
    val callback = composer.startJoin(false) {
        recomposer.isComposing = true
        @Suppress("PLUGIN_ERROR")
        body(recomposer)
        recomposer.isComposing = false
    }
    recomposer.recompose = { if(!recomposer.isComposing) callback(false) }
    recomposer.isComposing = true
    @Suppress("PLUGIN_ERROR")
    body(recomposer)
    recomposer.isComposing = false
    composer.doneJoin(false)
}
