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

package androidx;

import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

/**
 * Test class containing class verification failure scenarios.
 */
@SuppressWarnings("unused")
public class ClassVerificationFailureFromJava {

    /**
     * Unsafe reference to a new API with an SDK_INT check that satisfies the NewApi lint.
     */
    void unsafeReferenceWithSdkCheck(View view) {
        if (Build.VERSION.SDK_INT > 23) {
            ColorStateList tint = new ColorStateList(null, null);
            view.setBackgroundTintList(tint);
        }
    }

    /**
     * Unsafe static reference to a new API with an SDK_INT check that satisfies the NewApi lint.
     */
    int unsafeStaticReferenceWithSdkCheck() {
        if (Build.VERSION.SDK_INT >= 17) {
            return View.generateViewId();
        } else {
            return -1;
        }
    }

    /**
     * Unsafe reference to a new API whose auto-fix collides with the existing Api28Impl class.
     */
    CharSequence unsafeReferenceWithAutoFixCollision(View view) {
        return view.getAccessibilityClassName();
    }

    /**
     * Safe reference to a new API on a static inner class.
     */
    CharSequence safeGetAccessibilityPaneTitle(View view) {
        if (Build.VERSION.SDK_INT >= 28) {
            return Api28Impl.getAccessibilityPaneTitle(view);
        } else {
            return null;
        }
    }

    @RequiresApi(28)
    static class Api28Impl {

        private Api28Impl() {
            // Not instantiable.
        }

        public static CharSequence getAccessibilityPaneTitle(View view) {
            return view.getAccessibilityPaneTitle();
        }
    }
}
