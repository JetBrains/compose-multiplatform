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

package androidx.compose.ui

import java.lang.ref.WeakReference

// similar to leanback/leanback/src/androidTest/java/androidx/leanback/testutils/LeakDetector.java
class LeakDetector {
    private val weakReferences = ArrayList<WeakReference<*>>()

    fun observeObject(obj: Any) {
        weakReferences.add(WeakReference(obj))
    }

    fun noLeak(): Boolean {
        System.gc()
        System.runFinalization()
        for (weakReference in weakReferences) {
            var count = 0
            while (weakReference.get() != null && count < 5) {
                System.gc()
                System.runFinalization()
                Thread.sleep(1000)
                count++
            }

            if (weakReference.get() != null) {
                return false
            }
        }
        return true
    }
}
