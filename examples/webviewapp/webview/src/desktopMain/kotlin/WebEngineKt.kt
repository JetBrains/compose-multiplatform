import javafx.scene.web.WebEngine
import javafx.scene.web.WebView


fun WebView.load(url:String){
    engine.load(url)
}

fun WebView.loadContent(content: String){
    engine.loadContent(content)
}

fun WebView.stopLoading() {
    engine.stopLoading()
}

fun WebView.goForward() {
    engine.goForward()
}

fun WebView.goBack() {
    engine.goBack()
}

fun WebView.canGoBack(): Boolean {
    return engine.canGoBack()
}

fun WebView.canGoForward(): Boolean {
    return engine.canGoForward()
}


fun WebView.getCurrentUrl(): String? {
    return engine.getCurrentUrl()
}

fun WebView.goRoot(){
    engine.history.go(-engine.history.currentIndex)
}


internal fun WebEngine.getCurrentUrl(): String? {
    if (history.entries.size <= 0) return null
    return history.entries[history.currentIndex].url
}

internal fun WebEngine.stopLoading() {
    loadWorker.cancel()
}

internal fun WebEngine.goForward() {
    if (canGoForward()){
        history.go(1)
    }
}

internal fun WebEngine.goBack() {
    if (canGoBack()){
        history.go(-1)
    }
}

internal fun WebEngine.canGoBack(): Boolean {
    return history.maxSize > 0 && history.currentIndex != 0
}

internal fun WebEngine.canGoForward(): Boolean {
    return history.maxSize > 0 && history.currentIndex != history.maxSize - 1
}

