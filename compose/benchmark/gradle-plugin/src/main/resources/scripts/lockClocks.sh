#
# Copyright (C) 2018 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# This script can be used to lock device clocks to stable levels for comparing
# different versions of software.  Since the clock levels are not necessarily
# indicative of real world behavior, this should **never** be used to compare
# performance between different device models.

# Fun notes for maintaining this file:
#      $((arithmetic expressions)) can deal with ints > INT32_MAX, but if compares cannot. This is
#      why we use MHz.
#      $((arithmetic expressions)) can sometimes evaluate right-to-left. This is why we use parens.
#      Everything below the initial host-check isn't bash - Android uses mksh
#      mksh allows `\n` in an echo, bash doesn't
#      can't use `awk`
#      can't use `sed`
#      can't use `cut` on < L
#      can't use `expr` on < L

ARG_CORES=${1:-big}

CPU_TARGET_FREQ_PERCENT=50
GPU_TARGET_FREQ_PERCENT=50

if [ "`command -v getprop`" == "" ]; then
    if [ -n "`command -v adb`" ]; then
        echo ""
        echo "Pushing $0 and running it on device..."
        dest=/data/local/tmp/`basename $0`
        adb push $0 ${dest}
        adb shell ${dest} $@
        adb shell rm ${dest}
        exit
    else
        echo "Could not find adb. Options are:"
        echo "  1. Ensure adb is on your \$PATH"
        echo "  2. Use './gradlew lockClocks'"
        echo "  3. Manually adb push this script to your device, and run it there"
        exit -1
    fi
fi

# require root
if [[ `id` != "uid=0"* ]]; then
    echo "Not running as root, cannot lock clocks, aborting"
    exit -1
fi

DEVICE=`getprop ro.product.device`
MODEL=`getprop ro.product.model`

if [ "$ARG_CORES" == "big" ]; then
    CPU_IDEAL_START_FREQ_KHZ=0
elif [ "$ARG_CORES" == "little" ]; then
    CPU_IDEAL_START_FREQ_KHZ=100000000 ## finding min of max freqs, so start at 100M KHz (100 GHz)
else
    echo "Invalid argument \$1 for ARG_CORES, should be 'big' or 'little', but was $ARG_CORES"
    exit -1
fi

function_core_check() {
    if [ "$ARG_CORES" == "big" ]; then
        [ $1 -gt $2 ]
    elif [ "$ARG_CORES" == "little" ]; then
        [ $1 -lt $2 ]
    else
        echo "Invalid argument \$1 for ARG_CORES, should be 'big' or 'little', but was $ARG_CORES"
        exit -1
    fi
}

function_setup_go() {
    if [ -f /d/fpsgo/common/force_onoff ]; then
        # Disable fpsgo
        echo 0 > /d/fpsgo/common/force_onoff
        fpsgoState=`cat /d/fpsgo/common/force_onoff`
        if [ "$fpsgoState" != "0" ] && [ "$fpsgoState" != "force off" ]; then
            echo "Failed to disable fpsgo"
            exit -1
        fi
    fi
}

# Disable CPU hotpluging by killing mpdecision service via ctl.stop system property.
# This helper checks the state and existence of the mpdecision service via init.svc.
# Possible values from init.svc: "stopped", "stopping", "running", "restarting"
function_stop_mpdecision() {
    MPDECISION_STATUS=`getprop init.svc.mpdecision`
    while [ "$MPDECISION_STATUS" == "running" ] || [ "$MPDECISION_STATUS" == "restarting" ]; do
        setprop ctl.stop mpdecision
        # Give initrc some time to kill the mpdecision service.
        sleep 0.1
        MPDECISION_STATUS=`getprop init.svc.mpdecision`
    done
}

