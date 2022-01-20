import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.*
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@OptIn(ExperimentalTime::class)
fun main(): Unit = runBlocking {
    addNComposableItems(10)
    addNComposableItems(100)
    addNComposableItems(1000)
    addNComposableItems(10000)
}
