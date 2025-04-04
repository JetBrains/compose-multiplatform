/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package androidx.compose.xctest

import platform.Foundation.*
import platform.XCTest.XCTest
import platform.XCTest.XCTestObservationCenter
import platform.XCTest.XCTestSuite
import kotlin.native.internal.test.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction1


// Top level suite name used to hold all Native tests
internal const val TOP_LEVEL_SUITE = "Kotlin/Native test suite"

// Name of the key that contains arguments used to set [TestSettings]
private const val TEST_ARGUMENTS_KEY = "KotlinNativeTestArgs"

/**
 * Stores current settings with the filtered test suites, loggers, and listeners.
 * Test settings should be initialized by the setup method.
 */
private lateinit var testSettings: TestSettings

@Suppress("unused")
fun setupXCTestSuite(vararg tests: KClass<*>): XCTestSuite =
    setupXCTestSuite(tests = tests.toSet(), testCases = null)

@Suppress("unused")
inline fun <reified Class>setupXCTestSuite(vararg tests: KFunction1<Class, *>): XCTestSuite =
    setupXCTestSuite(tests = null, testCases = mapOf(Class::class.qualifiedName to tests.toSet()))

/**
 * This is an entry-point of XCTestSuites and XCTestCases generation.
 * Function returns the XCTest's top level TestSuite that holds all the test cases
 * with K/N tests.
 * This test suite can be run by either native launcher compiled to bundle or
 * by the other test suite (e.g. compiled as a framework).
 */
fun setupXCTestSuite(tests: Set<KClass<*>>? = null, testCases: Map<String?, Set<KFunction<*>>>? = null): XCTestSuite {
    val nativeTestSuite = XCTestSuite.testSuiteWithName(TOP_LEVEL_SUITE)

    // Initialize settings with the given args
    val args = testArguments(TEST_ARGUMENTS_KEY)

    testSettings = TestProcessor(GeneratedSuites.suites, args).process()

    check(::testSettings.isInitialized) {
        "Test settings wasn't set. Check provided arguments and test suites"
    }

    // Set test observer that will log test execution
    XCTestObservationCenter.sharedTestObservationCenter.addTestObserver(
        NativeTestObserver(
            testSettings
        )
    )

    if (testSettings.runTests) {
        val includeAllTests = tests == null && testCases == null
        val testSuiteNames = tests?.map { it.qualifiedName }.orEmpty() + testCases?.keys.orEmpty()
        fun includeTestSuite(testSuite: TestSuite) =
            includeAllTests || testSuiteNames.contains(testSuite.name)

        // Generate and add tests to the main suite
        testSettings.testSuites.generate(
            addTestSuite = { testSuite ->
                includeTestSuite(testSuite)
            },
            addTestCase = { testCase ->
                includeTestSuite(testCase.suite) &&
                        (testCases == null ||
                                testCases[testCase.suite.name]?.firstOrNull { it.name == testCase.name } != null)
            }
        ).forEach {
            nativeTestSuite.addTest(it)
        }

        if (includeAllTests) {
            // Tests created (self-check)
            @Suppress("UNCHECKED_CAST")
            check(testSettings.testSuites.size == (nativeTestSuite.tests as List<XCTest>).size) {
                "The amount of generated XCTest suites should be equal to Kotlin test suites"
            }
        }
    }

    return nativeTestSuite
}

/**
 * Gets test arguments from the Info.plist using the provided key to create test settings.
 *
 * @param key a key used in the `Info.plist` file or as environment variable to pass test arguments
 */
@Suppress("UNCHECKED_CAST")
private fun testArguments(key: String): Array<String> {
    (NSProcessInfo.processInfo.arguments as? List<String>)?.let {
        // Drop the first element containing executable name.
        // See https://developer.apple.com/documentation/foundation/nsprocessinfo/1415596-arguments
        // Then filter only relevant to the runner arguments.
        val args = it.drop(1)
            .filter { argument ->
                argument.startsWith("--gtest_") || argument.startsWith("--ktest_") ||
                        argument == "--help" || argument == "-h"
            }.toTypedArray()
        if (args.isNotEmpty()) return args
    }

    (NSProcessInfo.processInfo.environment[key] as? String)?.let {
        return it.split(" ").toTypedArray()
    }

    // As we don't know which bundle we are, iterate through all of them
    NSBundle.allBundles
        .mapNotNull { (it as? NSBundle)?.infoDictionary?.get(key) as? String }
        .singleOrNull()
        ?.let {
            return it.split(" ").toTypedArray()
        }

    return emptyArray()
}

internal val TestCase.fullName get() = "${suite.name}.$name"

private fun Collection<TestSuite>.generate(
    addTestSuite: (TestSuite) -> Boolean,
    addTestCase: (TestCase) -> Boolean
): List<XCTestSuite> {
    return this.filter(addTestSuite).map { suite ->
        val xcSuite = XCTestSuiteWrapper(suite)
        suite.testCases.values.filter(addTestCase).map { testCase ->
            XCTestCaseWrapper(testCase)
        }.forEach {
            // add test to its test suite wrapper
            xcSuite.addTest(it)
        }
        xcSuite
    }
}
