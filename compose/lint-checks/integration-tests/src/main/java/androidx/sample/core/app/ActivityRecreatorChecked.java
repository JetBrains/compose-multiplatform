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

package androidx.sample.core.app;

import static android.os.Build.VERSION.SDK_INT;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.ChecksSdkIntAtLeast;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * The goal here is to get common (and correct) behavior around Activity recreation for all API
 * versions up until P, where the behavior was specified to be useful and implemented to match the
 * specification. On API 26 and 27, recreate() doesn't actually recreate the Activity if it's
 * not in the foreground; it will be recreated when the user next interacts with it. This has a few
 * undesirable consequences:
 *
 * <p>1. It's impossible to recreate multiple activities at once, which means that activities in the
 * background will observe the new configuration before they're recreated. If we keep them on the
 * old configuration, we have two conflicting configurations active in the app, which leads to
 * logging skew.
 *
 * <p>2. Recreation occurs in the critical path of user interaction - re-inflating a bunch of views
 * isn't free, and we'd rather do it when we're in the background than when the user is staring at
 * the screen waiting to see us.
 *
 * <p>On API < 26, recreate() was implemented with a single call to a private method on
 * ActivityThread. That method still exists in 26 and 27, so we can use reflection to call it and
 * get the exact same behavior as < 26. However, that behavior has problems itself. When
 * an Activity in the background is recreated, it goes through: destroy -> create -> start ->
 * resume -> pause and doesn't stop. This is a violation of the contract for onStart/onStop,
 * but that might be palatable if it didn't also have the effect of preventing new configurations
 * from being applied - since the Activity doesn't go through onStop, our tracking of whether
 * our app is visible thinks we're always visible, and thus can't do another recreation later.
 *
 * <p>The fix for this is to add the missing onStop() call, by using reflection to call into
 * ActivityThread.
 *
 * @hide
 */
@RestrictTo(LIBRARY)
@SuppressWarnings({"PrivateApi", "JavaReflectionMemberAccess", "unused"})
final class ActivityRecreatorChecked {
    private ActivityRecreatorChecked() {}

    private static final String LOG_TAG = "ActivityRecreatorChecked";

    // Activity.mMainThread
    protected static final Field mainThreadField;
    // Activity.mToken. This object is an identifier that is the same between multiple instances of
    //the same underlying Activity.
    protected static final Field tokenField;
    // On API 25, a third param was added to performStopActivity
    protected static final Method performStopActivity3ParamsMethod;
    // Before API 25, performStopActivity had two params
    protected static final Method performStopActivity2ParamsMethod;
    // ActivityThread.requestRelaunchActivity
    protected static final Method requestRelaunchActivityMethod;

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    static {
        Class<?> activityThreadClass = getActivityThreadClass();
        mainThreadField = getMainThreadField();
        tokenField = getTokenField();
        performStopActivity3ParamsMethod = getPerformStopActivity3Params(activityThreadClass);
        performStopActivity2ParamsMethod = getPerformStopActivity2Params(activityThreadClass);
        requestRelaunchActivityMethod = getRequestRelaunchActivityMethod(activityThreadClass);
    }

    /**
     * Equivalent to {@link Activity#recreate}, but working around a number of platform bugs.
     *
     * @return true if a recreate() task was successfully scheduled.
     */
    static boolean recreate(@NonNull final Activity activity) {
        // On Android O and later we can rely on the platform recreate()
        if (SDK_INT >= 28) {
            activity.recreate();
            return true;
        }

        // API 26 needs this workaround but it's not possible because our reflective lookup failed.
        if (needsRelaunchCall() && requestRelaunchActivityMethod == null) {
            return false;
        }
        // All versions of android so far need this workaround, but it's not possible because our
        // reflective lookup failed.
        if (performStopActivity2ParamsMethod == null && performStopActivity3ParamsMethod == null) {
            return false;
        }
        try {
            final Object token = tokenField.get(activity);
            if (token == null) {
                return false;
            }
            Object activityThread = mainThreadField.get(activity);
            if (activityThread == null) {
                return false;
            }

            final Application application = activity.getApplication();
            final LifecycleCheckCallbacks callbacks = new LifecycleCheckCallbacks(activity);
            application.registerActivityLifecycleCallbacks(callbacks);

            /*
             * Runnables scheduled before/after recreate() will run before and after the Runnables
             * scheduled by recreate(). This allows us to bound the time where mActivity lifecycle
             * events that could be caused by recreate() run - that way we can detect onPause()
             * from the new Activity instance, and schedule onStop to run immediately after it.
             */
            mainHandler.post(() -> callbacks.currentlyRecreatingToken = token);

            try {
                if (needsRelaunchCall()) {
                    requestRelaunchActivityMethod.invoke(activityThread,
                            token, null, null, 0, false, null, null, false, false);
                } else {
                    activity.recreate();
                }
                return true;
            } finally {
                mainHandler.post(() -> {
                    // Since we're calling hidden API, it's entirely possible for it to
                    // simply do nothing;
                    // if that's the case, make sure to unregister so we don't leak memory
                    // waiting for an event that will never happen.
                    application.unregisterActivityLifecycleCallbacks(callbacks);
                });
            }
        } catch (Throwable t) {
            return false;
        }
    }

    // Only reachable on SDK_INT < 28
    private static final class LifecycleCheckCallbacks implements ActivityLifecycleCallbacks {
        Object currentlyRecreatingToken;

        private Activity mActivity;
        private final int mRecreatingHashCode;

        // Whether the activity on which recreate() was called went through onStart after
        // recreate() was called (and thus the callback was registered).
        private boolean mStarted = false;

