package example.todo.common.utils

import com.arkivanov.decompose.instancekeeper.InstanceKeeper
import com.arkivanov.decompose.instancekeeper.getOrCreate
import com.arkivanov.mvikotlin.core.store.Store

fun <T : Store<*, *, *>> InstanceKeeper.getStore(key: Any, factory: () -> T): T =
    getOrCreate(key) { StoreHolder(factory()) }
        .store

inline fun <reified T : Store<*, *, *>> InstanceKeeper.getStore(noinline factory: () -> T): T =
    getStore(T::class, factory)

private class StoreHolder<T : Store<*, *, *>>(
    val store: T
) : InstanceKeeper.Instance {
    override fun onDestroy() {
        store.dispose()
    }
}
