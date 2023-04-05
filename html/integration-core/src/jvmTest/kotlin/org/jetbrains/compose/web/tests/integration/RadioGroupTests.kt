package org.jetbrains.compose.web.tests.integration

import org.jetbrains.compose.web.tests.integration.common.BaseIntegrationTests
import org.jetbrains.compose.web.tests.integration.common.ResolveDrivers
import org.jetbrains.compose.web.tests.integration.common.openTestPage
import org.jetbrains.compose.web.tests.integration.common.waitTextToBe
import org.openqa.selenium.By
import org.openqa.selenium.remote.RemoteWebDriver

class RadioGroupTests : BaseIntegrationTests() {

    @ResolveDrivers
    fun radioGroupItemsCanBeChecked(driver: RemoteWebDriver) {
        driver.openTestPage("radioGroupItemsCanBeChecked")
        driver.waitTextToBe(value = "None")

        val r1 = driver.findElement(By.id("id1"))
        val r2 = driver.findElement(By.id("id2"))
        val r3 = driver.findElement(By.id("id3"))

        check(!r1.isSelected)
        check(!r2.isSelected)
        check(!r3.isSelected)

        r1.click()

        driver.waitTextToBe(value = "r1")
        check(r1.isSelected)
        check(!r2.isSelected)
        check(!r3.isSelected)

        r2.click()

        driver.waitTextToBe(value = "r2")
        check(!r1.isSelected)
        check(r2.isSelected)
        check(!r3.isSelected)

        r3.click()

        driver.waitTextToBe(value = "r3")
        check(!r1.isSelected)
        check(!r2.isSelected)
        check(r3.isSelected)
    }

    @ResolveDrivers
    fun twoRadioGroupsChangedIndependently(driver: RemoteWebDriver) {
        driver.openTestPage("twoRadioGroupsChangedIndependently")
        driver.waitTextToBe(textId = "txt", "None")
        driver.waitTextToBe(textId = "txt2", "None")

        val rg1Items = listOf(
            driver.findElement(By.id("id1")),
            driver.findElement(By.id("id2")),
            driver.findElement(By.id("id3"))
        )

        val rg2Items = listOf(
            driver.findElement(By.id("ida")),
            driver.findElement(By.id("idb")),
            driver.findElement(By.id("idc"))
        )

        check(rg1Items.all { !it.isSelected })
        check(rg2Items.all { !it.isSelected })

        rg1Items[1].click()

        driver.waitTextToBe(textId = "txt", "r2")
        driver.waitTextToBe(textId = "txt2", "None")

        check(rg1Items[1].isSelected)
        check(rg2Items.all { !it.isSelected })

        rg2Items[2].click()

        driver.waitTextToBe(textId = "txt", "r2")
        driver.waitTextToBe(textId = "txt2", "rc")

        check(rg2Items[2].isSelected)
        check(rg2Items.filterIndexed { index, _ -> index != 2 }.all { !it.isSelected })

        check(rg1Items[1].isSelected)
        check(rg1Items.filterIndexed { index, _ -> index != 1 }.all { !it.isSelected })
    }
}
