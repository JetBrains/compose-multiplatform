import androidx.compose.runtime.Composable
import benchmarks.animation.AnimatedVisibility
import benchmarks.lazygrid.LazyGrid
import benchmarks.visualeffects.NYContent

fun runBenchmark(name: String, frameCount: Int, content: @Composable () -> Unit) {
    println("$name: " + measureComposable(frameCount, content))
}

fun runBenchmarks() {
    runBenchmark("LazyGrid",40) { LazyGrid() }
}