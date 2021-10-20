package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.openqa.selenium.By
import org.openqa.selenium.JavascriptExecutor
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

private val PATH = "http://localhost:${ServerLauncher.port}"

fun WebDriver.openTestPage(test: String) {
    get("$PATH?test=$test")
}

fun WebDriver.waitTextToBe(textId: String = "txt", value: String) {
    WebDriverWait(this, 1, 16).until(ExpectedConditions.textToBe(By.id(textId), value))
}

internal object Drivers {
    val Chrome by lazy {
        object : ChromeDriver(
            ChromeOptions().apply {
                setHeadless(true)
                addArguments("--no-sandbox")
            }
        ) {
            override fun toString(): String = "chrome"
        }
    }

    val Firefox by lazy {
        object : FirefoxDriver(
            FirefoxOptions().apply {
                setHeadless(true)
            }
        ) {
            override fun toString(): String = "firefox"
        }
    }

    val Safari by lazy {
       object : SafariDriver() {
           override fun toString(): String = "safari"
       }
    }

    @OptIn(ExperimentalStdlibApi::class)
    val activatedDrivers: Array<Array<WebDriver>> = buildList<Array<WebDriver>> {
        add(arrayOf(Chrome))
        if (System.getProperty("compose.web.tests.integration.withFirefox") == "true") {
            add(arrayOf(Firefox))
        }
    }.toTypedArray()

    fun dispose() {
        activatedDrivers.forEach {
            val webDriver = it.first()
            println("Closing web driver: ${webDriver}")
            webDriver.quit()
        }
    }

}

@Target(AnnotationTarget.FUNCTION)
@ParameterizedTest(name = "{displayName} [{0}]")
@MethodSource("resolveDrivers")
annotation class ResolveDrivers


@DisplayNameGeneration(DisplayNameSimplifier::class)
@ExtendWith(value = [IntegrationTestsSetup::class])
abstract class BaseIntegrationTests() {
    companion object {
        @JvmStatic
        fun resolveDrivers() = Drivers.activatedDrivers
    }

    fun WebDriver.outerHtmlOfElementWithId(id: String): String {
        val script = """
             var callback = arguments[arguments.length - 1];
             callback(document.getElementById("$id").outerHTML);
        """.trimIndent()

        return (this as JavascriptExecutor).executeAsyncScript(script).toString()
    }

    fun WebDriver.cursorPosition(id: String): Int {
        val script = """
             var callback = arguments[arguments.length - 1];
             callback(document.getElementById("$id").selectionStart);
        """.trimIndent()

        return (this as JavascriptExecutor).executeAsyncScript(script).toString().toInt()
    }

    fun WebDriver.sendKeysForDateInput(input: WebElement, year: Int, month: Int, day: Int) {
        val keys = when (this) {
            is ChromeDriver -> "${day}${month}${year}"
            is FirefoxDriver -> "${year}-${month}-${day}"
            else -> ""
        }

        input.sendKeys(keys)
    }
}
