package org.jetbrains.compose.resources

import androidx.compose.runtime.*
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val scope = rememberCoroutineScope()
    return remember(key1, environment) {
        val mutableState = mutableStateOf(getDefault())
        scope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try {
                mutableState.value = block(environment)
            } catch (_: Exception) {
                //resource loading failed
            }
        }.also { ResourceStateLoadTracker.register(it) }
        mutableState
    }
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val scope = rememberCoroutineScope()
    return remember(key1, key2, environment) {
        val mutableState = mutableStateOf(getDefault())
        scope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try {
                mutableState.value = block(environment)
            } catch (_: Exception) {
                //resource loading failed
            }
        }.also { ResourceStateLoadTracker.register(it) }
        mutableState
    }
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    key3: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val scope = rememberCoroutineScope()
    return remember(key1, key2, key3, environment) {
        val mutableState = mutableStateOf(getDefault())
        scope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try {
                mutableState.value = block(environment)
            } catch (_: Exception) {
                //resource loading failed
            }
        }.also { ResourceStateLoadTracker.register(it) }
        mutableState
    }
}

@Composable
internal actual fun <T> rememberResourceState(
    key1: Any,
    key2: Any,
    key3: Any,
    key4: Any,
    getDefault: () -> T,
    block: suspend (ResourceEnvironment) -> T
): State<T> {
    val environment = LocalComposeEnvironment.current.rememberEnvironment()
    val scope = rememberCoroutineScope()
    return remember(key1, key2, key3, key4, environment) {
        val mutableState = mutableStateOf(getDefault())
        scope.launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            try {
                mutableState.value = block(environment)
            } catch (_: Exception) {
                //resource loading failed
            }
        }.also { ResourceStateLoadTracker.register(it) }
        mutableState
    }
}

private object ResourceStateLoadTracker {
    private val jobs = mutableSetOf<Job>()

    fun register(job: Job) {
        if (!job.isCompleted) {
            jobs += job
            job.invokeOnCompletion { jobs -= job }
        }
    }

    suspend fun awaitAll() {
        while (true) {
            val pendingJobs = jobs.toList()
            if (pendingJobs.isEmpty()) return
            pendingJobs.joinAll()
        }
    }
}

@ExperimentalResourceApi
actual suspend fun awaitPendingResourceStateLoads() {
    ResourceStateLoadTracker.awaitAll()
}