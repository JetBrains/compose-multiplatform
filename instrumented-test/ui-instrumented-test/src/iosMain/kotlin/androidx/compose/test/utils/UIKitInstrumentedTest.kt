/*
 * Copyright 2025 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package androidx.compose.test.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.UIKit.*
import platform.darwin.NSObject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/**
 * Sets up the test environment for iOS instrumented tests, runs the given [test][testBlock]
 * and then tears down the test environment. Use the methods on [UIKitInstrumentedTest]
 * in the test to find compose content and make assertions on it.
 * @param [testBlock] The test function.
 */
internal fun runUIKitInstrumentedTest(
    testBlock: UIKitInstrumentedTest.() -> Unit
) = with(UIKitInstrumentedTest()) {
    try {
        testBlock()
    } finally {
        tearDown()
    }
}

/**
 * A class designed for instrumented testing of UIKit-related functionality. It provides methods for setting
 * content, simulating user interactions, and managing application lifecycle during testing scenarios.
 *
 * This class is primarily intended for internal use within a Compose multiplatform environment that integrates
 * with UIKit APIs on iOS.
 *
 * Constructor properties are initialized with the attributes of the main screen and a mock delegate to simulate
 * the application setup.
 */
@OptIn(ExperimentalForeignApi::class)
internal class UIKitInstrumentedTest {
    private val screen = UIScreen.mainScreen()
    val density = Density(density = screen.scale.toFloat())
    val appDelegate = MockAppDelegate()
    val screenSize: DpSize = screen.bounds().useContents { DpSize(size.width.dp, size.height.dp) }
    lateinit var hostingViewController: UIViewController
        private set

    @OptIn(ExperimentalComposeApi::class)
    fun setContentWithAccessibilityEnabled(content: @Composable () -> Unit) {
        setContent({ accessibilitySyncOptions = AccessibilitySyncOptions.Always }, content)
    }

    fun setContent(
        configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
        content: @Composable () -> Unit
    ) {
        // TODO: Use ComposeHostingViewController when moving to compose-multiplatform-core repo
        hostingViewController = ComposeUIViewController(
            configure = {
                enforceStrictPlistSanityCheck = false
                configure()
            },
            content = content
        )

        appDelegate.setUpWindow(hostingViewController)
        waitForIdle()
    }

    fun tearDown() {
        appDelegate.cleanUp()
    }

    private val isIdle: Boolean
        get() {
            return false
            // TODO: Uncomment. Use proper isIdle implementation when moving to compose-multiplatform-core repo
            // val hadSnapshotChanges = Snapshot.current.hasPendingChanges()
            // val isApplyObserverNotificationPending = Snapshot.isApplyObserverNotificationPending
            //
            // val containerInvalidations = hostingViewController.performSelector(NSSelectorFromString("hasInvalidations")) as Boolean
            //
            // return !hadSnapshotChanges && !isApplyObserverNotificationPending && !containerInvalidations
        }


    fun waitForIdle(timeoutMillis: Long = 500) {
        // TODO: Properly implement `waitForIdle` when moving to compose-multiplatform-core repo
        try {
            waitUntil(timeoutMillis = timeoutMillis) { isIdle }
        } catch (e: Throwable) {
            // Do nothing
        }
    }

