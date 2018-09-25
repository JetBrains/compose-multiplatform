package org.jetbrains.kotlin.r4a

import org.jetbrains.kotlin.generators.tests.generator.testGroup
import org.jetbrains.kotlin.idea.completion.test.AbstractR4aCompletionTest


fun main(args: Array<String>) {
    System.setProperty("java.awt.headless", "true")

    testGroup("plugins/r4a/r4a-ide/tests", "plugins/r4a/r4a-ide/testData") {
        testClass<AbstractR4aCompletionTest> {
            model(relativeRootPath = "completion", recursive = true)
        }
    }
}