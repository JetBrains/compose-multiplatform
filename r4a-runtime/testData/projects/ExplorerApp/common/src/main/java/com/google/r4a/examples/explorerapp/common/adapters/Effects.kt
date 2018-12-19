package com.google.r4a.examples.explorerapp.common.adapters
/*
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import com.google.r4a.*
import android.widget.*
import com.google.r4a.adapters.setOnTextChange
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.produce
import java.lang.Exception

fun SOURCE_HASH(): Int {
    return 1234
}

annotation class Model

@DslMarker
annotation class EffectDsl


// global "effect" function. Don't think this is a good idea but for syntactic niceness, doing it right now...
inline fun <T> effect(key: Int = SOURCE_HASH(), crossinline block: Effect.() -> T): T = composer.effect(key, block)

inline fun <T> ViewComposition.effect(key: Any = SOURCE_HASH(), block: Effect.() -> T): T {
    with(composer) {
        startGroup(key)
        val effect = remember { Effect(this, GlobalScope) }
        val result = effect.block()
        endGroup()
        return result
    }
}


inline fun <T, P1> ViewComposition.effect(p1: P1, key: Int = SOURCE_HASH(), crossinline block: Effect.() -> T): T
        = effect(composer.joinKey(key, p1), block)
inline fun <T, P1, P2> ViewComposition.effect(p1: P1, p2: P2, key: Int = SOURCE_HASH(), crossinline block: Effect.() -> T): T
        = effect(composer.joinKey(key, composer.joinKey(p1, p2)), block)
// TODO(lmr): more arities
class Effect(
    @PublishedApi
    internal val c: ViewComposer,
    internal val coroutineScope: CoroutineScope
) {
    // This prevents people from putting KTX tags inside of effect blocks
    val composer: Unit = Unit

    @PublishedApi
    internal inline fun <T> effectGroup(key: Any, block: Effect.() -> T): T {
        with (c) {
            startGroup(key)
            val result = block()
            endGroup()
            return result
        }
    }

    inline fun <T> effect(key: Int = SOURCE_HASH(), block: Effect.() -> T): T = effectGroup(key, block)
    inline fun <T, reified P1> effect(p1: P1, key: Int = SOURCE_HASH(), block: Effect.() -> T): T = effectGroup(c.joinKey(key, p1), block)
    inline fun <T, reified P1, reified P2> effect(p1: P1, p2: P2, key: Int = SOURCE_HASH(), block: Effect.() -> T): T = effectGroup(c.joinKey(key, c.joinKey(p1, p2)), block)
    // TODO(lmr): more arities

    inline fun <T> memo(crossinline block: () -> T): T = c.remember(block)
    inline fun <T, reified P1> memo(p1: P1, crossinline block: () -> T) = c.remember(p1, block)
    inline fun <T, reified P1, reified P2> memo(p1: P1, p2: P2, crossinline block: () -> T): T = c.remember(p1, p2, block)
    // TODO(lmr): more arities

    inline fun <T> state(crossinline initial: () -> T): State<T> = memo { State(initial()) }
    inline fun <T, reified P1> state(p1: P1, crossinline initial: () -> T): State<T> = memo(p1) { State(initial()) }
    inline fun <T, reified P1, reified P2> state(p1: P1, p2: P2, crossinline initial: () -> T): State<T> = memo(p1, p2) { State(initial()) }
    // TODO(lmr): more arities

    fun <T> ambient(key: Ambient<T>): T {
        var result: Any? = null
        c.enumParents { parent ->
            if (parent is Ambient<*>.Provider && parent.ambient == key) {
                result = parent.value
                false
            } else true
        }

//        if (result == null) {
//            val ref = ambientReference
//            if (ref != null) {
//                return ref.getAmbient(key)
//            }
//            return key.defaultValue
//        }

        @Suppress("UNCHECKED_CAST")
        return result as T

        // this will be possible inside the runtime
//        return result as? T ?: key.defaultValue
    }

    fun didCommit(block: CommitScope.() -> Unit) {
        c.cache(false) { CommitScope(block) }
    }
    inline fun <reified P1> didCommit(p1: P1, noinline block: CommitScope.() -> Unit) {
        c.remember(p1) { CommitScope(block) }
    }
    inline fun <reified P1, reified P2> didCommit(p1: P1, p2: P2, noinline block: CommitScope.() -> Unit) {
        c.remember(p1, p2) { CommitScope(block) }
    }
    // TODO(lmr): more arities

    // "onActive" is essentially just an alias for didCommit, where it memoizes on a constant value, so it only gets
    // called once.
    fun onActive(block: CommitScope.() -> Unit) = didCommit(true, block)
    fun onDispose(block: () -> Unit) { didCommit { onDispose(block) } }
//    @JvmName("didCommitWithCleanup")
//    fun didCommit(block: () -> () -> Unit) {
//
//    }
}

val Effect.recompose: () -> Unit
    get() = TODO("travel up composer scope to find nearest recompose scope")

fun <T> Effect.observe(data: LiveData<T>): T? {
    val current = effect { state { data.value } }
    didCommit(data) {
        val observer = Observer<T> { t -> current.value = t }
        data.observe(this@observe, observer)
        onDispose {
            data.removeObserver(observer)
        }
    }
    return current.value
}


//sealed class AsyncState<T> {
//    class Pending<T>(): AsyncState<T>()
//    class Canceled<T>(): AsyncState<T>()
//    class Resolved<T>(val result: T): AsyncState<T>()
//}

enum class AsyncStatus { PENDING, CANCELED, SUCCESS }

@Model
data class AsyncState<T>(
        var result: T? = null,
        var status: AsyncStatus = AsyncStatus.PENDING
)

fun <T> Effect.async(task: suspend () -> T) : AsyncState<T> {
    val current = effect { memo { AsyncState<T>() } }
    effect {
        active {
            try {
                val result = task()
                current.result = result
                current.status = AsyncStatus.SUCCESS
            } catch(e: Exception) {
                current.status = AsyncStatus.CANCELED
            }
        }
    }
    return current
}

@Composable
fun UserProfile(userId: Int) {
    val (user, status) = effect(userId) { async { Api.getUser(userId) } }
    when {
        status == AsyncStatus.PENDING -> <TextView text="Loading..." />
        status == AsyncStatus.CANCELED -> <TextView text="Failed..." />
        user != null -> <TextView text=user.name />
    }
}


fun Effect.active(block: suspend CoroutineScope.() -> Unit): Job {
    val job = memo { coroutineScope.launch(block = block) }
    onDispose {
        job.cancel()
    }
    return job
}

fun <T> Effect.channel(initialValue: T, block: suspend ProducerScope<T>.() -> Unit): T {
    val current = effect { state { initialValue } }

    effect {
        active {
            for (msg in produce(block = block)) {
                current.value = msg
            }
        }
    }

    return current.value
}


@Composable
fun Results(results: LiveData<Item>) {
    val items = effect { observe(results) }
    for (item in items) {
        <TextView text=item.text />
    }
}


enum class VerificationState {
    LOADING,
    CHECKING,
    VERIFIED,
    UNVERIFIED
}

//@Composable
//fun VerificationFlow(userId: Int) {
//    val state = effect(userId) {
//        channel(initialValue = LOADING) {
//            val user = Api.getUser(userId).await()
//            send(CHECKING)
//            val verified = VerificationApi.isVerified(user).await()
//            if (verified)
//                send(VERIFIED)
//            else
//                send(UNVERIFIED)
//        }
//    }
//    when (state) {
//        LOADING -> <TextView text="Loading user..." />
//        CHECKING -> <TextView text="Checking user's verification level..." />
//        UNVERIFIED -> <Button text="Unverified! Try again?" onClick={ tryAgain() } />
//        VERIFIED -> <Button text="Continue..." onClick={ goToNext() } />
//    }
//}

//@Composable
//fun VerificationFlow2(userId: Int) = effect {
//    channel(userId) {
//        <TextView text="Loading user..." />
//        commit()
//        val user = Api.getUser(userId).await()
//        <TextView text="Checking user's verification level..." />
//        commit()
//        val verified = VerificationApi.isVerified(user).await()
//        if (verified)
//            <Button text="Continue..." onClick={ goToNext() } />
//        else
//            <Button text="Unverified! Try again?" onClick={ tryAgain() } />
//        commit()
//    }
//}



@Composable
//fun VerificationFlow2(userId: Int) = effect {
//    channel(userId) {
//        <TextView text="Loading user..." />
//        commit()
//        val user = Api.getUser(userId).await()
//        <TextView text="Checking user's verification level..." />
//        commit()
//        val verified = VerificationApi.isVerified(user).await()
//        if (verified)
//            <Button text="Continue..." onClick={ goToNext() } />
//        else
//            <Button text="Unverified! Try again?" onClick={ tryAgain() } />
//        commit()
//    }
//}

@Composable
fun Counter(userId: Int) {
    val count = effect(userId) {
        channel(initialValue = 0) {
            var count = 0
            while(true) {
                delay(1000L)
                count += 1
                send(count)
            }
        }
    }
    <TextView text="$count" />
}


@Composable
fun LiveWeather(location: String) {
    val weather = effect(location) {
        channel(initialValue = null) {
            val ws = WeatherService.connect()
            try {
                while (true) send(ws.current())
            } finally {
                ws.disconnect()
            }
        }
    }
    if (weather != null) {
        <TextView text="$weather" />
    }
}





@Model
class State<T>(var value: T) {
    operator fun component1(): T = value
    operator fun component2(): (T) -> Unit = { value = it }
}

// TODO(lmr): we would need to do something with lifecycles etc. to make it so that these got invoked
// at the right time.
class CommitScope(var didCommit: CommitScope.() -> Unit) {
    fun onDispose(block: () -> Unit) {

    }
}


@Model
class FormModel(var name: String, var password: String)


@Composable
fun Example() {
    val inc = effect { state { 0 } }

    // react hook-like syntax
    val (count, setCount) = effect { state {  memo { 0 } } }

    val activity = effect { ambient(Ambients.Activity) }

    val model = effect { memo { FormModel("", "") } }

    effect {
        didCommit {
            onDispose {

            }
        }
    }

    effect {
        onActive {
            onDispose {

            }
        }
    }

}
































/*
function Example() {
  // Declare a new state variable, which we'll call "count"
  const [count, setCount] = useState(0);

  return (
    <div>
      <p>You clicked {count} times</p>
      <button onClick={() => setCount(count + 1)}>
        Click me
      </button>
    </div>
  );
}
*/

