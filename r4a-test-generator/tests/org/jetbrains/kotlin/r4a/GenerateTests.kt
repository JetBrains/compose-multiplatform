package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.idea.completion.test.AbstractR4aCompletionTest
import org.jetbrains.kotlin.idea.quickfix.AbstractR4AQuickFixMultiFileTest


fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("plugins/r4a/r4a-ide/tests", "plugins/r4a/r4a-ide/testData") {
        testClass<AbstractR4aCompletionTest> {
            model(relativeRootPath = "completion", recursive = true)
        }


        testClass<AbstractR4AQuickFixMultiFileTest> {
            model(relativeRootPath = "quickfix", pattern = """^(\w+)\.((before\.Main\.\w+)|(test))$""", testMethod = "doTestWithExtraFile")
        }
    }
}