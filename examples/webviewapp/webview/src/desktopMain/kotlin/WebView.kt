/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * fork  from  https://github.com/google/accompanist/blob/main/web/src/main/java/com/google/accompanist/web/WebView.kt
 *
 * use with JavaFx WebView
 */

import LoadingState.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import javafx.application.Platform
import javafx.concurrent.Worker.State.*
import javafx.embed.swing.JFXPanel
import javafx.scene.Scene
import javafx.scene.web.WebView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A wrapper around the JavaFx WebView to provide a basic WebView composable.
 *
 * If you require more customisation you are most likely better rolling your own and using this
 * wrapper as an example.
 *
 * @param state The webview state holder where the Uri to load is defined.
 * @param navigator An optional navigator object that can be used to control the WebView's
 * navigation from outside the composable.
 * @param onCreated Called when the WebView is first created, this can be used to set additional
 * settings on the WebView. WebChromeClient and WebViewClient should not be set here as they will be
 * subsequently overwritten after this lambda is called.
 */
@Composable
internal fun WebView(
    state: WebViewState,
    modifier: Modifier = Modifier,
    navigator: WebViewNavigator = rememberWebViewNavigator(),
    onCreated: (WebView) -> Unit = {},
    onDispose: (WebView) -> Unit = {},
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(webView, navigator) {
        with(navigator) { webView?.handleNavigationEvents() }
    }

    val currentOnDispose by rememberUpdatedState(onDispose)

    webView?.let {
        DisposableEffect(it) {
            onDispose { currentOnDispose(it) }
        }
    }

    SwingPanel(factory = {
        JFXPanel()
    }, modifier = modifier) { jfxP ->
        Platform.runLater {
            val rootVewView = WebView()
            webView = rootVewView
            onCreated.invoke(rootVewView)
            addEngineListener(rootVewView, state, navigator)
            val scene = Scene(rootVewView)
            jfxP.scene = scene
            when (val content = state.content) {
                is WebContent.Url -> {
                    val url = content.url

                    if (url.isNotEmpty() && url != rootVewView.getCurrentUrl()) {
                        rootVewView.load(url)
                    }
                }
                is WebContent.Data -> {
                    rootVewView.loadContent(content.data)
                }
            }
        }
    }
}

private fun addEngineListener(
    root: WebView,
    state: WebViewState,
    navigator: WebViewNavigator
) {
    val engine = root.engine
    engine.loadWorker.exceptionProperty().addListener { _, _, newError ->
        println("page load error : $newError")
        state.errorsForCurrentRequest.add(
            WebViewError(
                engine.getCurrentUrl(),
                newError.message.toString()
            )
        )
    }
    engine.setOnError { error -> println("onError : $error") }
    engine.loadWorker.titleProperty().addListener { observable, oldValue, newValue ->
        println("page load titleProperty : $newValue")
        state.pageTitle = newValue
    }
    engine.loadWorker.progressProperty().addListener { observable, oldValue, newValue ->
        println("page load progressProperty : $newValue")
        if (newValue.toFloat() >= 0f) {
            state.loadingState = Loading(newValue.toFloat())
        }
    }

    //当加载了新的界面
    engine.history.currentIndexProperty().addListener { observable, oldValue, newValue ->
        val url = engine.getCurrentUrl()
        if (url != null &&
            !url.startsWith("data:text/html") &&
            state.content.getCurrentUrl() != url
        ) {
            state.content = state.content.withUrl(url)
        }
    }

    engine.loadWorker.stateProperty().addListener { _, _, newState ->
        println("page load stateProperty : $newState")
        //SCHEDULED ->RUNNING
        when (newState) {
            SUCCEEDED -> {
                state.loadingState = Finished
                navigator.canGoBack = engine.canGoBack()
                navigator.canGoForward = engine.canGoForward()
            }
            FAILED -> {
            }
            RUNNING -> {
                state.loadingState = Loading(0f)
            }
            CANCELLED -> {

            }
            READY, SCHEDULED -> {
                state.loadingState = Initializing
                state.errorsForCurrentRequest.clear()
                state.pageTitle = null
            }

        }
    }
}

actual sealed class WebContent {
    data class Url(
        val url: String,
        val additionalHttpHeaders: Map<String, String> = emptyMap(),
    ) : WebContent()

    data class Data(val data: String) : WebContent()

    fun getCurrentUrl(): String? {
        return when (this) {
            is Url -> url
            is Data -> null
        }
    }
}

