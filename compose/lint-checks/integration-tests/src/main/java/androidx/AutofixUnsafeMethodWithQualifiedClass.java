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

package androidx;

import android.print.PrintAttributes;

import androidx.annotation.RequiresApi;

/**
 * Test class containing unsafe method reference that uses qualified class names.
 */
@SuppressWarnings("unused")
public class AutofixUnsafeMethodWithQualifiedClass {
    /**
     * This method call:
     * - has a receiver of type PrintAttributes.Builder
     * - takes param of type PrintAttributes.MediaSize
     * - has return type PrintAttributes.Builder
     * In the generated fix, all three types should appear as qualified class names.
     */
    @RequiresApi(19)
    public PrintAttributes.Builder unsafeReferenceWithQualifiedClasses(
            PrintAttributes.Builder builder,
            PrintAttributes.MediaSize mediaSize
    ) {
        return builder.setMediaSize(mediaSize);
    }
}
