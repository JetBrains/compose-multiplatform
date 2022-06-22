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

package androidx.compose.foundation

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.widget.EdgeEffect
import androidx.annotation.DoNotInline
import androidx.annotation.RequiresApi

internal object EdgeEffectCompat {

    fun create(context: Context, attrs: AttributeSet?): EdgeEffect {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Api31Impl.create(context, attrs)
        } else EdgeEffect(context)
    }

    fun EdgeEffect.onPullDistanceCompat(
        deltaDistance: Float,
        displacement: Float
    ): Float {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return Api31Impl.onPullDistance(this, deltaDistance, displacement)
        }
        this.onPull(deltaDistance, displacement)
        return deltaDistance
    }

    fun EdgeEffect.onAbsorbCompat(velocity: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return this.onAbsorb(velocity)
        } else if (this.isFinished) { // only absorb the glow effect if it is not active (finished)
            this.onAbsorb(velocity)
        }
    }

    val EdgeEffect.distanceCompat: Float
        get() {
            return if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)) {
                Api31Impl.getDistance(this)
            } else 0f
        }
}

@RequiresApi(Build.VERSION_CODES.S)
private object Api31Impl {
    @DoNotInline
    fun create(context: Context, attrs: AttributeSet?): EdgeEffect {
        return try {
            EdgeEffect(context, attrs)
        } catch (t: Throwable) {
            EdgeEffect(context) // Old preview release
        }
    }

    @DoNotInline
    fun onPullDistance(
        edgeEffect: EdgeEffect,
        deltaDistance: Float,
        displacement: Float
    ): Float {
        return try {
            edgeEffect.onPullDistance(deltaDistance, displacement)
        } catch (t: Throwable) {
            edgeEffect.onPull(deltaDistance, displacement) // Old preview release
            0f
        }
    }

    @DoNotInline
    fun getDistance(edgeEffect: EdgeEffect): Float {
        return try {
            edgeEffect.getDistance()
        } catch (t: Throwable) {
            0f // Old preview release
        }
    }
}