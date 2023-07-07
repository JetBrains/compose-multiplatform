/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.testutils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for testing LifecycleOwners
 */
public class LifecycleOwnerUtils {
    private static final long TIMEOUT_MS = 5000;

    private static final Runnable DO_NOTHING = new Runnable() {
        @Override
        public void run() {
        }
    };

    /**
     * Waits until the given {@link LifecycleOwner} has the specified
     * {@link Lifecycle.State}. If the owner has not hit that state within a
     * suitable time period, it asserts that the current state equals the given state.
     */
    public static void waitUntilState(final @NonNull LifecycleOwner owner,
            final @NonNull Lifecycle.State state) throws Throwable {
        final Lifecycle.State currentState = owner.getLifecycle().getCurrentState();
        if (currentState == state) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                final Lifecycle.State currentState = owner.getLifecycle().getCurrentState();
                if (currentState == state) {
                    latch.countDown();
                    return;
                }
                owner.getLifecycle().addObserver(new LifecycleEventObserver() {
                    @Override
                    public void onStateChanged(@NonNull LifecycleOwner provider,
                            @NonNull Lifecycle.Event event) {
                        if (provider.getLifecycle().getCurrentState() == state) {
                            provider.getLifecycle().removeObserver(this);
                            latch.countDown();
                        }
                    }
                });
            }
        });
        final boolean latchResult = latch.await(15, TimeUnit.SECONDS);

        assertThat("Expected " + state + " never happened to " + owner
                        + ". Current state:" + owner.getLifecycle().getCurrentState(),
                latchResult,
                is(true));

        // wait for another loop to ensure all observers are called
        InstrumentationRegistry.getInstrumentation().runOnMainSync(DO_NOTHING);
    }

    /**
     * Waits until the given the current {@link Activity} has been recreated, and
     * the new instance is resumed.
     */
    @NonNull
    public static <T extends Activity & LifecycleOwner> T waitForRecreation(
            @SuppressWarnings("deprecation")
            @NonNull final androidx.test.rule.ActivityTestRule<T> activityRule
    ) throws Throwable {
        return waitForRecreation(activityRule.getActivity());
    }

    /**
     * Waits until the given the given {@link Activity} has been recreated, and
     * the new instance is resumed.
     */
    @NonNull
    public static <T extends Activity & LifecycleOwner> T waitForRecreation(
            @NonNull final T activity
    ) throws Throwable {
        return waitForRecreation(activity, null);
    }

    /**
     * Waits until the given {@link Activity} and {@link LifecycleOwner} has been recreated, and
     * the new instance is resumed.
     */
    @SuppressWarnings("unchecked")
    @NonNull
    public static <T extends Activity & LifecycleOwner> T waitForRecreation(
            @NonNull final T activity,
            @Nullable final Runnable actionOnUiThread
    ) throws Throwable {
        final Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                activity.getClass().getName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);

        if (actionOnUiThread != null) {
            instrumentation.runOnMainSync(actionOnUiThread);
        }

        // Wait for the old activity to be destroyed. This helps avoid flakiness on test devices
        // (ex. API 26) where the system takes a long time to go from STOPPED to DESTROYED.
        waitUntilState(activity, Lifecycle.State.DESTROYED);

        T result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        // noinspection SynchronizationOnLocalVariableOrMethodParameter
        try {
            synchronized (monitor) {
                do {
                    // The documentation says "Block until an Activity is created
                    // that matches this monitor." This statement is true, but there are some other
                    // true statements like: "Block until an Activity is destroyed" or
                    // "Block until an Activity is resumed"...
                    // this call will release synchronization monitor's monitor
                    result = (T) monitor.waitForActivityWithTimeout(TIMEOUT_MS);
                    if (result == null) {
                        throw new RuntimeException("Timeout. Activity was not recreated.");
                    }
                } while (result == activity);
            }
        } finally {
            instrumentation.removeMonitor(monitor);
        }

        // Finally wait for the recreated Activity to be resumed
        waitUntilState(result, Lifecycle.State.RESUMED);

        return result;
    }

    private LifecycleOwnerUtils() {
    }
}
