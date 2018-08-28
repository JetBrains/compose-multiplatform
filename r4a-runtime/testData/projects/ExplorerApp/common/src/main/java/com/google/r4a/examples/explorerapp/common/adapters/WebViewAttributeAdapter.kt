package com.google.r4a.examples.explorerapp.common.adapters

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.KeyEvent
import android.webkit.*


//    certificate
//    textClassifier
//    webChromeClient
//    webViewClient
//    addJavascriptInterface()
//    loadUrl()
//    loadUrl() // additionalHeaders
//    postVisualStateCallback()
//    restoreState(bundle)
//    setDownloadListener()
//    setFindListener()
//    settings
//    settings.setSupportZoom(true)
//    setInitialScale()
//    setNetworkAvailable()
//    setRendererPriorityPolicy(123, false)

class ComposeWebViewClient : WebViewClient() {

    var onPageFinished: ((url: String?) -> Unit)? = null
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onPageFinished?.invoke(url)
    }

    var onReceivedError: ((request: WebResourceRequest?, error: WebResourceError?) -> Unit)? = null
    override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
        super.onReceivedError(view, request, error)
        onReceivedError?.invoke(request, error)
    }

    var onReceivedLoginRequest: ((realm: String?, account: String?, args: String?) -> Unit)? = null
    override fun onReceivedLoginRequest(view: WebView?, realm: String?, account: String?, args: String?) {
        super.onReceivedLoginRequest(view, realm, account, args)
        onReceivedLoginRequest?.invoke(realm, account, args)
    }

    var onReceivedHttpError: ((request: WebResourceRequest?, errorResponse: WebResourceResponse?) -> Unit)? = null
    override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
        super.onReceivedHttpError(view, request, errorResponse)
        onReceivedHttpError?.invoke(request, errorResponse)
    }

    var onPageStarted: ((url: String?, favicon: Bitmap?) -> Unit)? = null
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        onPageStarted?.invoke(url, favicon)
    }

    var onUnhandledKeyEvent: ((event: KeyEvent?) -> Unit)? = null
    override fun onUnhandledKeyEvent(view: WebView?, event: KeyEvent?) {
        super.onUnhandledKeyEvent(view, event)
        onUnhandledKeyEvent?.invoke(event)
    }

    var onReceivedHttpAuthRequest: ((handler: HttpAuthHandler?, host: String?, realm: String?) -> Unit)? = null
    override fun onReceivedHttpAuthRequest(view: WebView?, handler: HttpAuthHandler?, host: String?, realm: String?) {
        super.onReceivedHttpAuthRequest(view, handler, host, realm)
        onReceivedHttpAuthRequest?.invoke(handler, host, realm)
    }

    var onFormResubmission: ((dontResend: Message?, resend: Message?) -> Unit)? = null
    override fun onFormResubmission(view: WebView?, dontResend: Message?, resend: Message?) {
        super.onFormResubmission(view, dontResend, resend)
        onFormResubmission?.invoke(dontResend, resend)
    }

    var onLoadResource: ((url: String?) -> Unit)? = null
    override fun onLoadResource(view: WebView?, url: String?) {
        super.onLoadResource(view, url)
        onLoadResource?.invoke(url)
    }

}

class ComposeWebChromeClient : WebChromeClient() {

    var onJsAlert: ((url: String?, message: String?, result: JsResult?) -> Boolean)? = null
    override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return onJsAlert?.invoke(url, message, result) ?: super.onJsAlert(view, url, message, result)
    }

    var onJsPrompt: ((url: String?, message: String?, defaultValue: String?, result: JsPromptResult?) -> Boolean)? = null
    override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
        return onJsPrompt?.invoke(url, message, defaultValue, result) ?: super.onJsPrompt(view, url, message, defaultValue, result)
    }

    var onGeolocationPermissionsShowPrompt: ((origin: String?, callback: GeolocationPermissions.Callback?) -> Unit)? = null
    override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)
        onGeolocationPermissionsShowPrompt?.invoke(origin, callback)
    }

    var onPermissionRequest: ((request: PermissionRequest?) -> Unit)? = null
    override fun onPermissionRequest(request: PermissionRequest?) {
        super.onPermissionRequest(request)
        onPermissionRequest?.invoke(request)
    }

    var onConsoleMessage: ((consoleMessage: ConsoleMessage?) -> Boolean)? = null
    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        return onConsoleMessage?.invoke(consoleMessage) ?: super.onConsoleMessage(consoleMessage)
    }

    var onPermissionRequestCanceled: ((request: PermissionRequest?) -> Unit)? = null
    override fun onPermissionRequestCanceled(request: PermissionRequest?) {
        super.onPermissionRequestCanceled(request)
        onPermissionRequestCanceled?.invoke(request)
    }

    var onShowFileChooser: ((filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?) -> Boolean)? = null
    override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
        return onShowFileChooser?.invoke(filePathCallback, fileChooserParams) ?: super.onShowFileChooser(webView, filePathCallback, fileChooserParams)
    }

    var onReceivedTitle: ((title: String?) -> Unit)? = null
    override fun onReceivedTitle(view: WebView?, title: String?) {
        super.onReceivedTitle(view, title)
        onReceivedTitle?.invoke(title)
    }

    var onProgressChanged: ((newProgress: Int) -> Unit)? = null
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        onProgressChanged?.invoke(newProgress)
    }

    var onJsConfirm: ((url: String?, message: String?, result: JsResult?) -> Boolean)? = null
    override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return onJsConfirm?.invoke(url, message, result) ?: super.onJsConfirm(view, url, message, result)
    }

    var onJsBeforeUnload: ((url: String?, message: String?, result: JsResult?) -> Boolean)? = null
    override fun onJsBeforeUnload(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
        return onJsBeforeUnload?.invoke(url, message, result) ?: super.onJsBeforeUnload(view, url, message, result)
    }

    var onCreateWindow: ((isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?) -> Boolean)? = null
    override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
        return onCreateWindow?.invoke(isDialog, isUserGesture, resultMsg) ?: super.onCreateWindow(view, isDialog, isUserGesture, resultMsg)
    }

    var onCloseWindow: (() -> Unit)? = null
    override fun onCloseWindow(window: WebView?) {
        super.onCloseWindow(window)
        onCloseWindow?.invoke()
    }
}

