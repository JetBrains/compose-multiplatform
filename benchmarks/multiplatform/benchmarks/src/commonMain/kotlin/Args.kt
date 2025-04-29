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
     * Parses command line arguments to determine modes and benchmarks settings.
     *
     * @param args an array of strings representing the command line arguments.
     * Each argument can specify either "modes" or "benchmarks" settings,
     * with values separated by commas.
     */
    fun parseArgs(args: Array<String>): Config {
        val modes = mutableSetOf<Mode>()
        val benchmarks = mutableMapOf<String, Int>()
        val unsupportedBenchmarks = mutableSetOf<String>()
        var versionInfo: String? = null
        var saveStatsToCSV: Boolean = false
        var saveStatsToJSON: Boolean = false

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
            } else if (arg.startsWith("unsupportedBenchmarks=", ignoreCase = true)) {
                unsupportedBenchmarks += argToMap(arg.decodeArg()).keys
            }
        }

        return Config(
            modes = modes,
            benchmarks = benchmarks,
            unsupportedBenchmarks = unsupportedBenchmarks,
            versionInfo = versionInfo,
            saveStatsToCSV = saveStatsToCSV,
            saveStatsToJSON = saveStatsToJSON
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
 * @property versionInfo Optional string containing version information.
 * @property saveStatsToCSV Flag indicating whether statistics should be saved to a CSV file.
 * @property saveStatsToJSON Flag indicating whether statistics should be saved to a JSON file.
 */
data class Config(
    val modes: Set<Mode> = emptySet(),
    val benchmarks: Map<String, Int> = emptyMap(), // Name -> Problem Size (-1 for default)
    val unsupportedBenchmarks: Set<String> = emptySet(),
    val versionInfo: String? = null,
    val saveStatsToCSV: Boolean = false,
    val saveStatsToJSON: Boolean = false
) {
    /**
     * Checks if a specific mode is enabled based on the configuration.
     * A mode is considered enabled if no modes were specified (default) or if it's explicitly listed.
     */
    fun isModeEnabled(mode: Mode): Boolean = modes.isEmpty() || modes.contains(mode)

    /**
     * Checks if a specific benchmark is enabled based *only* on the command line arguments (`benchmarks=` list).
     * A benchmark is enabled if no benchmarks were specified (default) or if it's explicitly listed.
     * Note: This does *not* account for runtime disabling (like `disableBenchmark` in the old Args).
     *       That check should be performed separately by the caller if needed.
     */
    fun isBenchmarkEnabled(benchmark: String): Boolean {
        val normalizedName = benchmark.uppercase()
        // Enabled if the benchmarks map is empty OR if the specific benchmark is present
        return (benchmarks.isEmpty() || benchmarks.containsKey(normalizedName))
                && !unsupportedBenchmarks.contains(normalizedName)
                && !unsupportedBenchmarks.contains(benchmark)
    }

    /**
     * Gets the specified problem size for a benchmark, falling back to a default if not specified
     * or if explicitly set to use the default (-1).
     *
     * @param benchmark The name of the benchmark (case-insensitive).
     * @param default The default problem size to return if none is specified in the config.
     * @return The specified problem size, or the default value.
     */
    fun getBenchmarkProblemSize(benchmark: String, default: Int): Int {
        val upperBenchmark = benchmark.uppercase()
        // Get the specified size, default to -1 if not found
        val specifiedSize = benchmarks[upperBenchmark] ?: -1
        // Return default if size is -1, otherwise return the specified size
        return if (specifiedSize == -1) default else specifiedSize
    }
}