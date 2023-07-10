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

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Test class containing unsafe method references.
 */
@SuppressWarnings("unused")
public class AutofixUnsafeConstructorReferenceJava {

    /**
     * Unsafe reference to a new API with an SDK_INT check that satisfies the NewApi lint.
     */
    void unsafeReferenceWithSdkCheck(Context context) {
        if (Build.VERSION.SDK_INT >= 30) {
            AccessibilityNodeInfo node = new AccessibilityNodeInfo(new View(context), 1);
        }
    }
}
