package jetbrains.compose.web.gradle
import org.gradle.api.*
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform
import java.io.File
import java.net.URL


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

    val geckoRepo = "https://github.com/mozilla/geckodriver/releases/download/v0.29.1/"
    val chromeRepo = "https://chromedriver.storage.googleapis.com/91.0.4472.101/"

    return when (id) {
        "chrome" -> chromeRepo + when {
            os.isWindows -> "chromedriver_win32.zip"
            os.isMacOsX -> if (arch.isArm) {
                "chromedriver_mac64_m1.zip"
            } else {
                "chromedriver_mac64.zip"
            }
            else -> "chromedriver_linux64.zip"
        }
        "gecko" -> geckoRepo + when {
            os.isWindows -> "geckodriver-v0.29.1-win64.zip"
            os.isMacOsX -> if (arch.isArm) {
                "geckodriver-v0.29.1-macos-aarch64.tar.gz"
            } else {
                "geckodriver-v0.29.1-macos.tar.gz"
            }
            else -> "geckodriver-v0.29.1-linux64.tar.gz"
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
