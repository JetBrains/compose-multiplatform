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

package androidx.compose.animation.core

import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Spring Simulation simulates spring physics, and allows you to query the motion (i.e. value and
 * velocity) at certain time in the future based on the starting velocity and value.
 *
 * By configuring the stiffness and damping ratio, callers can create a spring with the look and
 * feel suits their use case. Stiffness corresponds to the spring constant. The stiffer the spring
 * is, the harder it is to stretch it, the faster it undergoes dampening.
 *
 *
 * Spring damping ratio describes how oscillations in a system decay after a disturbance.
 * When damping ratio > 1* (i.e. over-damped), the object will quickly return to the rest position
 * without overshooting. If damping ratio equals to 1 (i.e. critically damped), the object will
 * return to equilibrium within the shortest amount of time. When damping ratio is less than 1
 * (i.e. under-damped), the mass tends to overshoot, and return, and overshoot again. Without any
 * damping (i.e. damping ratio = 0), the mass will oscillate forever.
 */

@kotlin.jvm.JvmInline
internal value class Motion(val packedValue: Long) {
    val value: Float
        get() = unpackFloat1(packedValue)
    val velocity: Float
        get() = unpackFloat2(packedValue)

    /**
     * Returns a copy of this Motion instance optionally overriding the
     * value or velocity parameters
     */
    fun copy(value: Float = this.value, velocity: Float = this.velocity) = Motion(value, velocity)
}

internal fun Motion(value: Float, velocity: Float) = Motion(packFloats(value, velocity))

// This multiplier is used to calculate the velocity threshold given a certain value threshold.
// The idea is that if it takes >= 1 frame to move the value threshold amount, then the velocity
// is a reasonable threshold.
private const val VelocityThresholdMultiplier = 1000.0 / 16.0

// Value to indicate an unset state.
internal val UNSET = Float.MAX_VALUE

internal class SpringSimulation(var finalPosition: Float) {

    // Natural frequency
    private var naturalFreq = sqrt(Spring.StiffnessVeryLow.toDouble())

    // Indicates whether the spring has been initialized
    private var initialized = false

    // Intermediate values to simplify the spring function calculation per frame.
    private var gammaPlus: Double = 0.0
    private var gammaMinus: Double = 0.0
    private var dampedFreq: Double = 0.0

    /**
     * Stiffness of the spring.
     */
    var stiffness: Float
        set(value) {
            if (stiffness <= 0) {
                throw IllegalArgumentException("Spring stiffness constant must be positive.")
            }
            naturalFreq = sqrt(value.toDouble())
            // All the intermediate values need to be recalculated.
            initialized = false
        }
        get() {
            return (naturalFreq * naturalFreq).toFloat()
        }

    /**
     * Returns the damping ratio of the spring.
     *
     * @return damping ratio of the spring
     */
    var dampingRatio: Float = Spring.DampingRatioNoBouncy
        set(value) {
            if (value < 0) {
                throw IllegalArgumentException("Damping ratio must be non-negative")
            }
            field = value
            // All the intermediate values need to be recalculated.
            initialized = false
        }

    /*********************** Below are private APIs  */

    fun getAcceleration(lastDisplacement: Float, lastVelocity: Float): Float {
        val adjustedDisplacement = lastDisplacement - finalPosition

        val k = naturalFreq * naturalFreq
        val c = 2.0 * naturalFreq * dampingRatio

        return (-k * adjustedDisplacement - c * lastVelocity).toFloat()
    }

    /**
     * Initialize the string by doing the necessary pre-calculation as well as some validity check
     * on the setup.
     *
     * @throws IllegalStateException if the final position is not yet set by the time the spring
     * animation has started
     */
    private fun init() {
        if (initialized) {
            return
        }

        if (finalPosition == UNSET) {
            throw IllegalStateException(
                "Error: Final position of the spring must be set before the animation starts"
            )
        }

        val dampingRatioSquared = dampingRatio * dampingRatio.toDouble()
        if (dampingRatio > 1) {
            // Over damping
            gammaPlus =
                (-dampingRatio * naturalFreq + naturalFreq * sqrt(dampingRatioSquared - 1))
            gammaMinus =
                (-dampingRatio * naturalFreq - naturalFreq * sqrt(dampingRatioSquared - 1))
        } else if (dampingRatio >= 0 && dampingRatio < 1) {
            // Under damping
            dampedFreq = naturalFreq * sqrt(1 - dampingRatioSquared)
        }

        initialized = true
    }

    /**
     * Internal only call for Spring to calculate the spring position/velocity using
     * an analytical approach.
     */
    internal fun updateValues(lastDisplacement: Float, lastVelocity: Float, timeElapsed: Long):
        Motion {
            init()

            val adjustedDisplacement = lastDisplacement - finalPosition
            val deltaT = timeElapsed / 1000.0 // unit: seconds
            val displacement: Double
            val currentVelocity: Double
            if (dampingRatio > 1) {
                // Overdamped
                val coeffA =
                    (
                        adjustedDisplacement - (
                            (gammaMinus * adjustedDisplacement - lastVelocity) /
                                (gammaMinus - gammaPlus)
                            )
                        )
                val coeffB = (
                    (gammaMinus * adjustedDisplacement - lastVelocity) /
                        (gammaMinus - gammaPlus)
                    )
                displacement = (
                    coeffA * exp(gammaMinus * deltaT) +
                        coeffB * exp(gammaPlus * deltaT)
                    )
                currentVelocity = (
                    coeffA * gammaMinus * exp(gammaMinus * deltaT) +
                        coeffB * gammaPlus * exp(gammaPlus * deltaT)
                    )
            } else if (dampingRatio == 1.0f) {
                // Critically damped
                val coeffA = adjustedDisplacement
                val coeffB = lastVelocity + naturalFreq * adjustedDisplacement
                displacement = (coeffA + coeffB * deltaT) * exp(-naturalFreq * deltaT)
                currentVelocity =
                    (
                        (
                            (coeffA + coeffB * deltaT) * exp(-naturalFreq * deltaT) *
                                (-naturalFreq)
                            ) + coeffB * exp(-naturalFreq * deltaT)
                        )
            } else {
                // Underdamped
                val cosCoeff = adjustedDisplacement
                val sinCoeff =
                    (
                        (1 / dampedFreq) * (
                            (
                                (dampingRatio * naturalFreq * adjustedDisplacement) +
                                    lastVelocity
                                )
                            )
                        )
                displacement = (
                    exp(-dampingRatio * naturalFreq * deltaT) *
                        (
                            (
                                cosCoeff * cos(dampedFreq * deltaT) +
                                    sinCoeff * sin(dampedFreq * deltaT)
                                )
                            )
                    )
                currentVelocity = (
                    displacement * (-naturalFreq) * dampingRatio + (
                        exp(
                            -dampingRatio * naturalFreq * deltaT
                        ) * (
                            (
                                -dampedFreq * cosCoeff *
                                    sin(dampedFreq * deltaT) + dampedFreq * sinCoeff *
                                    cos(dampedFreq * deltaT)
                                )
                            )
                        )
                    )
            }

            val newValue = (displacement + finalPosition).toFloat()
            val newVelocity = currentVelocity.toFloat()

            return Motion(newValue, newVelocity)
        }
}
