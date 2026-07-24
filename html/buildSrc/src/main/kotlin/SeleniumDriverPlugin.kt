package jetbrains.compose.web.gradle
import org.gradle.api.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File
import java.net.URL

// https://googlechromelabs.github.io/chrome-for-testing/
private val CHROME_DRIVER_VERSION = "140.0.7339.82"
private val GECKO_DRIVER_VERSION = "0.36.0"

private fun download(url: String, file: File) {
    println("downloading ${url} to ${file}")
    file.writeBytes(URL(url).readBytes())
}

private fun Project.extractZip(archive: File, destDir: String) {
    println("unpacking ${archive.absolutePath} to ${destDir}")
    copy { copySpec ->
        copySpec
            .from(zipTree(archive))
            .into(destDir)
    }
}

private fun Project.extractTar(archive: File, destDir: String) {
    println("unpacking ${archive.absolutePath} to ${destDir}")
    copy { copySpec ->
        copySpec
            .from(tarTree(resources.gzip(archive)))
            .into(destDir)
    }
}

private fun Project.extract(archive: File, destDir: String) {
    when {
        archive.absolutePath.endsWith(".zip") -> extractZip(archive, destDir)
        archive.absolutePath.endsWith(".gz") -> extractTar(archive, destDir)
    }
}

private fun resolvePath(id: String): String {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    val arch = DefaultNativePlatform.getCurrentArchitecture()

    val geckoRepo = "https://github.com/mozilla/geckodriver/releases/download/v$GECKO_DRIVER_VERSION/"
    val chromeRepo = "https://storage.googleapis.com/chrome-for-testing-public/$CHROME_DRIVER_VERSION/"

    return when (id) {
        //https://googlechromelabs.github.io/chrome-for-testing/
        "chrome" -> chromeRepo + when {
            os.isWindows -> "win32/chromedriver-win32.zip"
            os.isMacOsX -> if (arch.isArm) {
                "mac-arm64/chromedriver-mac-arm64.zip"
            } else {
                "mac-x64/chromedriver-mac-x64.zip"
            }
            else -> "linux64/chromedriver-linux64.zip"
        }
        "gecko" -> geckoRepo + when {
            os.isWindows -> "geckodriver-v$GECKO_DRIVER_VERSION-win64.zip"
            os.isMacOsX -> if (arch.isArm) {
                "geckodriver-v$GECKO_DRIVER_VERSION-macos-aarch64.tar.gz"
            } else {
                "geckodriver-v$GECKO_DRIVER_VERSION-macos.tar.gz"
            }
            else -> "geckodriver-v$GECKO_DRIVER_VERSION-linux64.tar.gz"
        }
        else -> throw Exception("unknown id: ${id}")
    }
}

private fun Project.pathToDriverDir(id: String) = gradle.gradleUserHomeDir.resolve("selenium/$id").absolutePath

private fun Project.pathToDriver(id: String): String {
    val os = DefaultNativePlatform.getCurrentOperatingSystem()
    val extension = if (os.isWindows) ".exe" else ""
    return File(pathToDriverDir(id)).resolve("${id}driver$extension").absolutePath
}

private fun Project.install(id: String) {
    val driverPath = resolvePath(id)
    File.createTempFile("selenium_", "." + driverPath.substringAfterLast(".")).let {
        download(
            driverPath,
            it
        )
        project.extract(it, project.pathToDriverDir(id))
    }
}

class SeleniumDriverPlugin: Plugin<Project> {

    override fun apply(project: Project) {
        if (System.getProperty("webdriver.chrome.driver") == null) {
            project.extensions.add("webdriver.chrome.driver", project.pathToDriver("chrome"))
        }
        if (System.getProperty("webdriver.gecko.driver") == null) {
            project.extensions.add("webdriver.gecko.driver", project.pathToDriver("gecko"))
        }

        project.tasks.register("installGeckoDriver") {
            project.install("gecko")
        }

        project.tasks.register("installChromeDriver") {
            project.install("chrome")
        }

        project.tasks.register("installWebDrivers") {
            it.dependsOn("installChromeDriver", "installGeckoDriver")
        }
    }
}
