/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This file is a conversion of ActivityRecreatorChecked.java, using the built-in Android Studio
// Java-to-Kotlin convertor.

package androidx.sample.core.app

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.RestrictTo
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * The goal here is to get common (and correct) behavior around Activity recreation for all API
 * versions up until P, where the behavior was specified to be useful and implemented to match the
 * specification. On API 26 and 27, recreate() doesn't actually recreate the Activity if it's
 * not in the foreground; it will be recreated when the user next interacts with it. This has a few
 * undesirable consequences:
 *
 *
 * 1. It's impossible to recreate multiple activities at once, which means that activities in the
 * background will observe the new configuration before they're recreated. If we keep them on the
 * old configuration, we have two conflicting configurations active in the app, which leads to
 * logging skew.
 *
 *
 * 2. Recreation occurs in the critical path of user interaction - re-inflating a bunch of views
 * isn't free, and we'd rather do it when we're in the background than when the user is staring at
 * the screen waiting to see us.
 *
 *
 * On API < 26, recreate() was implemented with a single call to a private method on
 * ActivityThread. That method still exists in 26 and 27, so we can use reflection to call it and
 * get the exact same behavior as < 26. However, that behavior has problems itself. When
 * an Activity in the background is recreated, it goes through: destroy -> create -> start ->
 * resume -> pause and doesn't stop. This is a violation of the contract for onStart/onStop,
 * but that might be palatable if it didn't also have the effect of preventing new configurations
 * from being applied - since the Activity doesn't go through onStop, our tracking of whether
 * our app is visible thinks we're always visible, and thus can't do another recreation later.
 *
 *
 * The fix for this is to add the missing onStop() call, by using reflection to call into
 * ActivityThread.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object ActivityRecreatorKtChecked {
    private const val LOG_TAG = "ActivityRecreatorChecked"

    // Activity.mMainThread
    private var mainThreadField: Field? = null

    // Activity.mToken. This object is an identifier that is the same between multiple instances of
    // the same underlying Activity.
    private var tokenField: Field? = null

    // On API 25, a third param was added to performStopActivity
    private var performStopActivity3ParamsMethod: Method? = null

    // Before API 25, performStopActivity had two params
    private var performStopActivity2ParamsMethod: Method? = null

    // ActivityThread.requestRelaunchActivity
    private var requestRelaunchActivityMethod: Method? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Equivalent to [Activity.recreate], but working around a number of platform bugs.
     *
     * @return true if a recreate() task was successfully scheduled.
     */
    fun recreate(activity: Activity): Boolean {
        // On Android O and later we can rely on the platform recreate()
        if (Build.VERSION.SDK_INT >= 28) {
            activity.recreate()
            return true
        }

        // API 26 needs this workaround but it's not possible because our reflective lookup failed.
        if (needsRelaunchCall() && requestRelaunchActivityMethod == null) {
            return false
        }
        // All versions of android so far need this workaround, but it's not possible because our
        // reflective lookup failed.
        if (performStopActivity2ParamsMethod == null && performStopActivity3ParamsMethod == null) {
            return false
        }
        return try {
            val token = tokenField!!.get(activity)
                ?: return false
            val activityThread = mainThreadField!!.get(activity)
                ?: return false
            val application = activity.application
            val callbacks = LifecycleCheckCallbacks(activity)
            application.registerActivityLifecycleCallbacks(callbacks)

            /*
                  * Runnables scheduled before/after recreate() will run before and after the Runnables
                  * scheduled by recreate(). This allows us to bound the time where mActivity lifecycle
                  * events that could be caused by recreate() run - that way we can detect onPause()
                  * from the new Activity instance, and schedule onStop to run immediately after it.
                  */mainHandler.post {
                callbacks.currentlyRecreatingToken = token
            }
            try {
                if (needsRelaunchCall()) {
                    requestRelaunchActivityMethod!!.invoke(
                        activityThread,
                        token, null, null, 0, false, null, null, false, false
                    )
                } else {
                    activity.recreate()
                }
                true
            } finally {
                mainHandler.post {
                    // Since we're calling hidden API, it's entirely possible for it to
                    // simply do nothing;
                    // if that's the case, make sure to unregister so we don't leak memory
                    // waiting for an event that will never happen.
                    application.unregisterActivityLifecycleCallbacks(callbacks)
                }
            }
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * Returns true if a stop call was scheduled successfully.
     *
     * Only reachable on SDK < 28.
     */
    internal fun queueOnStopIfNecessary(
        currentlyRecreatingToken: Any?,
        currentlyRecreatingHashCode: Int,
        activity: Activity
    ): Boolean {
        return try {
            val token = tokenField!![activity]
            if (token !== currentlyRecreatingToken ||
                activity.hashCode() != currentlyRecreatingHashCode
            ) {
                // We're looking at a different activity, don't try to make it stop! Note that
                // tokens are reused on SDK 21-23 but Activity objects (and thus hashCode, in
                // all likelihood) are not, so we need to check both.
                return false
            }
            val activityThread = mainThreadField!![activity]
            // These operations are posted at the front of the queue, so that operations
            // scheduled from onCreate, onStart etc run after the onStop call - this should
            // cause any redundant loads to be immediately cancelled.
            mainHandler.postAtFrontOfQueue {
                try {
                    if (Build.VERSION.SDK_INT < 28) {
                        if (performStopActivity3ParamsMethod != null) {
                            performStopActivity3ParamsMethod!!.invoke(
                                activityThread,
                                token, false, "AppCompat recreation"
                            )
                        } else {
                            performStopActivity2ParamsMethod!!.invoke(
                                activityThread,
                                token, false
                            )
                        }
                    }
                } catch (e: RuntimeException) {
                    // If an Activity throws from onStop, don't swallow it
                    if ((e.javaClass == RuntimeException::class.java) &&
                        (e.message != null) &&
                        e.message!!.startsWith("Unable to stop")
                    ) {
                        throw e
                    }
                    // Otherwise just swallow it - we're calling random private methods,
                    // there's no guarantee on how they'll behave.
                } catch (t: Throwable) {
                    Log.e(
                        LOG_TAG,
                        "Exception while invoking performStopActivity",
                        t
                    )
                }
            }
            true
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "Exception while fetching field values", t)
            false
        }
    }

    private fun getPerformStopActivity3Params(activityThreadClass: Class<*>?): Method? {
        if (activityThreadClass == null) {
            return null
        }
        return try {
            val performStop = activityThreadClass.getDeclaredMethod(
                "performStopActivity",
                IBinder::class.java, Boolean::class.javaPrimitiveType, String::class.java
            )
            performStop.isAccessible = true
            performStop
        } catch (t: Throwable) {
            null
        }
    }

    private fun getPerformStopActivity2Params(activityThreadClass: Class<*>?): Method? {
        if (activityThreadClass == null) {
            return null
        }
        return try {
            val performStop = activityThreadClass.getDeclaredMethod(
                "performStopActivity",
                IBinder::class.java, Boolean::class.javaPrimitiveType
            )
            performStop.isAccessible = true
            performStop
        } catch (t: Throwable) {
            null
        }
    }

    @ChecksSdkIntAtLeast(api = 26)
    private fun needsRelaunchCall(): Boolean {
        return Build.VERSION.SDK_INT == 26 || Build.VERSION.SDK_INT == 27
    }

    private fun getRequestRelaunchActivityMethod(activityThreadClass: Class<*>?): Method? {
        if (!needsRelaunchCall() || activityThreadClass == null) {
            return null
        }
        return try {
            val relaunch = activityThreadClass.getDeclaredMethod(
                "requestRelaunchActivity",
                IBinder::class.java,
                MutableList::class.java,
                MutableList::class.java,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Configuration::class.java,
                Configuration::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType
            )
            relaunch.isAccessible = true
            relaunch
        } catch (t: Throwable) {
            null
        }
    }

    private fun getMainThreadField(): Field? {
        return try {
            val mainThreadField = Activity::class.java.getDeclaredField("mMainThread")
            mainThreadField.isAccessible = true
            mainThreadField
        } catch (t: Throwable) {
            null
        }
    }

    private fun getTokenField(): Field? {
        return try {
            val tokenField = Activity::class.java.getDeclaredField("mToken")
            tokenField.isAccessible = true
            tokenField
        } catch (t: Throwable) {
            null
        }
    }

    private val activityThreadClass: Class<*>?
        get() = try {
            Class.forName("android.app.ActivityThread")
        } catch (t: Throwable) {
            null
        }

    // Only reachable on SDK_INT < 28
    private class LifecycleCheckCallbacks internal constructor(aboutToRecreate: Activity) :
        ActivityLifecycleCallbacks {
        var currentlyRecreatingToken: Any? = null
        private var mActivity: Activity?
        private val mRecreatingHashCode: Int

        // Whether the activity on which recreate() was called went through onStart after
        // recreate() was called (and thus the callback was registered).
        private var mStarted = false

        // Whether the activity on which recreate() was called went through onDestroy after
        // recreate() was called. This means we successfully initiated a recreate().
        private var mDestroyed = false

        // Whether we'll force the activity on which recreate() was called to go through an
        // onStop()
        private var mStopQueued = false
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
        override fun onActivityStarted(activity: Activity) {
            // If we see a start call on the original mActivity instance, then the mActivity
            // starting event executed between our call to recreate() and the actual
            // recreation of the mActivity. In that case, a stop() call should not be scheduled.
            if (mActivity === activity) {
                mStarted = true
            }
        }

        override fun onActivityResumed(activity: Activity) {}
        override fun onActivityPaused(activity: Activity) {
            if (mDestroyed && // Original mActivity must be gone
                !mStopQueued && // Don't schedule stop twice for one recreate() call
                !mStarted && // Don't schedule stop if the original instance starting raced with...
                queueOnStopIfNecessary(currentlyRecreatingToken, mRecreatingHashCode, activity)
            ) {
                mStopQueued = true
                // Don't retain this object longer than necessary
                currentlyRecreatingToken = null
            }
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityStopped(activity: Activity) {
            // Not possible to get a start/stop pair in the same UI thread loop
        }

        override fun onActivityDestroyed(activity: Activity) {
            if (mActivity === activity) {
                // Once the original mActivity instance is mDestroyed, we don't need to compare to
                // it any
                // longer, and we don't want to retain it any longer than necessary.
                mActivity = null
                mDestroyed = true
            }
        }

        init {
            mActivity = aboutToRecreate
            mRecreatingHashCode = mActivity.hashCode()
        }
    }

    init {
        val activityThreadClass = activityThreadClass
        mainThreadField = getMainThreadField()
        tokenField = getTokenField()
        performStopActivity3ParamsMethod =
            getPerformStopActivity3Params(activityThreadClass)
        performStopActivity2ParamsMethod =
            getPerformStopActivity2Params(activityThreadClass)
        requestRelaunchActivityMethod =
            getRequestRelaunchActivityMethod(activityThreadClass)
    }
}