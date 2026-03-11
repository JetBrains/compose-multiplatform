#!/bin/bash
#
# run_ios_benchmarks.sh
#
# Builds the iosApp, installs it on a real device or simulator, then runs
# every benchmark (from Benchmarks.kt) with parallel=true and parallel=false,
# ATTEMPTS times each.  Console output is saved to:
#
#   benchmarks_result/<device>_<ios>_parallel_<true|false>_<BenchmarkName>_<N>.txt
#
# Requirements:
#   - Xcode 15+ (uses xcrun devicectl for real devices, xcrun simctl for simulators)
#   - For real device: connected via USB and trusted, valid code-signing identity
#   - For simulator: any booted or available simulator
#
# Usage:  bash run_ios_benchmarks.sh [<device-udid>]
#
#   If no UDID is provided the first connected real device is used.
#   Pass a simulator UDID to target a simulator instead.
#

set -euo pipefail

# ── Configuration ──────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$SCRIPT_DIR"
MULTIPLATFORM_DIR="$SCRIPT_DIR/.."
OUTPUT_DIR="$MULTIPLATFORM_DIR/benchmarks_result"
SCHEME="iosApp"
CONFIGURATION="Release"
ATTEMPTS=5
BUILD_DIR="$MULTIPLATFORM_DIR/.benchmark_build"

BENCHMARKS=(
    "AnimatedVisibility"
    "LazyGrid"
    "LazyGrid-ItemLaunchedEffect"
    "LazyGrid-SmoothScroll"
    "LazyGrid-SmoothScroll-ItemLaunchedEffect"
    "VisualEffects"
    "LazyList"
    "MultipleComponents"
    "MultipleComponents-NoVectorGraphics"
    "TextLayout"
    "CanvasDrawing"
    "HeavyShader"
)

# ── Helpers ────────────────────────────────────────────────────────────────────

die() { echo ""; echo "ERROR: $*" >&2; exit 1; }

# ── 1. Detect target device or simulator ──────────────────────────────────────

echo ""
echo "==> [1/4] Detecting target..."

echo "  $ xcrun xctrace list devices"
XCTRACE_OUT=$(xcrun xctrace list devices 2>&1)

# Real device lines (between "== Devices ==" and the next "==").
# xctrace format: "Device Name (iOS Version) (UDID)"
ALL_REAL_LINES=$(awk \
    '/^== Devices ==/{p=1; next} /^== /{p=0} p && NF{print}' \
    <<< "$XCTRACE_OUT" | grep -E '\([0-9]+\.[0-9]' | grep -v " Mac " || true)

# Simulator lines (between "== Simulators ==" and the next "==").
ALL_SIM_LINES=$(awk \
    '/^== Simulators ==/{p=1; next} /^== /{p=0} p && NF{print}' \
    <<< "$XCTRACE_OUT" | grep -E '\([0-9]+\.[0-9]' || true)

ALL_DEVICE_LINES=$(printf '%s\n%s\n' "$ALL_REAL_LINES" "$ALL_SIM_LINES" | grep -v '^$' || true)

if [[ -z "$ALL_DEVICE_LINES" ]]; then
    echo "$XCTRACE_OUT"
    die "No iOS device or simulator found."
fi

# If a UDID was passed as an argument, look for that specific target; otherwise use the first real device.
ARG_UDID="${1:-}"
if [[ -n "$ARG_UDID" ]]; then
    DEVICE_LINE=$(grep "$ARG_UDID" <<< "$ALL_DEVICE_LINES" || true)
    if [[ -z "$DEVICE_LINE" ]]; then
        echo "Available devices and simulators:"
        echo "$ALL_DEVICE_LINES"
        die "Device with UDID '$ARG_UDID' not found among the above."
    fi
else
    if [[ -n "$ALL_REAL_LINES" ]]; then
        DEVICE_LINE=$(head -1 <<< "$ALL_REAL_LINES")
    else
        DEVICE_LINE=$(head -1 <<< "$ALL_SIM_LINES")
    fi
fi