internal fun WebContent.withUrl(url: String) = when (this) {
    is WebContent.Url -> copy(url = url)
    else -> WebContent.Url(url)
}


/**
 * A state holder to hold the state for the WebView. In most cases this will be remembered
 * using the rememberWebViewState(uri) function.
 */
@Stable
actual class WebViewState actual constructor(webContent: WebContent) {
    /**
     *  The content being loaded by the WebView
     */
    var content: WebContent by mutableStateOf(webContent)

    /**
     * Whether the WebView is currently [LoadingState.Loading] data in its main frame (along with
     * progress) or the data loading has [LoadingState.Finished]. See [LoadingState]
     */
    var loadingState: LoadingState by mutableStateOf(LoadingState.Initializing)
        internal set

    /**
     * Whether the webview is currently loading data in its main frame
     */
    val isLoading: Boolean
        get() = loadingState !is Finished

    /**
     * The title received from the loaded content of the current page
     */
    var pageTitle: String? by mutableStateOf(null)
        internal set

    /**
     * A list for errors captured in the last load. Reset when a new page is loaded.
     * Errors could be from any resource (iframe, image, etc.), not just for the main page.
     * For more fine grained control use the OnError callback of the WebView.
     */
    val errorsForCurrentRequest: SnapshotStateList<WebViewError> = mutableStateListOf()
}

/**
 * Allows control over the navigation of a WebView from outside the composable. E.g. for performing
 * a back navigation in response to the user clicking the "up" button in a TopAppBar.
 *
 * @see [rememberWebViewNavigator]
 */
@Stable
actual class WebViewNavigator actual constructor(private val coroutineScope: CoroutineScope) {

    private enum class NavigationEvent { BACK, FORWARD, RELOAD, STOP_LOADING }

    private val navigationEvents: MutableSharedFlow<NavigationEvent> = MutableSharedFlow()

    // Use Dispatchers.Main to ensure that the webview methods are called on UI thread
    internal suspend fun WebView.handleNavigationEvents() {
        withContext(Dispatchers.Main) {
            navigationEvents.collect { event ->
                when (event) {
                    NavigationEvent.BACK -> {
                        if (canGoBack) {
                            engine.goBack()
                        }
                    }
                    NavigationEvent.FORWARD -> {
                        if (canGoForward) {
                            engine.goForward()
                        }
                    }
                    NavigationEvent.RELOAD -> engine.reload()
                    NavigationEvent.STOP_LOADING -> engine.stopLoading()
                }
            }
        }
    }

    /**
     * True when the web view is able to navigate backwards, false otherwise.
     */
    var canGoBack: Boolean by mutableStateOf(false)
        internal set

    /**
     * True when the web view is able to navigate forwards, false otherwise.
     */
    var canGoForward: Boolean by mutableStateOf(false)
        internal set

    /**
     * Navigates the webview back to the previous page.
     */
    fun navigateBack() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.BACK) }
    }

    /**
     * Navigates the webview forward after going back from a page.
     */
    fun navigateForward() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.FORWARD) }
    }

    /**
     * Reloads the current page in the webview.
     */
    fun reload() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.RELOAD) }
    }

    /**
     * Stops the current page load (if one is loading).
     */
    fun stopLoading() {
        coroutineScope.launch { navigationEvents.emit(NavigationEvent.STOP_LOADING) }
    }
}

/**
 * A wrapper class to hold errors from the WebView.
 */
@Immutable
actual data class WebViewError(
    /**
     * The request the error came from.
     */
    val request: String?,
    /**
     * The error that was reported.
     */
    val error: String
)

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param url The url to load in the WebView
 * @param additionalHttpHeaders Optional, additional HTTP headers that are passed to [WebView.loadUrl].
 *                              Note that these headers are used for all subsequent requests of the WebView.
 */
@Composable
actual fun rememberWebViewState(url: String, additionalHttpHeaders: Map<String, String>): WebViewState =
// Rather than using .apply {} here we will recreate the state, this prevents
    // a recomposition loop when the webview updates the url itself.
    remember(url, additionalHttpHeaders) {
        WebViewState(
            WebContent.Url(
                url = url,
                additionalHttpHeaders = additionalHttpHeaders
            )
        )
    }

/**
 * Creates a WebView state that is remembered across Compositions.
 *
 * @param data The uri to load in the WebView
 * @param baseUrl no work
 */
@Composable
actual fun rememberWebViewStateWithHTMLData(data: String, baseUrl: String?): WebViewState =
    remember(data) {
        WebViewState(WebContent.Data(data))
    }




