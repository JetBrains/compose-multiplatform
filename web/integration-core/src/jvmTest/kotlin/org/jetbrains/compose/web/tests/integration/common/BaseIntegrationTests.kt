package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
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

}

@Target(AnnotationTarget.FUNCTION)
@ParameterizedTest(name = "{displayName} [{0}]")
@MethodSource("resolveDrivers")
annotation class ResolveDrivers

@DisplayNameGeneration(DisplayNameSimplifier::class)
@ExtendWith(value = [StaticServerSetupExtension::class])
abstract class BaseIntegrationTests() {
    companion object {
        @JvmStatic
        fun resolveDrivers(): Array<Array<Any>> {
            return arrayOf(
                arrayOf(Drivers.Chrome)
            )
        }
    }
}