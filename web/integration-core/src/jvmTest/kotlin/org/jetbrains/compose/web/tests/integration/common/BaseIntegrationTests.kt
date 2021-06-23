package org.jetbrains.compose.web.tests.integration.common

import org.junit.jupiter.api.DisplayNameGeneration
import org.junit.jupiter.api.DisplayNameGenerator
import org.junit.jupiter.api.Test
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
import java.lang.annotation.ElementType
import java.lang.annotation.RetentionPolicy
import java.lang.reflect.Method


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

    internal fun resolve(id: String): WebDriver {
        return when(id) {
            "chrome" -> Chrome
            "firefox" -> Firefox
            "safari" -> Safari
            else -> throw Exception("unknown id: $id")
        }
    }
}

internal fun String.openTestPage(test: String) = Drivers.resolve(this).openTestPage(test)
internal fun String.waitTextToBe(textId: String = "txt", value: String) = Drivers.resolve(this).waitTextToBe(textId, value)
internal fun String.findElement(by: By) = Drivers.resolve(this).findElement(by)

@Target(AnnotationTarget.FUNCTION)
@ParameterizedTest(name = "{displayName} [{0}]")
@MethodSource("resolveDrivers")
annotation class ResolveDrivers

@DisplayNameGeneration(DisplayNameSimplifier::class)
@ExtendWith(value = [StaticServerSetupExtension::class])
abstract class BaseIntegrationTests(val driver: RemoteWebDriver) {
    fun openTestPage(test: String) = driver.openTestPage(test)
    fun waitTextToBe(textId: String = "txt", value: String) = driver.waitTextToBe(textId, value)

    companion object {
        @JvmStatic
        fun resolveDrivers(): Array<Array<Any>> {
            return arrayOf(arrayOf("chrome"))
        }
    }
}