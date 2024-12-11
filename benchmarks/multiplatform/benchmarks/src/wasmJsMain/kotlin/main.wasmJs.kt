import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.url.URLSearchParams

fun main() {
    val urlParams = URLSearchParams(window.location.search.toJsString())
    var i = 0
    val args = generateSequence { urlParams.get("arg${i++}") }.toList().toTypedArray()
    Args.parseArgs(args)
    MainScope().launch {
        runBenchmarks()
        println("Completed!")
    }
}