# Find the min or max (little vs big) of CPU max frequency, and lock cores of the selected type to
# an available frequency that's >= $CPU_TARGET_FREQ_PERCENT% of max. Disable other cores.
function_lock_cpu() {
    CPU_BASE=/sys/devices/system/cpu
    GOV=cpufreq/scaling_governor

    # Options to make clock locking on go devices more sticky.
    function_setup_go

    # Find max CPU freq, and associated list of available freqs
    cpuIdealFreq=$CPU_IDEAL_START_FREQ_KHZ
    cpuAvailFreqCmpr=0
    cpuAvailFreq=0
    enableIndices=''
    disableIndices=''
    cpu=0

    # Stop mpdecision (CPU hotplug service) if it exists. Not available on all devices.
    function_stop_mpdecision

    # Loop through all available cores; We have to check by the parent folder
    # "cpu#" instead of cpu#/online or cpu#/cpufreq directly, since they may
    # not be accessible yet.
    while [ -d ${CPU_BASE}/cpu${cpu} ]; do

        # Try to enable core, so we can find its frequencies.
        # Note: In cases where the online file is inaccessible, it represents a
        # core which cannot be turned off, so we simply assume it is enabled if
        # this command fails.
        if [ -f "$CPU_BASE/cpu$cpu/online" ]; then
            echo 1 > ${CPU_BASE}/cpu${cpu}/online || true
        fi

        # set userspace governor on all CPUs to ensure freq scaling is disabled
        echo userspace > ${CPU_BASE}/cpu${cpu}/${GOV}

        maxFreq=`cat ${CPU_BASE}/cpu$cpu/cpufreq/cpuinfo_max_freq`
        availFreq=`cat ${CPU_BASE}/cpu$cpu/cpufreq/scaling_available_frequencies`
        availFreqCmpr=${availFreq// /-}

        if (function_core_check $maxFreq $cpuIdealFreq); then
            # new min/max of max freq, look for cpus with same max freq and same avail freq list
            cpuIdealFreq=${maxFreq}
            cpuAvailFreq=${availFreq}
            cpuAvailFreqCmpr=${availFreqCmpr}

            if [ -z "$disableIndices" ]; then
                disableIndices="$enableIndices"
            else
                disableIndices="$disableIndices $enableIndices"
            fi
            enableIndices=${cpu}
        elif [ ${maxFreq} == ${cpuIdealFreq} ] && [ ${availFreqCmpr} == ${cpuAvailFreqCmpr} ]; then
            enableIndices="$enableIndices $cpu"
        else
            if [ -z "$disableIndices" ]; then
                disableIndices="$cpu"
            else
                disableIndices="$disableIndices $cpu"
            fi
        fi

        cpu=$(($cpu + 1))
    done

    # check that some CPUs will be enabled
    if [ -z "$enableIndices" ]; then
        echo "Failed to find any $ARG_CORES cores to enable, aborting."
        exit -1
    fi

    # Chose a frequency to lock to that's >= $CPU_TARGET_FREQ_PERCENT% of max
    # (below, 100M = 1K for KHz->MHz * 100 for %)
    TARGET_FREQ_MHZ=$(( ($cpuIdealFreq * $CPU_TARGET_FREQ_PERCENT) / 100000 ))
    chosenFreq=0
    chosenFreqDiff=100000000
    for freq in ${cpuAvailFreq}; do
        freqMhz=$(( ${freq} / 1000 ))
        if [ ${freqMhz} -ge ${TARGET_FREQ_MHZ} ]; then
            newChosenFreqDiff=$(( $freq - $TARGET_FREQ_MHZ ))
            if [ $newChosenFreqDiff -lt $chosenFreqDiff ]; then
                chosenFreq=${freq}
                chosenFreqDiff=$(( $chosenFreq - $TARGET_FREQ_MHZ ))
            fi
        fi
    done

    # Lock wembley clocks using high-priority op code method.
    # This block depends on the shell utility awk, which is only available on API 27+
    if [ "$DEVICE" == "wembley" ]; then
        # Get list of available frequencies to lock to by parsing the op-code list.
        AVAIL_OP_FREQS=`cat /proc/cpufreq/MT_CPU_DVFS_LL/cpufreq_oppidx \
            | awk '{print $2}' \
            | tail -n +3 \
            | while read line; do
                echo "${line:1:${#line}-2}"
            done`

        # Compute the closest available frequency to the desired frequency, $chosenFreq.
        # This assumes the op codes listen in /proc/cpufreq/MT_CPU_DVFS_LL/cpufreq_oppidx are listed
        # in order and 0-indexed.
        opCode=-1
        opFreq=0
        currOpCode=-1
        for currOpFreq in $AVAIL_OP_FREQS; do
            currOpCode=$((currOpCode + 1))

            prevDiff=$((chosenFreq-opFreq))
            prevDiff=`function_abs $prevDiff`
            currDiff=$((chosenFreq-currOpFreq))
            currDiff=`function_abs $currDiff`
            if [ $currDiff -lt $prevDiff ]; then
                opCode="$currOpCode"
                opFreq="$currOpFreq"
            fi
        done

        echo "$opCode" > /proc/ppm/policy/ut_fix_freq_idx
    fi

    # enable 'big' CPUs
    for cpu in ${enableIndices}; do
        freq=${CPU_BASE}/cpu$cpu/cpufreq

        # Try to enable core, so we can find its frequencies.
        # Note: In cases where the online file is inaccessible, it represents a
        # core which cannot be turned off, so we simply assume it is enabled if
        # this command fails.
        if [ -f "$CPU_BASE/cpu$cpu/online" ]; then
            echo 1 > ${CPU_BASE}/cpu${cpu}/online || true
        fi

        # scaling_max_freq must be set before scaling_min_freq
        echo ${chosenFreq} > ${freq}/scaling_max_freq
        echo ${chosenFreq} > ${freq}/scaling_min_freq
        echo ${chosenFreq} > ${freq}/scaling_setspeed

        # Give system a bit of time to propagate the change to scaling_setspeed.
        sleep 0.1

        # validate setting the freq worked
        obsCur=`cat ${freq}/scaling_cur_freq`
        obsMin=`cat ${freq}/scaling_min_freq`
        obsMax=`cat ${freq}/scaling_max_freq`
        if [ "$obsCur" -ne "$chosenFreq" ] || [ "$obsMin" -ne "$chosenFreq" ] || [ "$obsMax" -ne "$chosenFreq" ]; then
            echo "Failed to set CPU$cpu to $chosenFreq Hz! Aborting..."
            echo "scaling_cur_freq = $obsCur"
            echo "scaling_min_freq = $obsMin"
            echo "scaling_max_freq = $obsMax"
            exit -1
        fi
    done

    # disable other CPUs (Note: important to enable big cores first!)
    for cpu in ${disableIndices}; do
      echo 0 > ${CPU_BASE}/cpu${cpu}/online
    done

    echo "=================================="
    echo "Locked CPUs ${enableIndices// /,} to $chosenFreq / $cpuIdealFreq KHz"
    echo "Disabled CPUs ${disableIndices// /,}"
    echo "=================================="
}

# Returns the absolute value of the first arg passed to this helper.
function_abs() {
    n=$1
    if [ $n -lt 0 ]; then
        echo "$((n * -1 ))"
    else
        echo "$n"
    fi
}

# If we have a Qualcomm GPU, find its max frequency, and lock to
# an available frequency that's >= GPU_TARGET_FREQ_PERCENT% of max.
function_lock_gpu_kgsl() {
    if [ ! -d /sys/class/kgsl/kgsl-3d0/ ]; then
        # not kgsl, abort
        echo "Currently don't support locking GPU clocks of $MODEL ($DEVICE)"
        return -1
    fi
    if [ ${DEVICE} == "walleye" ] || [ ${DEVICE} == "taimen" ]; then
        # Workaround crash
        echo "Unable to lock GPU clocks of $MODEL ($DEVICE)"
        return -1
    fi

    GPU_BASE=/sys/class/kgsl/kgsl-3d0

    gpuMaxFreq=0
    gpuAvailFreq=`cat $GPU_BASE/devfreq/available_frequencies`
    for freq in ${gpuAvailFreq}; do
        if [ ${freq} -gt ${gpuMaxFreq} ]; then
            gpuMaxFreq=${freq}
        fi
    done

    # (below, 100M = 1M for MHz * 100 for %)
    TARGET_FREQ_MHZ=$(( (${gpuMaxFreq} * ${GPU_TARGET_FREQ_PERCENT}) / 100000000 ))

    chosenFreq=${gpuMaxFreq}
    index=0
    chosenIndex=0
    for freq in ${gpuAvailFreq}; do
        freqMhz=$(( ${freq} / 1000000 ))
        if [ ${freqMhz} -ge ${TARGET_FREQ_MHZ} ] && [ ${chosenFreq} -ge ${freq} ]; then
            # note avail freq are generally in reverse order, so we don't break out of this loop
            chosenFreq=${freq}
            chosenIndex=${index}
        fi
        index=$(($index + 1))
    done
    lastIndex=$(($index - 1))

    firstFreq=`function_cut_first_from_space_seperated_list $gpuAvailFreq`

    if [ ${gpuMaxFreq} != ${firstFreq} ]; then
        # pwrlevel is index of desired freq among available frequencies, from highest to lowest.
        # If gpuAvailFreq appears to be in-order, reverse the index
        chosenIndex=$(($lastIndex - $chosenIndex))
    fi

    echo 0 > ${GPU_BASE}/bus_split
    echo 1 > ${GPU_BASE}/force_clk_on
    echo 10000 > ${GPU_BASE}/idle_timer

    echo performance > ${GPU_BASE}/devfreq/governor

    # NOTE: we store in min/max twice, because we don't know if we're increasing
    # or decreasing, and it's invalid to try and set min > max, or max < min
    echo ${chosenFreq} > ${GPU_BASE}/devfreq/min_freq
    echo ${chosenFreq} > ${GPU_BASE}/devfreq/max_freq
    echo ${chosenFreq} > ${GPU_BASE}/devfreq/min_freq
    echo ${chosenFreq} > ${GPU_BASE}/devfreq/max_freq
    echo ${chosenIndex} > ${GPU_BASE}/min_pwrlevel
    echo ${chosenIndex} > ${GPU_BASE}/max_pwrlevel
    echo ${chosenIndex} > ${GPU_BASE}/min_pwrlevel
    echo ${chosenIndex} > ${GPU_BASE}/max_pwrlevel

    obsCur=`cat ${GPU_BASE}/devfreq/cur_freq`
    obsMin=`cat ${GPU_BASE}/devfreq/min_freq`
    obsMax=`cat ${GPU_BASE}/devfreq/max_freq`
    if [ obsCur -ne ${chosenFreq} ] || [ obsMin -ne ${chosenFreq} ] || [ obsMax -ne ${chosenFreq} ]; then
        echo "Failed to set GPU to $chosenFreq Hz! Aborting..."
        echo "cur_freq = $obsCur"
        echo "min_freq = $obsMin"
        echo "max_freq = $obsMax"
        echo "index = $chosenIndex"
        exit -1
    fi
    echo "Locked GPU to $chosenFreq / $gpuMaxFreq Hz"
}

# cut is not available on some devices (Nexus 5 running LRX22C).
function_cut_first_from_space_seperated_list() {
    list=$1

    for freq in $list; do
        echo $freq
        break
    done
}

# kill processes that manage thermals / scaling
stop thermal-engine || true
stop perfd || true
stop vendor.thermal-engine || true
stop vendor.perfd || true
setprop vendor.powerhal.init 0 || true
setprop ctl.interface_restart android.hardware.power@1.0::IPower/default || true

function_lock_cpu

if [ "$DEVICE" -ne "wembley" ]; then
    function_lock_gpu_kgsl
else
    echo "Unable to lock gpu clocks of $MODEL ($DEVICE)."
fi

# Memory bus - hardcoded per-device for now
if [ ${DEVICE} == "marlin" ] || [ ${DEVICE} == "sailfish" ]; then
    echo 13763 > /sys/class/devfreq/soc:qcom,gpubw/max_freq
else
    echo "Unable to lock memory bus of $MODEL ($DEVICE)."
fi

echo "$DEVICE clocks have been locked - to reset, reboot the device"