    fun delay(timeoutMillis: Long) {
        val runLoop = NSRunLoop.currentRunLoop()
        runLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(timeoutMillis.toDouble() / 1000.0))
    }

    fun waitUntil(
        conditionDescription: String? = null,
        timeoutMillis: Long = 5_000,
        condition: () -> Boolean
    ) {
        val runLoop = NSRunLoop.currentRunLoop()
        val endTime = TimeSource.Monotonic.markNow() + timeoutMillis.milliseconds
        while (!condition()) {
            if (TimeSource.Monotonic.markNow() > endTime) {
                throw AssertionError(conditionDescription ?: "Timeout ${timeoutMillis}ms reached.")
            }
            runLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.005))
        }
    }

    // Touches:

    /**
     * Simulates a touch-down event at the specified position on the screen.
     *
     * @param position The position on the root hosting controller.
     * @return A UITouch object representing the touch interaction.
     */
    fun touchDown(position: DpOffset): UITouch {
        val positionOnWindow = hostingViewController.view.convertPoint(
            point = position.toCGPoint(),
            toView = appDelegate.window()
        )

        return appDelegate.window()!!.touchDown(positionOnWindow.toDpOffset())
    }

    /**
     * Simulates a tap gesture at the specified position on the screen.
     *
     * @param position The position on the root hosting controller.
     */
    fun tap(position: DpOffset): UITouch {
        return touchDown(position).up()
    }

    /**
     * Simulates a tap gesture for a given AccessibilityTestNode.
     */
    fun AccessibilityTestNode.tap(): UITouch {
        val frame = frame ?: error("Internal error. Frame is missing.")
        return tap(frame.center())
    }

    fun AccessibilityTestNode.doubleTap(): UITouch {
        val frame = frame ?: error("Internal error. Frame is missing.")
        tap(frame.center())
        delay(50)
        return tap(frame.center())
    }

    /**
     * Simulates a drag gesture on the screen, moving the touch from its current location to a specified position
     * over a given duration.
     *
     * @param location The target position of the drag in DpOffset.
     * @param duration The duration of the drag gesture, defaulting to 0.5 seconds.
     * @return The same UITouch instance after completing the drag gesture.
     */
    fun UITouch.dragTo(location: DpOffset, duration: Duration = 0.5.seconds): UITouch {
        val startLocation = locationInView(appDelegate.window()!!).toDpOffset()
        val endLocation = hostingViewController.view.convertPoint(
            point = location.toCGPoint(),
            toView = appDelegate.window()
        ).toDpOffset()

        val startTime = TimeSource.Monotonic.markNow()
        while (TimeSource.Monotonic.markNow() <= startTime + duration) {
            val progress = ((TimeSource.Monotonic.markNow() - startTime) / duration).coerceIn(0.0, 1.0)
            val touchLocation = lerp(startLocation, endLocation, progress.toFloat())

            this.moveToLocationOnWindow(touchLocation)
            NSRunLoop.currentRunLoop().runUntilDate(NSDate.dateWithTimeIntervalSinceNow(1.0 / 60))
        }
        this.moveToLocationOnWindow(endLocation)
        return this
    }

    /**
     * Simulates a drag gesture on the screen, moving the touch from its current location by a specified offset
     * over a given duration.
     *
     * @param offset The offset by which the touch is moved, specified as a DpOffset.
     * @param duration The duration of the drag gesture, defaulting to 0.5 seconds.
     * @return The same UITouch instance after completing the drag gesture.
     */
    fun UITouch.dragBy(offset: DpOffset, duration: Duration = 0.5.seconds): UITouch {
        return dragTo(location + offset, duration)
    }

    /**
     * Simulates a drag gesture on the screen, moving the touch from its current location by specified x and y offsets
     * over a given duration.
     *
     * @param dx The horizontal offset by which the touch is moved, specified as a Dp. Defaults to 0.dp.
     * @param dy The vertical offset by which the touch is moved, specified as a Dp. Defaults to 0.dp.
     * @param duration The duration of the drag gesture, specified as a Duration. Defaults to 0.5 seconds.
     * @return The same UITouch instance after completing the drag gesture.
     */
    fun UITouch.dragBy(dx: Dp = 0.dp, dy: Dp = 0.dp, duration: Duration = 0.5.seconds): UITouch {
        return dragBy(DpOffset(dx, dy), duration)
    }

    val UITouch.location: DpOffset get() {
        return locationInView(hostingViewController.view).toDpOffset()
    }
}

@OptIn(ExperimentalForeignApi::class)
internal class MockAppDelegate: NSObject(), UIApplicationDelegateProtocol {
    private var _window: UIWindow? = null
    override fun window(): UIWindow? = _window

    fun setUpWindow(viewController: UIViewController) {
        UIApplication.sharedApplication().setDelegate(this)

        _window = UIWindow(frame = UIScreen.mainScreen.bounds)
        _window?.backgroundColor = UIColor.systemBackgroundColor

        _window?.rootViewController = viewController
        _window?.makeKeyAndVisible()
    }

    fun cleanUp() {
        _window = null
        val window = UIWindow(frame = UIScreen.mainScreen.bounds)
        window.rootViewController = UIViewController()
        window.makeKeyAndVisible()
    }
}
