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
package androidx.compose.plugins.idea;

import com.intellij.CommonBundle;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;

/**
 * Compose resource bundle
 */
public class ComposeBundle {
    private static Reference<ResourceBundle> sOurBundle;

    @NonNls
    private static final String BUNDLE = "androidx.compose.plugins.idea.ComposeBundle";

    private ComposeBundle() {
    }

    /**
     * Get a message by key
     * @param key The key for the message
     * @param params Substitution parameters
     * @return the localized message
     */
    @NotNull
    public static String message(
            @NonNls @PropertyKey(resourceBundle = BUNDLE) String key,
            Object... params
    ) {
        return CommonBundle.message(getBundle(), key, params);
    }

    private static ResourceBundle getBundle() {
        ResourceBundle bundle = null;
        if (sOurBundle != null) bundle = sOurBundle.get();
        if (bundle == null) {
            bundle = ResourceBundle.getBundle(BUNDLE);
            sOurBundle = new SoftReference<ResourceBundle>(bundle);
        }
        return bundle;
    }
}