@Composable
fun Example() {
    val count = effect { state { 0 } }

    <TextView text="You clicked ${count.value} times" />
    <Button
        text="Click me"
        onClick={ count.value += 1 }
    />
}


/*
function Example() {
  const [count, setCount] = useState(0);

  // Similar to componentDidMount and componentDidUpdate:
  useEffect(() => {
    // Update the document title using the browser API
    document.title = `You clicked ${count} times`;
  });

  return (
    <div>
      <p>You clicked {count} times</p>
      <button onClick={() => setCount(count + 1)}>
        Click me
      </button>
    </div>
  );
}
 */

@Composable
fun Example() {
    val count = effect { state { 0 } }

    effect {
        didCommit {
            document.title = "You clicked ${count.value} times"
        }
    }

    <TextView text="You clicked ${count.value} times" />
    <Button
        text="Click me"
        onClick={ count.value += 1 }
    />
}


@Composable
fun Example() {
    val (count, setCount) = effect { state { 0 } }

    <TextView text="You clicked ${count} times" />
    <Button
        text="Click me"
        onClick={ setCount(count + 1) }
    />
}


@Composable
fun NameForm() {
    val fname = effect { state { "" } }
    val lname = effect { state { "" } }

    <EditText text=fname.value onTextChange={ fname.value = it } />
    <EditText text=lname.value onTextChange={ lname.value = it } />
}