// TODO(lmr): probably need an adapter to allow people to add headers in a separate params
fun WebView.setUrl(url: String) = loadUrl(url)
fun WebView.setHtml(html: String) = loadData(html, "text/html", null)






private val composeWebChromeClientKey = tagKey("ComposeWebChromeClient")
private val WebView.composeWebChromeClient: ComposeWebChromeClient get() {
    var client = getTag(composeWebChromeClientKey) as? ComposeWebChromeClient
    if (client == null) {
        client = ComposeWebChromeClient()
        webChromeClient = client
        setTag(composeWebChromeClientKey, client)
    }
    return client
}
fun WebView.setOnJsAlert(handler: (url: String?, message: String?, result: JsResult?) -> Boolean) { composeWebChromeClient.onJsAlert = handler }
fun WebView.setOnJsPrompt(handler: (url: String?, message: String?, defaultValue: String?, result: JsPromptResult?) -> Boolean) { composeWebChromeClient.onJsPrompt = handler }
fun WebView.setOnGeolocationPermissionsShowPrompt(handler: (origin: String?, callback: GeolocationPermissions.Callback?) -> Unit) { composeWebChromeClient.onGeolocationPermissionsShowPrompt = handler }
fun WebView.setOnPermissionRequest(handler: (request: PermissionRequest?) -> Unit) { composeWebChromeClient.onPermissionRequest = handler }
fun WebView.setOnConsoleMessage(handler: (consoleMessage: ConsoleMessage?) -> Boolean) { composeWebChromeClient.onConsoleMessage = handler }
fun WebView.setOnPermissionRequestCanceled(handler: (request: PermissionRequest?) -> Unit) { composeWebChromeClient.onPermissionRequestCanceled = handler }
fun WebView.setOnShowFileChooser(handler: (filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?) -> Boolean) { composeWebChromeClient.onShowFileChooser = handler }
fun WebView.setOnReceivedTitle(handler: (title: String?) -> Unit) { composeWebChromeClient.onReceivedTitle = handler }
fun WebView.setOnProgressChanged(handler: (newProgress: Int) -> Unit) { composeWebChromeClient.onProgressChanged = handler }
fun WebView.setOnJsConfirm(handler: (url: String?, message: String?, result: JsResult?) -> Boolean) { composeWebChromeClient.onJsConfirm = handler }
fun WebView.setOnJsBeforeUnload(handler: (url: String?, message: String?, result: JsResult?) -> Boolean) { composeWebChromeClient.onJsBeforeUnload = handler }
fun WebView.setOnCreateWindow(handler: (isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?) -> Boolean) { composeWebChromeClient.onCreateWindow = handler }
fun WebView.setOnCloseWindow(handler: () -> Unit) { composeWebChromeClient.onCloseWindow = handler }


private val composeWebViewClientKey = tagKey("ComposeWebViewClient")
private val WebView.composeWebViewClient: ComposeWebViewClient get() {
    var client = getTag(composeWebViewClientKey) as? ComposeWebViewClient
    if (client == null) {
        client = ComposeWebViewClient()
        webViewClient = client
        setTag(composeWebViewClientKey, client)
    }
    return client
}
fun WebView.setOnPageFinished(handler: (url: String?) -> Unit) { composeWebViewClient.onPageFinished = handler }
fun WebView.setOnReceivedError(handler: (request: WebResourceRequest?, error: WebResourceError?) -> Unit) { composeWebViewClient.onReceivedError = handler }
fun WebView.setOnReceivedLoginRequest(handler: (realm: String?, account: String?, args: String?) -> Unit) { composeWebViewClient.onReceivedLoginRequest = handler }
fun WebView.setOnReceivedHttpError(handler: (request: WebResourceRequest?, errorResponse: WebResourceResponse?) -> Unit) { composeWebViewClient.onReceivedHttpError = handler }
fun WebView.setOnPageStarted(handler: (url: String?, favicon: Bitmap?) -> Unit) { composeWebViewClient.onPageStarted = handler }
fun WebView.setOnUnhandledKeyEvent(handler: (event: KeyEvent?) -> Unit) { composeWebViewClient.onUnhandledKeyEvent = handler }
fun WebView.setOnReceivedHttpAuthRequest(handler: (handler: HttpAuthHandler?, host: String?, realm: String?) -> Unit) { composeWebViewClient.onReceivedHttpAuthRequest = handler }
fun WebView.setOnFormResubmission(handler: (dontResend: Message?, resend: Message?) -> Unit) { composeWebViewClient.onFormResubmission = handler }
fun WebView.setOnLoadResource(handler: (url: String?) -> Unit) { composeWebViewClient.onLoadResource = handler }