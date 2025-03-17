enum class Mode {
    SIMPLE,
    VSYNC_EMULATION,
}

object Args {
    private val modes = mutableSetOf<Mode>()

    private val benchmarks = mutableMapOf<String, Int>()

    var versionInfo: String? = null
        private set

    var saveStatsToCSV: Boolean = false
        private set

    var saveStatsToJSON: Boolean = false
        private set

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
        // reset the previous configuration before setting a new one for cases when parseArgs is called more than once:
        reset()
        for (arg in args) {
            if (arg.startsWith("modes=", ignoreCase = true)) {
                modes.addAll(argToSet(arg.decodeArg()).map { Mode.valueOf(it) })
            } else if (arg.startsWith("benchmarks=", ignoreCase = true)) {
                benchmarks += argToMap(arg.decodeArg())
            } else if (arg.startsWith("versionInfo=", ignoreCase = true)) {
                versionInfo = arg.substringAfter("=").decodeArg()
            } else if (arg.startsWith("saveStatsToCSV=", ignoreCase = true)) {
                saveStatsToCSV = arg.substringAfter("=").toBoolean()
            } else if (arg.startsWith("saveStatsToJSON=", ignoreCase = true)) {
                saveStatsToJSON = arg.substringAfter("=").toBoolean()
            }
        }
    }

    private fun String.decodeArg() = replace("%20", " ")

    private fun reset() {
        modes.clear()
        benchmarks.clear()
    }

    fun enableModes(vararg modes: Mode) = this.modes.addAll(modes)

    fun isModeEnabled(mode: Mode): Boolean = modes.isEmpty() || modes.contains(mode)

    fun isBenchmarkEnabled(benchmark: String): Boolean = benchmarks.isEmpty() || benchmarks.contains(benchmark.uppercase())

    fun getBenchmarkProblemSize(benchmark: String, default: Int): Int {
        val result = benchmarks[benchmark.uppercase()]?: -1
        return if (result == -1) default else result
    }
}
