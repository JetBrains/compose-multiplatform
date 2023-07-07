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

package androidx.testutils

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.testutils.runtime.R

class TestActivity : Activity(), Resettable {
    override fun setFinishEnabled(finishEnabled: Boolean) {
        finishEnabledFlag = finishEnabled
    }

    private var finishEnabledFlag = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.content_view)
        println("onCreate")
        overridePendingTransition(0, 0)
    }

    override fun onResume() {
        super.onResume()
        resumes++
    }

    override fun finish() {
        if (!finishEnabledFlag) {
            // sometimes we get surprise finish's...
            Log.d("System.out", "early finish", Exception())
            return
        }

        super.finish()
        overridePendingTransition(0, 0)
    }

    companion object {
        var resumes = 0
    }
}