
fun main() {
    val isTrainingRun = System.getProperty("compose.aot.training-run") != null
    println("Running app to create archive: $isTrainingRun")
}
