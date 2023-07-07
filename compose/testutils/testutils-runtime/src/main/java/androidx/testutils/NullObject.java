/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.annotation.NonNull;

/**
 * Provides null objects for testing.
 */
public class NullObject {
    private NullObject() {
        // This class is not instantiable.
    }

    /**
     * Returns a null object pretending to be non-null.
     * <p>
     * Useful for testing handling of null parameters in Java code called from Kotlin tests.
     */
    @SuppressWarnings("TypeParameterUnusedInFormals")
    @NonNull
    public static <T> T get() {
        //noinspection ConstantConditions
        return null;
    }
}
