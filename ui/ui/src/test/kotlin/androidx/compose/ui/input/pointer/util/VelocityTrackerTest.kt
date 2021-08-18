/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.compose.ui.input.pointer.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class VelocityTrackerTest {

    // TODO(shepshapard): This test needs to be broken up into smaller tests
    // that make edge cases clearer.  Right now its just a bunch of inputs and outputs
    // and its impossible for the reader to know how well different cases are being tested.
    @Test
    fun calculateVelocity_returnsExpectedValues() {

        val expected = listOf(
            Pair(219.59280094228163f, 1304.701682306001f),
            Pair(355.71046950050845f, 967.2112857054104f),
            Pair(12.657970884022308f, -36.90447839251946f),
            Pair(714.1399654786744f, -2561.534447931869f),
            Pair(-19.668121066218564f, -2910.105747052462f),
            Pair(646.8690114934209f, 2976.977762577527f),
            Pair(396.6988447819592f, 2106.225572911095f),
            Pair(298.31594440044495f, -3660.8315955215294f),
            Pair(-1.7334232785165882f, -3288.13174127454f),
            Pair(384.6361280392334f, -2645.6612524779835f),
            Pair(176.37900397918557f, 2711.2542876273264f),
            Pair(396.9328560260098f, 4280.651578291764f),
            Pair(-71.51939428321249f, 3716.7385187526947f)
        )

        val tracker = VelocityTracker()
        var i = 0
        velocityEventData.forEach {
            if (it.down) {
                tracker.addPosition(it.uptime, it.position)
            } else {
                checkVelocity(tracker.calculateVelocity(), expected[i].first, expected[i].second)
                tracker.resetTracking()
                i += 1
            }
        }
    }

    @Test
    fun calculateVelocity_gapOf40MillisecondsInPositions_positionsAfterGapIgnored() {
        val tracker = VelocityTracker()
        interruptedVelocityEventData.forEach {
            if (it.down) {
                tracker.addPosition(it.uptime, it.position)
            } else {
                checkVelocity(
                    tracker.calculateVelocity(),
                    649.48932102748f,
                    3890.30505589076f
                )
                tracker.resetTracking()
            }
        }
    }

    @Test
    fun calculateVelocity_noData_returnsZero() {
        val tracker = VelocityTracker()
        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    @Test
    fun calculateVelocity_onePosition_returnsZero() {
        val tracker = VelocityTracker()
        tracker.addPosition(
            velocityEventData[0].uptime,
            velocityEventData[0].position
        )
        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    @Test
    fun resetTracking_resetsTracking() {
        val tracker = VelocityTracker()
        tracker.addPosition(
            velocityEventData[0].uptime,
            velocityEventData[0].position
        )

        tracker.resetTracking()

        assertThat(tracker.calculateVelocity()).isEqualTo(Velocity.Zero)
    }

    private fun checkVelocity(actual: Velocity, expectedDx: Float, expectedDy: Float) {
        assertThat(actual.x).isWithin(0.1f).of(expectedDx)
        assertThat(actual.y).isWithin(0.1f).of(expectedDy)
    }
}

/**
 * This extracts the inline PxPosition to a separate function so that velocityEventData
 * creation doesn't make the function too long for dex.
 */
private fun createPxPosition(width: Float, height: Float) = Offset(width, height)

internal class PointerInputData(
    val uptime: Long,
    val position: Offset,
    val down: Boolean
)

internal val velocityEventData: List<PointerInputData> = listOf(
    PointerInputData(
        uptime = 216690896L,
        down = true,
        position = createPxPosition(270f, 538.2857055664062f)
    ),
    PointerInputData(
        uptime = 216690906L,
        down = true,
        position = createPxPosition(270f, 538.2857055664062f)
    ),
    PointerInputData(
        uptime = 216690951L,
        down = true,
        position = createPxPosition(270f, 530.8571166992188f)
    ),
    PointerInputData(
        uptime = 216690959L,
        down = true,
        position = createPxPosition(270f, 526.8571166992188f)
    ),
    PointerInputData(
        uptime = 216690967L,
        down = true,
        position = createPxPosition(270f, 521.4285888671875f)
    ),
    PointerInputData(
        uptime = 216690975L,
        down = true,
        position = createPxPosition(270f, 515.4285888671875f)
    ),
    PointerInputData(
        uptime = 216690983L,
        down = true,
        position = createPxPosition(270f, 506.8571472167969f)
    ),
    PointerInputData(
        uptime = 216690991L,
        down = true,
        position = createPxPosition(268.8571472167969f, 496f)
    ),
    PointerInputData(
        uptime = 216690998L,
        down = true,
        position = createPxPosition(267.4285583496094f, 483.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691006L,
        down = true,
        position = createPxPosition(266.28570556640625f, 469.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691014L,
        down = true,
        position = createPxPosition(265.4285583496094f, 456.8571472167969f)
    ),
    PointerInputData(
        uptime = 216691021L,
        down = true,
        position = createPxPosition(264.28570556640625f, 443.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691029L,
        down = true,
        position = createPxPosition(264f, 431.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691036L,
        down = true,
        position = createPxPosition(263.4285583496094f, 421.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691044L,
        down = true,
        position = createPxPosition(263.4285583496094f, 412.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691052L,
        down = true,
        position = createPxPosition(263.4285583496094f, 404.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691060L,
        down = true,
        position = createPxPosition(263.4285583496094f, 396.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691068L,
        down = true,
        position = createPxPosition(264.5714416503906f, 390f)
    ),
    PointerInputData(
        uptime = 216691075L,
        down = true,
        position = createPxPosition(265.1428527832031f, 384.8571472167969f)
    ),
    PointerInputData(
        uptime = 216691083L,
        down = true,
        position = createPxPosition(266f, 380.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691091L,
        down = true,
        position = createPxPosition(266.5714416503906f, 376.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691098L,
        down = true,
        position = createPxPosition(267.1428527832031f, 373.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691106L,
        down = true,
        position = createPxPosition(267.71429443359375f, 370.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691114L,
        down = true,
        position = createPxPosition(268.28570556640625f, 367.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691121L,
        down = true,
        position = createPxPosition(268.5714416503906f, 366f)
    ),
    PointerInputData(
        uptime = 216691130L,
        down = true,
        position = createPxPosition(268.8571472167969f, 364.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691137L,
        down = true,
        position = createPxPosition(269.1428527832031f, 363.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691145L,
        down = true,
        position = createPxPosition(269.1428527832031f, 362.8571472167969f)
    ),
    PointerInputData(
        uptime = 216691153L,
        down = true,
        position = createPxPosition(269.4285583496094f, 362.8571472167969f)
    ),
    PointerInputData(
        uptime = 216691168L,
        down = true,
        position = createPxPosition(268.5714416503906f, 365.4285583496094f)
    ),
    PointerInputData(
        uptime = 216691176L,
        down = true,
        position = createPxPosition(267.1428527832031f, 370.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691183L,
        down = true,
        position = createPxPosition(265.4285583496094f, 376.8571472167969f)
    ),
    PointerInputData(
        uptime = 216691191L,
        down = true,
        position = createPxPosition(263.1428527832031f, 385.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691199L,
        down = true,
        position = createPxPosition(261.4285583496094f, 396.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691207L,
        down = true,
        position = createPxPosition(259.71429443359375f, 408.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691215L,
        down = true,
        position = createPxPosition(258.28570556640625f, 419.4285583496094f)
    ),
    PointerInputData(
        uptime = 216691222L,
        down = true,
        position = createPxPosition(257.4285583496094f, 428.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691230L,
        down = true,
        position = createPxPosition(256.28570556640625f, 436f)
    ),
    PointerInputData(
        uptime = 216691238L,
        down = true,
        position = createPxPosition(255.7142791748047f, 442f)
    ),
    PointerInputData(
        uptime = 216691245L,
        down = true,
        position = createPxPosition(255.14285278320312f, 447.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691253L,
        down = true,
        position = createPxPosition(254.85714721679688f, 453.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691261L,
        down = true,
        position = createPxPosition(254.57142639160156f, 458.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691268L,
        down = true,
        position = createPxPosition(254.2857208251953f, 463.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691276L,
        down = true,
        position = createPxPosition(254.2857208251953f, 470.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691284L,
        down = true,
        position = createPxPosition(254.2857208251953f, 477.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691292L,
        down = true,
        position = createPxPosition(255.7142791748047f, 487.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691300L,
        down = true,
        position = createPxPosition(256.8571472167969f, 498.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691307L,
        down = true,
        position = createPxPosition(258.28570556640625f, 507.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691315L,
        down = true,
        position = createPxPosition(259.4285583496094f, 516f)
    ),
    PointerInputData(
        uptime = 216691323L,
        down = true,
        position = createPxPosition(260.28570556640625f, 521.7142944335938f)
    ),
    PointerInputData(
        uptime = 216691338L,
        down = false,
        position = createPxPosition(260.28570556640625f, 521.7142944335938f)
    ),
    PointerInputData(
        uptime = 216691573L,
        down = true,
        position = createPxPosition(266f, 327.4285583496094f)
    ),
    PointerInputData(
        uptime = 216691588L,
        down = true,
        position = createPxPosition(266f, 327.4285583496094f)
    ),
    PointerInputData(
        uptime = 216691626L,
        down = true,
        position = createPxPosition(261.1428527832031f, 337.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691634L,
        down = true,
        position = createPxPosition(258.28570556640625f, 343.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691642L,
        down = true,
        position = createPxPosition(254.57142639160156f, 354f)
    ),
    PointerInputData(
        uptime = 216691650L,
        down = true,
        position = createPxPosition(250.2857208251953f, 368.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691657L,
        down = true,
        position = createPxPosition(247.42857360839844f, 382.8571472167969f)
    ),
    PointerInputData(
        uptime = 216691665L,
        down = true,
        position = createPxPosition(245.14285278320312f, 397.4285583496094f)
    ),
    PointerInputData(
        uptime = 216691673L,
        down = true,
        position = createPxPosition(243.14285278320312f, 411.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691680L,
        down = true,
        position = createPxPosition(242.2857208251953f, 426.28570556640625f)
    ),
    PointerInputData(
        uptime = 216691688L,
        down = true,
        position = createPxPosition(241.7142791748047f, 440.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691696L,
        down = true,
        position = createPxPosition(241.7142791748047f, 454.5714416503906f)
    ),
    PointerInputData(
        uptime = 216691703L,
        down = true,
        position = createPxPosition(242.57142639160156f, 467.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691712L,
        down = true,
        position = createPxPosition(243.42857360839844f, 477.4285583496094f)
    ),
    PointerInputData(
        uptime = 216691720L,
        down = true,
        position = createPxPosition(244.85714721679688f, 485.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691727L,
        down = true,
        position = createPxPosition(246.2857208251953f, 493.1428527832031f)
    ),
    PointerInputData(
        uptime = 216691735L,
        down = true,
        position = createPxPosition(248f, 499.71429443359375f)
    ),
    PointerInputData(
        uptime = 216691750L,
        down = false,
        position = createPxPosition(248f, 499.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692255L,
        down = true,
        position = createPxPosition(249.42857360839844f, 351.4285583496094f)
    ),
    PointerInputData(
        uptime = 216692270L,
        down = true,
        position = createPxPosition(249.42857360839844f, 351.4285583496094f)
    ),
    PointerInputData(
        uptime = 216692309L,
        down = true,
        position = createPxPosition(246.2857208251953f, 361.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692317L,
        down = true,
        position = createPxPosition(244f, 368.5714416503906f)
    ),
    PointerInputData(
        uptime = 216692325L,
        down = true,
        position = createPxPosition(241.42857360839844f, 377.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692333L,
        down = true,
        position = createPxPosition(237.7142791748047f, 391.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692340L,
        down = true,
        position = createPxPosition(235.14285278320312f, 406.5714416503906f)
    ),
    PointerInputData(
        uptime = 216692348L,
        down = true,
        position = createPxPosition(232.57142639160156f, 421.4285583496094f)
    ),
    PointerInputData(
        uptime = 216692356L,
        down = true,
        position = createPxPosition(230.2857208251953f, 436.5714416503906f)
    ),
    PointerInputData(
        uptime = 216692363L,
        down = true,
        position = createPxPosition(228.2857208251953f, 451.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692371L,
        down = true,
        position = createPxPosition(227.42857360839844f, 466f)
    ),
    PointerInputData(
        uptime = 216692378L,
        down = true,
        position = createPxPosition(226.2857208251953f, 479.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692387L,
        down = true,
        position = createPxPosition(225.7142791748047f, 491.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692395L,
        down = true,
        position = createPxPosition(225.14285278320312f, 501.71429443359375f)
    ),
    PointerInputData(
        uptime = 216692402L,
        down = true,
        position = createPxPosition(224.85714721679688f, 509.1428527832031f)
    ),
    PointerInputData(
        uptime = 216692410L,
        down = true,
        position = createPxPosition(224.57142639160156f, 514.8571166992188f)
    ),
    PointerInputData(
        uptime = 216692418L,
        down = true,
        position = createPxPosition(224.2857208251953f, 519.4285888671875f)
    ),
    PointerInputData(
        uptime = 216692425L,
        down = true,
        position = createPxPosition(224f, 523.4285888671875f)
    ),
    PointerInputData(
        uptime = 216692433L,
        down = true,
        position = createPxPosition(224f, 527.1428833007812f)
    ),
    PointerInputData(
        uptime = 216692441L,
        down = true,
        position = createPxPosition(224f, 530.5714111328125f)
    ),
    PointerInputData(
        uptime = 216692448L,
        down = true,
        position = createPxPosition(224f, 533.1428833007812f)
    ),
    PointerInputData(
        uptime = 216692456L,
        down = true,
        position = createPxPosition(224f, 535.4285888671875f)
    ),
    PointerInputData(
        uptime = 216692464L,
        down = true,
        position = createPxPosition(223.7142791748047f, 536.8571166992188f)
    ),
    PointerInputData(
        uptime = 216692472L,
        down = true,
        position = createPxPosition(223.7142791748047f, 538.2857055664062f)
    ),
    PointerInputData(
        uptime = 216692487L,
        down = false,
        position = createPxPosition(223.7142791748047f, 538.2857055664062f)
    ),
    PointerInputData(
        uptime = 216692678L,
        down = true,
        position = createPxPosition(221.42857360839844f, 526.2857055664062f)
    ),
    PointerInputData(
        uptime = 216692701L,
        down = true,
        position = createPxPosition(220.57142639160156f, 514.8571166992188f)
    ),
    PointerInputData(
        uptime = 216692708L,
        down = true,
        position = createPxPosition(220.2857208251953f, 508f)
    ),
    PointerInputData(
        uptime = 216692716L,
        down = true,
        position = createPxPosition(220.2857208251953f, 498f)
    ),
    PointerInputData(
        uptime = 216692724L,
        down = true,
        position = createPxPosition(221.14285278320312f, 484.28570556640625f)
    ),
    PointerInputData(
        uptime = 216692732L,
        down = true,
        position = createPxPosition(221.7142791748047f, 469.4285583496094f)
    ),
    PointerInputData(
        uptime = 216692740L,
        down = true,
        position = createPxPosition(223.42857360839844f, 453.1428527832031f)
    ),
    PointerInputData(
        uptime = 216692748L,
        down = true,
        position = createPxPosition(225.7142791748047f, 436.28570556640625f)
    ),
    PointerInputData(
        uptime = 216692755L,
        down = true,
        position = createPxPosition(229.14285278320312f, 418.28570556640625f)
    ),
    PointerInputData(
        uptime = 216692763L,
        down = true,
        position = createPxPosition(232.85714721679688f, 400.28570556640625f)
    ),
    PointerInputData(
        uptime = 216692770L,
        down = true,
        position = createPxPosition(236.85714721679688f, 382.5714416503906f)
    ),
    PointerInputData(
        uptime = 216692778L,
        down = true,
        position = createPxPosition(241.14285278320312f, 366f)
    ),
    PointerInputData(
        uptime = 216692786L,
        down = true,
        position = createPxPosition(244.85714721679688f, 350.28570556640625f)
    ),
    PointerInputData(
        uptime = 216692793L,
        down = true,
        position = createPxPosition(249.14285278320312f, 335.4285583496094f)
    ),
    PointerInputData(
        uptime = 216692809L,
        down = false,
        position = createPxPosition(249.14285278320312f, 335.4285583496094f)
    ),
    PointerInputData(
        uptime = 216693222L,
        down = true,
        position = createPxPosition(224f, 545.4285888671875f)
    ),
    PointerInputData(
        uptime = 216693245L,
        down = true,
        position = createPxPosition(224f, 545.4285888671875f)
    ),
    PointerInputData(
        uptime = 216693275L,
        down = true,
        position = createPxPosition(222.85714721679688f, 535.1428833007812f)
    ),
    PointerInputData(
        uptime = 216693284L,
        down = true,
        position = createPxPosition(222.85714721679688f, 528.8571166992188f)
    ),
    PointerInputData(
        uptime = 216693291L,
        down = true,
        position = createPxPosition(222.2857208251953f, 518.5714111328125f)
    ),
    PointerInputData(
        uptime = 216693299L,
        down = true,
        position = createPxPosition(222f, 503.4285583496094f)
    ),
    PointerInputData(
        uptime = 216693307L,
        down = true,
        position = createPxPosition(222f, 485.4285583496094f)
    ),
    PointerInputData(
        uptime = 216693314L,
        down = true,
        position = createPxPosition(221.7142791748047f, 464f)
    ),
    PointerInputData(
        uptime = 216693322L,
        down = true,
        position = createPxPosition(222.2857208251953f, 440.28570556640625f)
    ),
    PointerInputData(
        uptime = 216693337L,
        down = false,
        position = createPxPosition(222.2857208251953f, 440.28570556640625f)
    ),
    PointerInputData(
        uptime = 216693985L,
        down = true,
        position = createPxPosition(208f, 544f)
    ),
    PointerInputData(
        uptime = 216694047L,
        down = true,
        position = createPxPosition(208.57142639160156f, 532.2857055664062f)
    ),
    PointerInputData(
        uptime = 216694054L,
        down = true,
        position = createPxPosition(208.85714721679688f, 525.7142944335938f)
    ),
    PointerInputData(
        uptime = 216694062L,
        down = true,
        position = createPxPosition(208.85714721679688f, 515.1428833007812f)
    ),
    PointerInputData(
        uptime = 216694070L,
        down = true,
        position = createPxPosition(208f, 501.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694077L,
        down = true,
        position = createPxPosition(207.42857360839844f, 487.1428527832031f)
    ),
    PointerInputData(
        uptime = 216694085L,
        down = true,
        position = createPxPosition(206.57142639160156f, 472.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694092L,
        down = true,
        position = createPxPosition(206.57142639160156f, 458.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694100L,
        down = true,
        position = createPxPosition(206.57142639160156f, 446f)
    ),
    PointerInputData(
        uptime = 216694108L,
        down = true,
        position = createPxPosition(206.57142639160156f, 434.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694116L,
        down = true,
        position = createPxPosition(207.14285278320312f, 423.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694124L,
        down = true,
        position = createPxPosition(208.57142639160156f, 412.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694131L,
        down = true,
        position = createPxPosition(209.7142791748047f, 402.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694139L,
        down = true,
        position = createPxPosition(211.7142791748047f, 393.1428527832031f)
    ),
    PointerInputData(
        uptime = 216694147L,
        down = true,
        position = createPxPosition(213.42857360839844f, 385.1428527832031f)
    ),
    PointerInputData(
        uptime = 216694154L,
        down = true,
        position = createPxPosition(215.42857360839844f, 378.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694162L,
        down = true,
        position = createPxPosition(217.42857360839844f, 371.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694169L,
        down = true,
        position = createPxPosition(219.42857360839844f, 366f)
    ),
    PointerInputData(
        uptime = 216694177L,
        down = true,
        position = createPxPosition(221.42857360839844f, 360.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694185L,
        down = true,
        position = createPxPosition(223.42857360839844f, 356.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694193L,
        down = true,
        position = createPxPosition(225.14285278320312f, 352.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694201L,
        down = true,
        position = createPxPosition(226.85714721679688f, 348.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694209L,
        down = true,
        position = createPxPosition(228.2857208251953f, 346f)
    ),
    PointerInputData(
        uptime = 216694216L,
        down = true,
        position = createPxPosition(229.14285278320312f, 343.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694224L,
        down = true,
        position = createPxPosition(230f, 342f)
    ),
    PointerInputData(
        uptime = 216694232L,
        down = true,
        position = createPxPosition(230.57142639160156f, 340.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694239L,
        down = true,
        position = createPxPosition(230.85714721679688f, 339.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694247L,
        down = true,
        position = createPxPosition(230.85714721679688f, 339.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694262L,
        down = true,
        position = createPxPosition(230.2857208251953f, 342f)
    ),
    PointerInputData(
        uptime = 216694270L,
        down = true,
        position = createPxPosition(228.85714721679688f, 346.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694278L,
        down = true,
        position = createPxPosition(227.14285278320312f, 352.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694286L,
        down = true,
        position = createPxPosition(225.42857360839844f, 359.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694294L,
        down = true,
        position = createPxPosition(223.7142791748047f, 367.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694301L,
        down = true,
        position = createPxPosition(222.57142639160156f, 376f)
    ),
    PointerInputData(
        uptime = 216694309L,
        down = true,
        position = createPxPosition(221.42857360839844f, 384.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694317L,
        down = true,
        position = createPxPosition(220.85714721679688f, 392.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694324L,
        down = true,
        position = createPxPosition(220f, 400.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694332L,
        down = true,
        position = createPxPosition(219.14285278320312f, 409.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694339L,
        down = true,
        position = createPxPosition(218.85714721679688f, 419.1428527832031f)
    ),
    PointerInputData(
        uptime = 216694348L,
        down = true,
        position = createPxPosition(218.2857208251953f, 428.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694356L,
        down = true,
        position = createPxPosition(218.2857208251953f, 438.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694363L,
        down = true,
        position = createPxPosition(218.2857208251953f, 447.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694371L,
        down = true,
        position = createPxPosition(218.2857208251953f, 455.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694379L,
        down = true,
        position = createPxPosition(219.14285278320312f, 462.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694386L,
        down = true,
        position = createPxPosition(220f, 469.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694394L,
        down = true,
        position = createPxPosition(221.14285278320312f, 475.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694401L,
        down = true,
        position = createPxPosition(222f, 480.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694409L,
        down = true,
        position = createPxPosition(222.85714721679688f, 485.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694417L,
        down = true,
        position = createPxPosition(224f, 489.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694425L,
        down = true,
        position = createPxPosition(224.85714721679688f, 492.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694433L,
        down = true,
        position = createPxPosition(225.42857360839844f, 495.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694440L,
        down = true,
        position = createPxPosition(226f, 497.1428527832031f)
    ),
    PointerInputData(
        uptime = 216694448L,
        down = true,
        position = createPxPosition(226.2857208251953f, 498.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694456L,
        down = true,
        position = createPxPosition(226.2857208251953f, 498.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694471L,
        down = true,
        position = createPxPosition(226.2857208251953f, 498.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694479L,
        down = true,
        position = createPxPosition(226.2857208251953f, 496.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694486L,
        down = true,
        position = createPxPosition(226.2857208251953f, 493.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694494L,
        down = true,
        position = createPxPosition(226.2857208251953f, 490f)
    ),
    PointerInputData(
        uptime = 216694502L,
        down = true,
        position = createPxPosition(226.2857208251953f, 486f)
    ),
    PointerInputData(
        uptime = 216694510L,
        down = true,
        position = createPxPosition(226.2857208251953f, 480.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694518L,
        down = true,
        position = createPxPosition(226.2857208251953f, 475.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694525L,
        down = true,
        position = createPxPosition(226.2857208251953f, 468.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694533L,
        down = true,
        position = createPxPosition(226.2857208251953f, 461.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694541L,
        down = true,
        position = createPxPosition(226.2857208251953f, 452.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694548L,
        down = true,
        position = createPxPosition(226.57142639160156f, 442.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694556L,
        down = true,
        position = createPxPosition(226.57142639160156f, 432.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694564L,
        down = true,
        position = createPxPosition(226.85714721679688f, 423.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694571L,
        down = true,
        position = createPxPosition(227.42857360839844f, 416f)
    ),
    PointerInputData(
        uptime = 216694580L,
        down = true,
        position = createPxPosition(227.7142791748047f, 410f)
    ),
    PointerInputData(
        uptime = 216694587L,
        down = true,
        position = createPxPosition(228.2857208251953f, 404.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694595L,
        down = true,
        position = createPxPosition(228.85714721679688f, 399.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694603L,
        down = true,
        position = createPxPosition(229.14285278320312f, 395.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694610L,
        down = true,
        position = createPxPosition(229.42857360839844f, 392.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694618L,
        down = true,
        position = createPxPosition(229.7142791748047f, 390f)
    ),
    PointerInputData(
        uptime = 216694625L,
        down = true,
        position = createPxPosition(229.7142791748047f, 388f)
    ),
    PointerInputData(
        uptime = 216694633L,
        down = true,
        position = createPxPosition(229.7142791748047f, 386.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694641L,
        down = true,
        position = createPxPosition(229.7142791748047f, 386.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694648L,
        down = true,
        position = createPxPosition(229.7142791748047f, 386f)
    ),
    PointerInputData(
        uptime = 216694657L,
        down = true,
        position = createPxPosition(228.85714721679688f, 386f)
    ),
    PointerInputData(
        uptime = 216694665L,
        down = true,
        position = createPxPosition(228f, 388f)
    ),
    PointerInputData(
        uptime = 216694672L,
        down = true,
        position = createPxPosition(226f, 392.5714416503906f)
    ),
    PointerInputData(
        uptime = 216694680L,
        down = true,
        position = createPxPosition(224f, 397.71429443359375f)
    ),
    PointerInputData(
        uptime = 216694688L,
        down = true,
        position = createPxPosition(222f, 404.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694695L,
        down = true,
        position = createPxPosition(219.7142791748047f, 411.1428527832031f)
    ),
    PointerInputData(
        uptime = 216694703L,
        down = true,
        position = createPxPosition(218.2857208251953f, 418f)
    ),
    PointerInputData(
        uptime = 216694710L,
        down = true,
        position = createPxPosition(217.14285278320312f, 425.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694718L,
        down = true,
        position = createPxPosition(215.7142791748047f, 433.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694726L,
        down = true,
        position = createPxPosition(214.85714721679688f, 442.28570556640625f)
    ),
    PointerInputData(
        uptime = 216694734L,
        down = true,
        position = createPxPosition(214f, 454f)
    ),
    PointerInputData(
        uptime = 216694742L,
        down = true,
        position = createPxPosition(214f, 469.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694749L,
        down = true,
        position = createPxPosition(215.42857360839844f, 485.4285583496094f)
    ),
    PointerInputData(
        uptime = 216694757L,
        down = true,
        position = createPxPosition(217.7142791748047f, 502.8571472167969f)
    ),
    PointerInputData(
        uptime = 216694765L,
        down = true,
        position = createPxPosition(221.14285278320312f, 521.4285888671875f)
    ),
    PointerInputData(
        uptime = 216694772L,
        down = true,
        position = createPxPosition(224.57142639160156f, 541.1428833007812f)
    ),
    PointerInputData(
        uptime = 216694780L,
        down = true,
        position = createPxPosition(229.14285278320312f, 561.1428833007812f)
    ),
    PointerInputData(
        uptime = 216694788L,
        down = true,
        position = createPxPosition(233.42857360839844f, 578.8571166992188f)
    ),
    PointerInputData(
        uptime = 216694802L,
        down = false,
        position = createPxPosition(233.42857360839844f, 578.8571166992188f)
    ),
    PointerInputData(
        uptime = 216695344L,
        down = true,
        position = createPxPosition(253.42857360839844f, 310.5714416503906f)
    ),
    PointerInputData(
        uptime = 216695352L,
        down = true,
        position = createPxPosition(253.42857360839844f, 310.5714416503906f)
    ),
    PointerInputData(
        uptime = 216695359L,
        down = true,
        position = createPxPosition(252.85714721679688f, 318f)
    ),
    PointerInputData(
        uptime = 216695367L,
        down = true,
        position = createPxPosition(251.14285278320312f, 322f)
    ),
    PointerInputData(
        uptime = 216695375L,
        down = true,
        position = createPxPosition(248.85714721679688f, 327.1428527832031f)
    ),
    PointerInputData(
        uptime = 216695382L,
        down = true,
        position = createPxPosition(246f, 334.8571472167969f)
    ),
    PointerInputData(
        uptime = 216695390L,
        down = true,
        position = createPxPosition(242.57142639160156f, 344.5714416503906f)
    ),
    PointerInputData(
        uptime = 216695397L,
        down = true,
        position = createPxPosition(238.85714721679688f, 357.4285583496094f)
    ),
    PointerInputData(
        uptime = 216695406L,
        down = true,
        position = createPxPosition(235.7142791748047f, 371.71429443359375f)
    ),
    PointerInputData(
        uptime = 216695414L,
        down = true,
        position = createPxPosition(232.2857208251953f, 386.8571472167969f)
    ),
    PointerInputData(
        uptime = 216695421L,
        down = true,
        position = createPxPosition(229.42857360839844f, 402f)
    ),
    PointerInputData(
        uptime = 216695429L,
        down = true,
        position = createPxPosition(227.42857360839844f, 416.8571472167969f)
    ),
    PointerInputData(
        uptime = 216695437L,
        down = true,
        position = createPxPosition(226.2857208251953f, 431.4285583496094f)
    ),
    PointerInputData(
        uptime = 216695444L,
        down = true,
        position = createPxPosition(226.2857208251953f, 446f)
    ),
    PointerInputData(
        uptime = 216695452L,
        down = true,
        position = createPxPosition(227.7142791748047f, 460.28570556640625f)
    ),
    PointerInputData(
        uptime = 216695459L,
        down = true,
        position = createPxPosition(230f, 475.1428527832031f)
    ),
    PointerInputData(
        uptime = 216695467L,
        down = true,
        position = createPxPosition(232.2857208251953f, 489.71429443359375f)
    ),
    PointerInputData(
        uptime = 216695475L,
        down = true,
        position = createPxPosition(235.7142791748047f, 504f)
    ),
    PointerInputData(
        uptime = 216695490L,
        down = false,
        position = createPxPosition(235.7142791748047f, 504f)
    ),
    PointerInputData(
        uptime = 216695885L,
        down = true,
        position = createPxPosition(238.85714721679688f, 524f)
    ),
    PointerInputData(
        uptime = 216695908L,
        down = true,
        position = createPxPosition(236.2857208251953f, 515.7142944335938f)
    ),
    PointerInputData(
        uptime = 216695916L,
        down = true,
        position = createPxPosition(234.85714721679688f, 509.1428527832031f)
    ),
    PointerInputData(
        uptime = 216695924L,
        down = true,
        position = createPxPosition(232.57142639160156f, 498.5714416503906f)
    ),
    PointerInputData(
        uptime = 216695931L,
        down = true,
        position = createPxPosition(230.57142639160156f, 483.71429443359375f)
    ),
    PointerInputData(
        uptime = 216695939L,
        down = true,
        position = createPxPosition(229.14285278320312f, 466.5714416503906f)
    ),
    PointerInputData(
        uptime = 216695947L,
        down = true,
        position = createPxPosition(229.14285278320312f, 446.5714416503906f)
    ),
    PointerInputData(
        uptime = 216695955L,
        down = true,
        position = createPxPosition(230.57142639160156f, 424.8571472167969f)
    ),
    PointerInputData(
        uptime = 216695963L,
        down = true,
        position = createPxPosition(232.57142639160156f, 402.28570556640625f)
    ),
    PointerInputData(
        uptime = 216695970L,
        down = true,
        position = createPxPosition(235.14285278320312f, 380f)
    ),
    PointerInputData(
        uptime = 216695978L,
        down = true,
        position = createPxPosition(238.57142639160156f, 359.4285583496094f)
    ),
    PointerInputData(
        uptime = 216695993L,
        down = false,
        position = createPxPosition(238.57142639160156f, 359.4285583496094f)
    ),
    PointerInputData(
        uptime = 216696429L,
        down = true,
        position = createPxPosition(238.2857208251953f, 568.5714111328125f)
    ),
    PointerInputData(
        uptime = 216696459L,
        down = true,
        position = createPxPosition(234f, 560f)
    ),
    PointerInputData(
        uptime = 216696467L,
        down = true,
        position = createPxPosition(231.42857360839844f, 553.1428833007812f)
    ),
    PointerInputData(
        uptime = 216696475L,
        down = true,
        position = createPxPosition(228.2857208251953f, 543.1428833007812f)
    ),
    PointerInputData(
        uptime = 216696483L,
        down = true,
        position = createPxPosition(225.42857360839844f, 528.8571166992188f)
    ),
    PointerInputData(
        uptime = 216696491L,
        down = true,
        position = createPxPosition(223.14285278320312f, 512.2857055664062f)
    ),
    PointerInputData(
        uptime = 216696498L,
        down = true,
        position = createPxPosition(222f, 495.4285583496094f)
    ),
    PointerInputData(
        uptime = 216696506L,
        down = true,
        position = createPxPosition(221.7142791748047f, 477.4285583496094f)
    ),
    PointerInputData(
        uptime = 216696514L,
        down = true,
        position = createPxPosition(221.7142791748047f, 458.28570556640625f)
    ),
    PointerInputData(
        uptime = 216696521L,
        down = true,
        position = createPxPosition(223.14285278320312f, 438f)
    ),
    PointerInputData(
        uptime = 216696529L,
        down = true,
        position = createPxPosition(224.2857208251953f, 416.28570556640625f)
    ),
    PointerInputData(
        uptime = 216696544L,
        down = false,
        position = createPxPosition(224.2857208251953f, 416.28570556640625f)
    ),
    PointerInputData(
        uptime = 216696974L,
        down = true,
        position = createPxPosition(218.57142639160156f, 530.5714111328125f)
    ),
    PointerInputData(
        uptime = 216697012L,
        down = true,
        position = createPxPosition(220.2857208251953f, 522f)
    ),
    PointerInputData(
        uptime = 216697020L,
        down = true,
        position = createPxPosition(221.14285278320312f, 517.7142944335938f)
    ),
    PointerInputData(
        uptime = 216697028L,
        down = true,
        position = createPxPosition(222.2857208251953f, 511.71429443359375f)
    ),
    PointerInputData(
        uptime = 216697036L,
        down = true,
        position = createPxPosition(224f, 504.28570556640625f)
    ),
    PointerInputData(
        uptime = 216697044L,
        down = true,
        position = createPxPosition(227.14285278320312f, 490.5714416503906f)
    ),
    PointerInputData(
        uptime = 216697052L,
        down = true,
        position = createPxPosition(229.42857360839844f, 474f)
    ),
    PointerInputData(
        uptime = 216697059L,
        down = true,
        position = createPxPosition(231.42857360839844f, 454.5714416503906f)
    ),
    PointerInputData(
        uptime = 216697067L,
        down = true,
        position = createPxPosition(233.7142791748047f, 431.1428527832031f)
    ),
    PointerInputData(
        uptime = 216697082L,
        down = false,
        position = createPxPosition(233.7142791748047f, 431.1428527832031f)
    ),
    PointerInputData(
        uptime = 216697435L,
        down = true,
        position = createPxPosition(257.1428527832031f, 285.1428527832031f)
    ),
    PointerInputData(
        uptime = 216697465L,
        down = true,
        position = createPxPosition(251.7142791748047f, 296.8571472167969f)
    ),
    PointerInputData(
        uptime = 216697473L,
        down = true,
        position = createPxPosition(248.2857208251953f, 304f)
    ),
    PointerInputData(
        uptime = 216697481L,
        down = true,
        position = createPxPosition(244.57142639160156f, 314.8571472167969f)
    ),
    PointerInputData(
        uptime = 216697489L,
        down = true,
        position = createPxPosition(240.2857208251953f, 329.1428527832031f)
    ),
    PointerInputData(
        uptime = 216697497L,
        down = true,
        position = createPxPosition(236.85714721679688f, 345.1428527832031f)
    ),
    PointerInputData(
        uptime = 216697505L,
        down = true,
        position = createPxPosition(233.7142791748047f, 361.4285583496094f)
    ),
    PointerInputData(
        uptime = 216697512L,
        down = true,
        position = createPxPosition(231.14285278320312f, 378.28570556640625f)
    ),
    PointerInputData(
        uptime = 216697520L,
        down = true,
        position = createPxPosition(229.42857360839844f, 395.4285583496094f)
    ),
    PointerInputData(
        uptime = 216697528L,
        down = true,
        position = createPxPosition(229.42857360839844f, 412.8571472167969f)
    ),
    PointerInputData(
        uptime = 216697535L,
        down = true,
        position = createPxPosition(230.85714721679688f, 430.8571472167969f)
    ),
    PointerInputData(
        uptime = 216697543L,
        down = true,
        position = createPxPosition(233.42857360839844f, 449.71429443359375f)
    ),
    PointerInputData(
        uptime = 216697558L,
        down = false,
        position = createPxPosition(233.42857360839844f, 449.71429443359375f)
    ),
    PointerInputData(
        uptime = 216697749L,
        down = true,
        position = createPxPosition(246f, 311.4285583496094f)
    ),
    PointerInputData(
        uptime = 216697780L,
        down = true,
        position = createPxPosition(244.57142639160156f, 318.28570556640625f)
    ),
    PointerInputData(
        uptime = 216697787L,
        down = true,
        position = createPxPosition(243.14285278320312f, 325.4285583496094f)
    ),
    PointerInputData(
        uptime = 216697795L,
        down = true,
        position = createPxPosition(241.42857360839844f, 336f)
    ),
    PointerInputData(
        uptime = 216697803L,
        down = true,
        position = createPxPosition(239.7142791748047f, 351.1428527832031f)
    ),
    PointerInputData(
        uptime = 216697811L,
        down = true,
        position = createPxPosition(238.2857208251953f, 368.5714416503906f)
    ),
    PointerInputData(
        uptime = 216697819L,
        down = true,
        position = createPxPosition(238f, 389.4285583496094f)
    ),
    PointerInputData(
        uptime = 216697826L,
        down = true,
        position = createPxPosition(239.14285278320312f, 412f)
    ),
    PointerInputData(
        uptime = 216697834L,
        down = true,
        position = createPxPosition(242.2857208251953f, 438f)
    ),
    PointerInputData(
        uptime = 216697842L,
        down = true,
        position = createPxPosition(247.42857360839844f, 466.8571472167969f)
    ),
    PointerInputData(
        uptime = 216697849L,
        down = true,
        position = createPxPosition(254.2857208251953f, 497.71429443359375f)
    ),
    PointerInputData(
        uptime = 216697864L,
        down = false,
        position = createPxPosition(254.2857208251953f, 497.71429443359375f)
    ),
    PointerInputData(
        uptime = 216698321L,
        down = true,
        position = createPxPosition(250f, 306f)
    ),
    PointerInputData(
        uptime = 216698328L,
        down = true,
        position = createPxPosition(250f, 306f)
    ),
    PointerInputData(
        uptime = 216698344L,
        down = true,
        position = createPxPosition(249.14285278320312f, 314f)
    ),
    PointerInputData(
        uptime = 216698351L,
        down = true,
        position = createPxPosition(247.42857360839844f, 319.4285583496094f)
    ),
    PointerInputData(
        uptime = 216698359L,
        down = true,
        position = createPxPosition(245.14285278320312f, 326.8571472167969f)
    ),
    PointerInputData(
        uptime = 216698366L,
        down = true,
        position = createPxPosition(241.7142791748047f, 339.4285583496094f)
    ),
    PointerInputData(
        uptime = 216698374L,
        down = true,
        position = createPxPosition(238.57142639160156f, 355.71429443359375f)
    ),
    PointerInputData(
        uptime = 216698382L,
        down = true,
        position = createPxPosition(236.2857208251953f, 374.28570556640625f)
    ),
    PointerInputData(
        uptime = 216698390L,
        down = true,
        position = createPxPosition(235.14285278320312f, 396.5714416503906f)
    ),
    PointerInputData(
        uptime = 216698398L,
        down = true,
        position = createPxPosition(236.57142639160156f, 421.4285583496094f)
    ),
    PointerInputData(
        uptime = 216698406L,
        down = true,
        position = createPxPosition(241.14285278320312f, 451.4285583496094f)
    ),
    PointerInputData(
        uptime = 216698421L,
        down = false,
        position = createPxPosition(241.14285278320312f, 451.4285583496094f)
    )
)

internal val interruptedVelocityEventData: List<PointerInputData> = listOf(
    PointerInputData(
        uptime = 216698321L,
        down = true,
        position = createPxPosition(250f, 306f)
    ),
    PointerInputData(
        uptime = 216698328L,
        down = true,
        position = createPxPosition(250f, 306f)
    ),
    PointerInputData(
        uptime = 216698344L,
        down = true,
        position = createPxPosition(249.14285278320312f, 314f)
    ),
    PointerInputData(
        uptime = 216698351L,
        down = true,
        position = createPxPosition(247.42857360839844f, 319.4285583496094f)
    ),
    PointerInputData(
        uptime = 216698359L,
        down = true,
        position = createPxPosition(245.14285278320312f, 326.8571472167969f)
    ),
    PointerInputData(
        uptime = 216698366L,
        down = true,
        position = createPxPosition(241.7142791748047f, 339.4285583496094f)
    ),

// The pointer "stops" here because we've introduced a 40+ms gap
// in the move event stream. See kAssumePointerMoveStoppedMilliseconds
// in velocity_tracker.dart.

    PointerInputData(
        uptime = (216698374L + 40),
        down = true,
        position = createPxPosition(238.57142639160156f, 355.71429443359375f)
    ),
    PointerInputData(
        uptime = (216698382L + 40),
        down = true,
        position = createPxPosition(236.2857208251953f, 374.28570556640625f)
    ),
    PointerInputData(
        uptime = (216698390L + 40),
        down = true,
        position = createPxPosition(235.14285278320312f, 396.5714416503906f)
    ),
    PointerInputData(
        uptime = (216698398L + 40),
        down = true,
        position = createPxPosition(236.57142639160156f, 421.4285583496094f)
    ),
    PointerInputData(
        uptime = (216698406L + 40),
        down = true,
        position = createPxPosition(241.14285278320312f, 451.4285583496094f)
    ),
    PointerInputData(
        uptime = (216698421L + 40),
        down = false,
        position = createPxPosition(241.14285278320312f, 451.4285583496094f)
    )
)