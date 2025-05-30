enum class Mode {
    SIMPLE,
    VSYNC_EMULATION,
}

object Args {

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

    private fun String.decodeArg() = replace("%20", " ")

    /**
     * Parses command line arguments to create a [Config] for benchmarks run.
     *
     * @param args an array of strings representing the command line arguments.
     * Each argument can specify either of these settings:
     * modes, benchmarks, disabledBenchmarks - comma separated values,
     * versionInfo, saveStatsToCSV, saveStatsToJSON - single values.
     *
     * Example: benchmarks=AnimatedVisibility(100),modes=SIMPLE,versionInfo=Kotlin_2_1_20,saveStatsToCSV=true
     */
    fun parseArgs(args: Array<String>): Config {
        val modes = mutableSetOf<Mode>()
        val benchmarks = mutableMapOf<String, Int>()
        val disabledBenchmarks = mutableSetOf<String>()
        var versionInfo: String? = null
        var saveStatsToCSV: Boolean = false
        var saveStatsToJSON: Boolean = false
        var runServer: Boolean = false

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
            } else if (arg.startsWith("disabledBenchmarks=", ignoreCase = true)) {
                disabledBenchmarks += argToMap(arg.decodeArg()).keys
            } else if (arg.startsWith("runServer=", ignoreCase = true)) {
                runServer = arg.substringAfter("=").toBoolean()
            }
        }

        return Config(
            modes = modes,
            benchmarks = benchmarks,
            disabledBenchmarks = disabledBenchmarks,
            versionInfo = versionInfo,
            saveStatsToCSV = saveStatsToCSV,
            saveStatsToJSON = saveStatsToJSON,
            runServer = runServer,
        )
    }
}

/**
 * Represents the benchmarks configuration parsed from command line arguments or configured programmatically.
 *
 * @property modes The set of enabled execution modes. If empty, all modes are considered enabled by default checks.
 * @property benchmarks A map of explicitly mentioned benchmarks to their specific problem sizes.
 *                     A value of -1 indicates the benchmark is enabled but should use its default size.
 *                     If the map is empty, all benchmarks are considered enabled by default checks.
 * @property disabledBenchmarks A set of benchmarks to skip.
 * @property versionInfo Optional string containing version information.
 * @property saveStatsToCSV Flag indicating whether statistics should be saved to a CSV file.
 * @property saveStatsToJSON Flag indicating whether statistics should be saved to a JSON file.
 */
data class Config(
    val modes: Set<Mode> = emptySet(),
    val benchmarks: Map<String, Int> = emptyMap(), // Name -> Problem Size (-1 for default)
    val disabledBenchmarks: Set<String> = emptySet(),
    val versionInfo: String? = null,
    val saveStatsToCSV: Boolean = false,
    val saveStatsToJSON: Boolean = false,
    val runServer: Boolean = false,
) {
    /**
     * Checks if a specific mode is enabled based on the configuration.
     * A mode is considered enabled if no modes were specified (default) or if it's explicitly listed.
     */
    fun isModeEnabled(mode: Mode): Boolean = modes.isEmpty() || modes.contains(mode)

    /**
     * Checks if a specific benchmark is enabled
     */
    fun isBenchmarkEnabled(benchmark: String): Boolean {
        val normalizedName = benchmark.uppercase()
        // Enabled if the benchmarks map is empty OR if the specific benchmark is present
        return (benchmarks.isEmpty() || benchmarks.containsKey(normalizedName))
                && !disabledBenchmarks.contains(normalizedName)
                && !disabledBenchmarks.contains(benchmark)
    }

    /**
     * Returns the problem size configured for [benchmark], or [default] if not set.
     *
     * @param benchmark Benchmark name (case-insensitive).
     * @param default Fallback size when no configuration is found.
     * @return The problem size to use.
     */
    fun getBenchmarkProblemSize(benchmark: String, default: Int): Int {
        val normalizedName = benchmark.uppercase()
        val problemSize = benchmarks[normalizedName] ?: -1
        return if (problemSize == -1) default else problemSize
    }

    companion object {
        private var global: Config = Config()

        val versionInfo: String?
            get() = global.versionInfo

        val saveStatsToCSV: Boolean
            get() = global.saveStatsToCSV

        val saveStatsToJSON: Boolean
            get() = global.saveStatsToJSON

        val runServer: Boolean
            get() = global.runServer

        fun setGlobal(global: Config) {
            this.global = global
        }

        fun setGlobalFromArgs(args: Array<String>) {
            this.global = Args.parseArgs(args)
        }

        fun isModeEnabled(mode: Mode): Boolean =
            global.isModeEnabled(mode)

        fun isBenchmarkEnabled(benchmark: String): Boolean =
            global.isBenchmarkEnabled(benchmark)

        fun getBenchmarkProblemSize(benchmark: String, default: Int): Int =
            global.getBenchmarkProblemSize(benchmark, default)

        fun saveStats() = saveStatsToCSV || saveStatsToJSON
   }
}