# Determine if the selected target is a simulator.
IS_SIMULATOR=false
if [[ -n "$ALL_SIM_LINES" ]] && grep -qF "$DEVICE_LINE" <<< "$ALL_SIM_LINES"; then
    IS_SIMULATOR=true
fi

# Parse "Device Name (iOS Version) (UDID)"
# UDID is the last parenthesised token on the line.
DEVICE_ID=$(  grep -oE '\([0-9A-Fa-f-]+\)' <<< "$DEVICE_LINE" | tail -1 | tr -d '()')
DEVICE_IOS=$( grep -oE '\([0-9]+\.[0-9.]+\)' <<< "$DEVICE_LINE" | head -1 | tr -d '()')
DEVICE_NAME=$(sed 's/ ([0-9].*//' <<< "$DEVICE_LINE" | xargs)

# Normalize for filenames: lowercase, spaces→underscores, keep only [a-z0-9._-]
DEVICE_PREFIX=$(printf '%s_%s' "$DEVICE_NAME" "$DEVICE_IOS" \
    | tr '[:upper:]' '[:lower:]' \
    | tr ' ' '_' \
    | LC_ALL=C tr -cd 'a-z0-9._-')

echo "    Name      : $DEVICE_NAME"
echo "    iOS       : $DEVICE_IOS"
echo "    UDID      : $DEVICE_ID"
echo "    Simulator : $IS_SIMULATOR"
echo "    Prefix    : ${DEVICE_PREFIX}_parallel_<true|false>_<Benchmark>_<N>.txt"

# ── 2. Build ───────────────────────────────────────────────────────────────────

echo ""
echo "==> [2/4] Building '$SCHEME' ($CONFIGURATION)..."
echo "  $ mkdir -p $BUILD_DIR"
mkdir -p "$BUILD_DIR"

XCODE_LOG="$BUILD_DIR/xcodebuild.log"

# Clean stale Kotlin Native build artifacts to avoid klib ABI version mismatches.
echo "  $ cd $MULTIPLATFORM_DIR && ./gradlew clean"
(cd "$MULTIPLATFORM_DIR" && ./gradlew clean) 2>&1

echo "  $ xcodebuild build -project $PROJECT_DIR/iosApp.xcodeproj -scheme $SCHEME -configuration $CONFIGURATION -destination id=$DEVICE_ID ONLY_ACTIVE_ARCH=YES SYMROOT=$BUILD_DIR"
set +e
xcodebuild build \
    -project "$PROJECT_DIR/iosApp.xcodeproj" \
    -scheme "$SCHEME" \
    -configuration "$CONFIGURATION" \
    -destination "id=$DEVICE_ID" \
    ONLY_ACTIVE_ARCH=YES \
    SYMROOT="$BUILD_DIR" \
    >"$XCODE_LOG" 2>&1
BUILD_EXIT=$?
set -e

if [[ $BUILD_EXIT -ne 0 ]]; then
    echo "Build failed. Last 50 lines of xcodebuild output:"
    echo "----------------------------------------------------"
    tail -50 "$XCODE_LOG"
    echo "----------------------------------------------------"
    echo "Full log: $XCODE_LOG"
    exit 1
fi

if [[ "$IS_SIMULATOR" == "true" ]]; then
    APP_PATH="$BUILD_DIR/${CONFIGURATION}-iphonesimulator/ComposeBenchmarks.app"
else
    APP_PATH="$BUILD_DIR/${CONFIGURATION}-iphoneos/ComposeBenchmarks.app"
fi
[[ -d "$APP_PATH" ]] || die "App bundle not found at expected path: $APP_PATH"

echo "  $ /usr/libexec/PlistBuddy -c 'Print CFBundleIdentifier' $APP_PATH/Info.plist"
BUNDLE_ID=$(/usr/libexec/PlistBuddy -c "Print CFBundleIdentifier" "$APP_PATH/Info.plist")
echo "    Build  : OK"
echo "    Bundle : $BUNDLE_ID"

# ── 3. Install ─────────────────────────────────────────────────────────────────

echo ""
echo "==> [3/4] Installing..."

