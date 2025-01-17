enum class Mode {
    CPU,
    FRAMES,
    FRAMES_GPU
}

object Args {
    private val modes = mutableSetOf<Mode>()

    private val benchmarks = mutableMapOf<String, Int>()

    private fun argToSet(arg: String): Set<String> = arg.substring(arg.indexOf('=') + 1)
        .split(",").filter{!it.isEmpty()}.map{it.uppercase()}.toSet()


    private fun argToMap(arg: String): Map<String, Int> = arg.substring(arg.indexOf('=') + 1)
        .split(",").filter { !it.isEmpty() }.map { it.uppercase() }.associate {
            if (it.contains('(') && it.contains(')')) {
                it.substringBefore('(') to it.substringAfter('(').substringBefore(')').toInt()
            } else {
                it to -1
            }
        }

    /**
     * Parses command line arguments to determine modes and benchmarks settings.
     *
     * @param args an array of strings representing the command line arguments.
     * Each argument can specify either "modes" or "benchmarks" settings,
     * with values separated by commas.
     */
    fun parseArgs(args: Array<String>) {
        for (arg in args) {
            if (arg.startsWith("modes=", ignoreCase = true)) {
                modes.addAll(argToSet(arg).map { Mode.valueOf(it) })
            } else if (arg.startsWith("benchmarks=", ignoreCase = true)) {
                benchmarks += argToMap(arg)
            }
        }
    }

    fun isModeEnabled(mode: Mode): Boolean = modes.isEmpty() || modes.contains(mode)

    fun isBenchmarkEnabled(benchmark: String): Boolean = benchmarks.isEmpty() || benchmarks.contains(benchmark.uppercase())

    fun getBenchmarkProblemSize(benchmark: String, default: Int): Int {
        val result = benchmarks[benchmark.uppercase()]?: -1
        return if (result == -1) default else result
    }
}
