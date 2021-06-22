package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.extension.ExtendWith
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.remote.RemoteWebDriver
import org.openqa.selenium.safari.SafariDriver
import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait

private val PATH = "http://localhost:${ServerLauncher.port}"

fun WebDriver.openTestPage(test: String) {
    get("$PATH?test=$test")
}

fun WebDriver.waitTextToBe(textId: String = "txt", value: String) {
    WebDriverWait(this, 1).until(ExpectedConditions.textToBe(By.id(textId), value))
}

object Drivers {
    val Chrome by lazy {
        ChromeDriver(
            ChromeOptions().apply {
                setHeadless(true)
                addArguments("--no-sandbox")
            }
        )
    }

    val Firefox by lazy {
        FirefoxDriver(
            FirefoxOptions().apply {
                setHeadless(true)
            }
        )
    }

    val Safari by lazy {
        SafariDriver()
    }
}

@ExtendWith(value = [StaticServerSetupExtension::class])
abstract class BaseIntegrationTests(val driver: RemoteWebDriver) {
    fun openTestPage(test: String) = driver.openTestPage(test)
    fun waitTextToBe(textId: String = "txt", value: String) = driver.waitTextToBe(textId, value)
}