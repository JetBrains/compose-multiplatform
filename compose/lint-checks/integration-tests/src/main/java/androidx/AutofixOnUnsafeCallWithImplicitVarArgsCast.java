/*
 * Copyright 2023 The Android Open Source Project
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

import android.widget.BaseAdapter;

import androidx.annotation.RequiresApi;

import java.nio.CharBuffer;

/**
 * Contains unsafe calls to a method with a variable number of arguments which are implicitly cast.
 */
@SuppressWarnings("unused")
public class AutofixOnUnsafeCallWithImplicitVarArgsCast {
    /**
     * Calls the vararg method with no args.
     */
    @RequiresApi(27)
    public void callVarArgsMethodNoArgs(BaseAdapter adapter) {
        adapter.setAutofillOptions();
    }

    /**
     *Calls the vararg method with one args.
     */
    @RequiresApi(27)
    public void callVarArgsMethodOneArg(BaseAdapter adapter, CharBuffer vararg) {
        adapter.setAutofillOptions(vararg);
    }

    /**
     * Calls the vararg method with multiple args.
     */
    @RequiresApi(27)
    public void callVarArgsMethodManyArgs(BaseAdapter adapter, CharBuffer vararg1,
            CharBuffer vararg2, CharBuffer vararg3) {
        adapter.setAutofillOptions(vararg1, vararg2, vararg3);
    }
}
