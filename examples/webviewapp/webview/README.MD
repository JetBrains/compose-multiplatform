## Android And Desktop WebViewImpl

1. [android-web](https://google.github.io/accompanist/web/) - A wrapper around WebView for basic WebView support in
   Jetpack Compose.
2. [JavaFx](https://openjfx.io) - JavaFX is an open source, next generation client application platform for
   desktop, mobile and embedded systems built on Java

### 1.  how to use it

load url
```kotlin
val webViewState = rememberWebViewState("https://example.com")
WebView(
    state = webViewState,
    modifier = Modifier.fillMaxSize(),
    navigator = rememberWebViewNavigator()

)
```
load data
```kotlin
val webViewState = rememberWebViewStateWithHTMLData("test")
WebView(
    state = webViewState,
    modifier = Modifier.fillMaxSize(),
    navigator = rememberWebViewNavigator()
)
```

 