        // Whether the activity on which recreate() was called went through onDestroy after
        // recreate() was called. This means we successfully initiated a recreate().
        private boolean mDestroyed = false;

        // Whether we'll force the activity on which recreate() was called to go through an
        // onStop()
        private boolean mStopQueued = false;

        LifecycleCheckCallbacks(@NonNull Activity aboutToRecreate) {
            mActivity = aboutToRecreate;
            mRecreatingHashCode = mActivity.hashCode();
        }

        @Override
        public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityStarted(Activity activity) {
            // If we see a start call on the original mActivity instance, then the mActivity
            // starting event executed between our call to recreate() and the actual
            // recreation of the mActivity. In that case, a stop() call should not be scheduled.
            if (mActivity == activity) {
                mStarted = true;
            }
        }

        @Override
        public void onActivityResumed(Activity activity) {
        }

        @Override
        public void onActivityPaused(Activity activity) {
            if (mDestroyed // Original mActivity must be gone
                    && !mStopQueued // Don't schedule stop twice for one recreate() call
                    && !mStarted
                    // Don't schedule stop if the original instance starting raced with recreate()
                    && queueOnStopIfNecessary(
                    currentlyRecreatingToken, mRecreatingHashCode, activity)) {
                mStopQueued = true;
                // Don't retain this object longer than necessary
                currentlyRecreatingToken = null;
            }
        }

        @Override
        public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        }

        @Override
        public void onActivityStopped(Activity activity) {
            // Not possible to get a start/stop pair in the same UI thread loop
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            if (mActivity == activity) {
                // Once the original mActivity instance is mDestroyed, we don't need to compare to
                // it any
                // longer, and we don't want to retain it any longer than necessary.
                mActivity = null;
                mDestroyed = true;
            }
        }
    }

    /**
     * Returns true if a stop call was scheduled successfully.
     *
     * Only reachable on SDK < 28.
     */
    protected static boolean queueOnStopIfNecessary(
            Object currentlyRecreatingToken, int currentlyRecreatingHashCode, Activity activity) {
        try {
            final Object token = tokenField.get(activity);
            if (token != currentlyRecreatingToken
                    || activity.hashCode() != currentlyRecreatingHashCode) {
                // We're looking at a different activity, don't try to make it stop! Note that
                // tokens are reused on SDK 21-23 but Activity objects (and thus hashCode, in
                // all likelihood) are not, so we need to check both.
                return false;
            }
            final Object activityThread = mainThreadField.get(activity);
            // These operations are posted at the front of the queue, so that operations
            // scheduled from onCreate, onStart etc run after the onStop call - this should
            // cause any redundant loads to be immediately cancelled.
            mainHandler.postAtFrontOfQueue(() -> {
                try {
                    if (SDK_INT < 28) {
                        if (performStopActivity3ParamsMethod != null) {
                            performStopActivity3ParamsMethod.invoke(activityThread,
                                    token, false, "AppCompat recreation");
                        } else {
                            performStopActivity2ParamsMethod.invoke(activityThread,
                                    token, false);
                        }
                    }
                } catch (RuntimeException e) {
                    // If an Activity throws from onStop, don't swallow it
                    if (e.getClass() == RuntimeException.class
                            && e.getMessage() != null
                            && e.getMessage().startsWith("Unable to stop")) {
                        throw e;
                    }
                    // Otherwise just swallow it - we're calling random private methods,
                    // there's no guarantee on how they'll behave.
                } catch (Throwable t) {
                    Log.e(LOG_TAG, "Exception while invoking performStopActivity", t);
                }
            });
            return true;
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Exception while fetching field values", t);
            return false;
        }
    }

    private static Method getPerformStopActivity3Params(Class<?> activityThreadClass) {
        if (activityThreadClass == null) {
            return null;
        }
        try {
            Method performStop = activityThreadClass.getDeclaredMethod("performStopActivity",
                    IBinder.class, boolean.class, String.class);
            performStop.setAccessible(true);
            return performStop;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Method getPerformStopActivity2Params(Class<?> activityThreadClass) {
        if (activityThreadClass == null) {
            return null;
        }
        try {
            Method performStop = activityThreadClass.getDeclaredMethod("performStopActivity",
                    IBinder.class, boolean.class);
            performStop.setAccessible(true);
            return performStop;
        } catch (Throwable t) {
            return null;
        }
    }

    @ChecksSdkIntAtLeast(api = 26)
    private static boolean needsRelaunchCall() {
        return SDK_INT == 26 || SDK_INT == 27;
    }

    private static Method getRequestRelaunchActivityMethod(Class<?> activityThreadClass) {
        if (!needsRelaunchCall() || activityThreadClass == null) {
            return null;
        }
        try {
            Method relaunch = activityThreadClass.getDeclaredMethod(
                    "requestRelaunchActivity",
                    IBinder.class,
                    List.class,
                    List.class,
                    int.class,
                    boolean.class,
                    Configuration.class,
                    Configuration.class,
                    boolean.class,
                    boolean.class);
            relaunch.setAccessible(true);
            return relaunch;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field getMainThreadField() {
        try {
            Field mainThreadField = Activity.class.getDeclaredField("mMainThread");
            mainThreadField.setAccessible(true);
            return mainThreadField;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Field getTokenField() {
        try {
            Field tokenField = Activity.class.getDeclaredField("mToken");
            tokenField.setAccessible(true);
            return tokenField;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Class<?> getActivityThreadClass() {
        try {
            return Class.forName("android.app.ActivityThread");
        } catch (Throwable t) {
            return null;
        }
    }
}