@Model
class FormModel(var fname: String = "", var lname: String = "")

@Composable
fun NameForm() {
    val model = effect { memo { FormModel() } }

    <EditText text=model.fname onTextChange={ model.fname = it } />
    <EditText text=model.lname onTextChange={ model.lname = it } />
}




/*
import { useState, useEffect } from 'react';

function FriendStatus(props) {
 const [isOnline, setIsOnline] = useState(null);

 function handleStatusChange(status) {
   setIsOnline(status.isOnline);
 }

 useEffect(() => {
   ChatAPI.subscribeToFriendStatus(props.friend.id, handleStatusChange);

   return () => {
     ChatAPI.unsubscribeFromFriendStatus(props.friend.id, handleStatusChange);
   };
 });

 if (isOnline === null) {
   return 'Loading...';
 }
 return isOnline ? 'Online' : 'Offline';
}
 */

enum class Status { LOADING, ONLINE, OFFLINE }

@Composable
fun FriendStatus(friend: Friend) {
    val status = effect { state { Status.LOADING } }
    val handleStatusChange = { it: Status -> status.value = it }

    effect(friend.id) {
        didCommit {
            ChatAPI.subscribeToFriendStatus(friend.id, handleStatusChange)
            return { ChatAPI.unsubscribeFromFriendStatus(friend.id, handleStatusChange) }
        }
    }

    <TextView
        text=when (status) {
            Status.LOADING -> "Loading..."
            Status.ONLINE -> "Online"
            Status.OFFLINE -> "Offline"
        }
    />
}

