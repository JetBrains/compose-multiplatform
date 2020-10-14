package example.todo.common.database

import com.badoo.reaktive.base.setCancellable
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.map
import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.observeOn
import com.badoo.reaktive.scheduler.ioScheduler
import com.squareup.sqldelight.Query

fun <T : Any, R> Query<T>.asObservable(execute: (Query<T>) -> R): Observable<R> =
    asObservable()
        .observeOn(ioScheduler)
        .map(execute)

fun <T : Any> Query<T>.asObservable(): Observable<Query<T>> =
    observable { emitter ->
        val listener =
            object : Query.Listener {
                override fun queryResultsChanged() {
                    emitter.onNext(this@asObservable)
                }
            }

        emitter.onNext(this@asObservable)
        addListener(listener)
        emitter.setCancellable { removeListener(listener) }
    }
