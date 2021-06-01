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
package androidx.compose.ui.test

import android.os.Build
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ViewLayer
import java.util.concurrent.CountDownLatch

open class TestActivity : ComponentActivity() {
    var receivedKeyEvent: KeyEvent? = null

    var hasFocusLatch = CountDownLatch(1)

    var stopLatch = CountDownLatch(1)
    var resumeLatch = CountDownLatch(1)

    init {
        setViewLayerTypeForApi28()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hasFocusLatch.countDown()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        receivedKeyEvent = event
        return super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        stopLatch.countDown()
        if (resumedActivity === this) {
            resumedActivity = null
        }
    }

    override fun onResume() {
        resumedActivity = this
        super.onResume()
        resumeLatch.countDown()
    }

    companion object {
        var resumedActivity: TestActivity? = null
    }
}

/**
 * We have a ViewLayer that doesn't use reflection that won't be activated on
 * any Google devices, so we must trigger it directly. Here, we use it on all P
 * devices. The normal ViewLayer is used on L devices. RenderNodeLayer is used
 * on all other devices.
 */
internal fun setViewLayerTypeForApi28() {
    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
        ViewLayer.shouldUseDispatchDraw = true
    }
}
