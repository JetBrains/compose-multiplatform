package example.todo.common.utils

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.lifecycle.Lifecycle
import com.arkivanov.decompose.lifecycle.subscribe
import com.arkivanov.mvikotlin.core.binder.Binder
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.arkivanov.mvikotlin.extensions.reaktive.BindingsBuilder
import com.arkivanov.mvikotlin.extensions.reaktive.bind

fun bind(lifecycle: Lifecycle, mode: BinderLifecycleMode, builder: BindingsBuilder.() -> Unit): Binder {
    val binder = bind(builder)

    when (mode) {
        BinderLifecycleMode.CREATE_DESTROY -> lifecycle.subscribe(onCreate = { binder.start() }, onDestroy = { binder.stop() })
        BinderLifecycleMode.START_STOP -> lifecycle.subscribe(onStart = { binder.start() }, onStop = { binder.stop() })
        BinderLifecycleMode.RESUME_PAUSE -> lifecycle.subscribe(onResume = { binder.start() }, onPause = { binder.stop() })
    }.let {}

    return binder
}

fun ComponentContext.bind(mode: BinderLifecycleMode, builder: BindingsBuilder.() -> Unit): Binder =
    bind(lifecycle, mode, builder)
