/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.graphics

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.os.Build
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

internal object CanvasUtils {
    private var reorderBarrierMethod: Method? = null
    private var inorderBarrierMethod: Method? = null
    private var orderMethodsFetched = false

    /**
     * Enables Z support for the Canvas.
     *
     * This is only supported on Lollipop and later.
     */
    @SuppressLint("SoonBlockedPrivateApi")
    fun enableZ(canvas: Canvas, enable: Boolean) {
        if (Build.VERSION.SDK_INT >= 29) {
            CanvasZHelper.enableZ(canvas, enable)
        } else {
            if (!orderMethodsFetched) {
                try {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                        // use double reflection to avoid grey list on P
                        val getDeclaredMethod = Class::class.java.getDeclaredMethod(
                            "getDeclaredMethod",
                            String::class.java,
                            arrayOf<Class<*>>()::class.java
                        )
                        reorderBarrierMethod = getDeclaredMethod.invoke(
                            Canvas::class.java,
                            "insertReorderBarrier",
                            emptyArray<Class<*>>()
                        ) as Method?
                        inorderBarrierMethod = getDeclaredMethod.invoke(
                            Canvas::class.java,
                            "insertInorderBarrier",
                            emptyArray<Class<*>>()
                        ) as Method?
                    } else {
                        reorderBarrierMethod = Canvas::class.java.getDeclaredMethod(
                            "insertReorderBarrier"
                        )
                        inorderBarrierMethod = Canvas::class.java.getDeclaredMethod(
                            "insertInorderBarrier"
                        )
                    }
                    reorderBarrierMethod?.isAccessible = true
                    inorderBarrierMethod?.isAccessible = true
                } catch (ignore: IllegalAccessException) { // Do nothing
                } catch (ignore: InvocationTargetException) { // Do nothing
                } catch (ignore: NoSuchMethodException) { // Do nothing
                }
                orderMethodsFetched = true
            }
            try {
                if (enable && reorderBarrierMethod != null) {
                    reorderBarrierMethod!!.invoke(canvas)
                }
                if (!enable && inorderBarrierMethod != null) {
                    inorderBarrierMethod!!.invoke(canvas)
                }
            } catch (ignore: IllegalAccessException) { // Do nothing
            } catch (ignore: InvocationTargetException) { // Do nothing
            }
        }
    }
}

@RequiresApi(29)
private object CanvasZHelper {
    @DoNotInline
    fun enableZ(canvas: Canvas, enable: Boolean) {
        if (enable) {
            canvas.enableZ()
        } else {
            canvas.disableZ()
        }
    }
}