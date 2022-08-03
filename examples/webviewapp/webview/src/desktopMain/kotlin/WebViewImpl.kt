import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun WebViewImpl(
    state: WebViewState,
    modifier: Modifier,
    navigator: WebViewNavigator
) {

    WebView(state, modifier, navigator, onCreated = {

    }, onDispose = {


    })
}