@Composable
fun FriendStatus(friend: Friend) {
    val status = effect { state(friend.id) { Status.LOADING } }

    effect {
        didCommit(friend.id) {
            val subscription = ChatAPI.subscribeToFriendStatus(friend.id) {
                status.value = it
            }
            onDispose {
                subscription.unsubscribe()
            }
        }
    }

    <TextView
        text=when (status) {
            Status.LOADING -> "Loading..."
            Status.ONLINE -> "Online"
            Status.OFFLINE -> "Offline"
        }
    />
}

/*
import { useState, useEffect } from 'react';

function useFriendStatus(friendID) {
  const [isOnline, setIsOnline] = useState(null);

  function handleStatusChange(status) {
    setIsOnline(status.isOnline);
  }

  useEffect(() => {
    ChatAPI.subscribeToFriendStatus(friendID, handleStatusChange);
    return () => {
      ChatAPI.unsubscribeFromFriendStatus(friendID, handleStatusChange);
    };
  });

  return isOnline;
}
 */

fun Effect.friendStatus(friendId: Int) {
    val status = effect { state { Status.LOADING } }
    val handleStatusChange = { it: Status -> status.value = it }

    effect(friendId) {
        didCommit {
            ChatAPI.subscribeToFriendStatus(friendId, handleStatusChange)
            onDispose {
                ChatAPI.unsubscribeFromFriendStatus(friendId, handleStatusChange)
            }
        }
    }
}

fun Effect.friendStatus(friendId: Int): Status {
    val status = effect { state(friendId) { Status.LOADING } }
    effect {
        didCommit(friendId) {
            val subscription = ChatAPI.subscribeToFriendStatus(friendId) {
                status.value = it
            }
            onDispose {
                subscription.unsubscribe()
            }
        }
    }
    return status.value
}


@Model
class Location(
        val lat: Double,
        val lng: Double,
        val accuracy:
)

val Effect.currentLocation: Location get() {
    val location = effect { state { Location.LOADING } }
    effect {
        onActive {
            val subscription = Geolocation.subscribe {
                location.value = it
            }
            onDispose {
                subscription.unsubscribe()
            }
        }
    }
    return location.value
}

val Effect.foo get() = 123


@Composable
fun FriendStatus(friend: Friend) {
    val status = effect { friendStatus(friend.id) }
    val location = effect { currentLocation }

    <Text
        text=when (status) {
            Status.LOADING -> "Loading..."
            Status.ONLINE -> "Online"
            Status.OFFLINE -> "Offline"
        }
    />
}

val LocaleAmbient = Ambient.of<Locale>()
val ContextAmbient = Ambient.of<Context>()

@Composable
fun Example() {
    val locale = effect { ambient(LocaleAmbient) }

    // ...
}

@Composable
fun App() {
    <LocaleAmbient.Provider value=Locale.ENGLISH>
        // ...
    </LocaleAmbient.Provider>
}


@Composable
fun Example() {
    <LocaleAmbient.Consumer> locale ->
        // ...
    </LocaleAmbient.Consumer>
}

// TODO(lmr): should the `(a, b)` go on `memo` or `effect`?

val memoizedValue = effect { memo(a, b) { computeExpensiveValue(a, b) } }

fun <T> Effect.state(initial: () -> T) = memo { State(initial()) }


@Composable
fun Example(friends: List<Friend>) {
    for (friend in friends) {
        val status = effect(friend.id) { friendStatus(friend.id) }
        <TextView text="${friend.name} (${status.value})" />
    }
}

@Composable
fun Example(friends: List<Friend>) {
    for (friend in friends) {
        <Key value=friend.id>
            val status = effect { friendStatus(friend.id) }
            <TextView text="${friend.name} (${status.value})" />
        </Key>
    }
}


/*
  Notes:
  - state + @Model classes
  - passing arguments into `effect(...)` to determine the memoization???
  - does `effect` need to be a method on the `composer`? Should we just live with `composer.effect`?

  - React's "useImperativeMethods" effect is a way for you to expose your own "ref"
  - React's "useMutationEffect" effect is a way to mutate DOM nodes synchronously with the flush
  - React's "useLayoutEffect" effect is similar to the useMutationEffect, but it runs after all of them are run.

  - AnimationFrame
  - LiveData? Observables?
  - @Model
  - suspend functions?
  - layout? measure?
  - data loading?


  debounce?
  previous?
  tween? spring? animations?
 */