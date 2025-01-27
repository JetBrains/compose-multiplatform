import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

actual fun saveBenchmarksOnDisk(name: String, stats: BenchmarkStats) {
    val file = File("build/benchmarks/$name.csv")
    val keyToValue = mutableMapOf<String, String>()
    keyToValue.put("Date",
        LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")
        )
    )
    stats.putFormattedValuesTo(keyToValue)
    if (!file.exists()) {
        file.parentFile?.mkdirs()
        file.appendText(keyToValue.keys.joinToString(",") + "\n")
    }
    file.appendText(keyToValue.values.joinToString(",") { it.replace(",", ";") } + "\n")
    println("Results saved to ${file.absolutePath}")
    println()
}
