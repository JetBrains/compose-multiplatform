import java.io.File

fun main() {
    checkContent("common-resource.txt", "common resource")
    checkContent("os-specific-resource.txt", "$currentOS only resource")
    checkContent("target-specific-resource.txt", "$currentTarget only resource")
}

fun checkContent(actualFileName: String, expectedContent: String) {
    val file = composeAppResource(actualFileName)
    val actualContent = file.readText().trim()
    check(actualContent == expectedContent) {
        """
            Actual: '$actualContent'
            Expected: '$expectedContent'
        """.trimIndent()
    }
}

fun composeAppResource(path: String): File =
    composeAppResourceDir.resolve(path)

private val composeAppResourceDir: File by lazy {
    val property = "compose.application.resources.dir"
    val path = System.getProperty(property) ?: error("System property '$property' is not set!")
    File(path)
}

internal val currentTarget by lazy {
    "$currentOS-$currentArch"
}

internal val currentOS: String by lazy {
    val os = System.getProperty("os.name")
    when {
        os.equals("Mac OS X", ignoreCase = true) -> "macos"
        os.startsWith("Win", ignoreCase = true) -> "windows"
        os.startsWith("Linux", ignoreCase = true) -> "linux"
        else -> error("Unknown OS name: $os")
    }
}

internal val currentArch by lazy {
    val osArch = System.getProperty("os.arch")
    when (osArch) {
        "x86_64", "amd64" -> "x64"
        "aarch64" -> "arm64"
        else -> error("Unsupported OS arch: $osArch")
    }
}