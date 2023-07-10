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

import android.os.Build;
import android.view.ViewGroup;

/**
 * Test class containing unsafe references to a parent's instance method.
 */
@SuppressWarnings("unused")
public abstract class AutofixUnsafeCallToThis extends ViewGroup {
    /*
     * Constructor to prevent complication error.
     */
    public AutofixUnsafeCallToThis() {
        super(null);
    }

    /**
     * Method making the unsafe reference on an implicit this.
     */
    public void unsafeReferenceOnImplicitThis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getClipToPadding();
        }
    }

    /**
     * Method making the unsafe reference on an explicit this.
     */
    public void unsafeReferenceOnExplicitThis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.getClipToPadding();
        }
    }

    /**
     * Method making the unsafe reference on an explicit super.
     */
    public void unsafeReferenceOnExplicitSuper() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.getClipToPadding();
        }
    }
}