if [[ "$IS_SIMULATOR" == "true" ]]; then
    # Boot the simulator if it is not already running.
    SIM_STATE=$(xcrun simctl list devices | grep "$DEVICE_ID" | grep -oE '\(Booted\)|\(Shutdown\)' | tr -d '()' || true)
    if [[ "$SIM_STATE" != "Booted" ]]; then
        echo "  $ xcrun simctl boot $DEVICE_ID"
        xcrun simctl boot "$DEVICE_ID"
    fi
    echo "  $ xcrun simctl install $DEVICE_ID $APP_PATH"
    xcrun simctl install "$DEVICE_ID" "$APP_PATH"
else
    echo "  $ xcrun devicectl device install app --device $DEVICE_ID $APP_PATH"
    xcrun devicectl device install app \
        --device "$DEVICE_ID" \
        "$APP_PATH"
fi
echo "    Installed."

echo "  $ mkdir -p $OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# ── 4. Run benchmarks ──────────────────────────────────────────────────────────

TOTAL=$(( ${#BENCHMARKS[@]} * 2 * ATTEMPTS ))
CURRENT=0

echo ""
echo "==> [4/4] Running $TOTAL benchmark sessions"
echo "    ${#BENCHMARKS[@]} benchmarks  ×  2 parallel modes  ×  $ATTEMPTS attempts"
echo ""

for BENCHMARK in "${BENCHMARKS[@]}"; do
    for PARALLEL in "true" "false"; do
        for (( ATTEMPT=1; ATTEMPT<=ATTEMPTS; ATTEMPT++ )); do
            CURRENT=$(( CURRENT + 1 ))

            OUT_FILE="$OUTPUT_DIR/${DEVICE_PREFIX}_parallel_${PARALLEL}_${BENCHMARK}_${ATTEMPT}.txt"

            printf "  [%3d/%3d]  %-52s  parallel=%-5s  attempt=%d\n" \
                "$CURRENT" "$TOTAL" "$BENCHMARK" "$PARALLEL" "$ATTEMPT"
            printf "            → %s\n" "$(basename "$OUT_FILE")"

            set +e
            if [[ "$IS_SIMULATOR" == "true" ]]; then
                # simctl launch --console streams stdout and waits for the process to exit.
                echo "  $ xcrun simctl launch --console $DEVICE_ID $BUNDLE_ID benchmarks=$BENCHMARK parallel=$PARALLEL warmupCount=100 modes=REAL reportAtTheEnd=true"
                xcrun simctl launch \
                    --console \
                    "$DEVICE_ID" \
                    "$BUNDLE_ID" \
                    "benchmarks=$BENCHMARK" \
                    "parallel=$PARALLEL" \
                    "warmupCount=100" \
                    "modes=REAL" \
                    "reportAtTheEnd=true" \
                    2>&1 | tee "$OUT_FILE"
            else
                echo "  $ xcrun devicectl device process launch --console --device $DEVICE_ID $BUNDLE_ID -- benchmarks=$BENCHMARK parallel=$PARALLEL warmupCount=100 modes=REAL reportAtTheEnd=true"
                xcrun devicectl device process launch \
                    --console \
                    --device "$DEVICE_ID" \
                    "$BUNDLE_ID" \
                    -- \
                    "benchmarks=$BENCHMARK" \
                    "parallel=$PARALLEL" \
                    "warmupCount=100" \
                    "modes=REAL" \
                    "reportAtTheEnd=true" \
                    2>&1 | tee "$OUT_FILE"
            fi
            RUN_STATUS=${PIPESTATUS[0]}
            set -e

            if [[ $RUN_STATUS -ne 0 ]]; then
                printf "            ⚠  WARNING: process exited with code %d\n" "$RUN_STATUS"
            else
                printf "            ✓  done\n"
            fi

            # Brief cooldown between runs so the device settles
            echo "  $ sleep 3"
            sleep 3

        done
    done
done

echo ""
echo "==> All done!"
printf "    %d output files written to: %s\n" "$TOTAL" "$OUTPUT_DIR"
echo ""
