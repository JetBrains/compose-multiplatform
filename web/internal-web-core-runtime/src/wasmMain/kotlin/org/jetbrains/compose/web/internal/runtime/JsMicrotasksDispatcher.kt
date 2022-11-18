package org.jetbrains.compose.web.internal.runtime

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlin.coroutines.CoroutineContext
import kotlin.js.Promise

@JsFun("() => 'value'")
private external fun someValue(): Dynamic

@ComposeWebInternalApi
actual class JsMicrotasksDispatcher : CoroutineDispatcher() {
    private val value = someValue()
    override fun dispatch(context: CoroutineContext, block: Runnable) {
        Promise.resolve(value).then { block.run(); null  }
    }
}
