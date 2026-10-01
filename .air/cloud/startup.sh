#!/usr/bin/env bash
# Air cloud startup script for the compose-multiplatform repository.
#
# Runs in two modes (see $AIR_STARTUP_MODE):
#   warmup - snapshot-baking run: do all the expensive, cacheable work
#            (JDK install, Android SDK, Gradle distributions, dependency caches).
#   task   - real task run: everything is already on disk from the snapshot,
#            so just make sure tools are on PATH and exit promptly.
set -euo pipefail

if [ "${AIR_STARTUP_MODE:-}" = warmup ]; then WARMUP=1; else WARMUP=; fi

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
TOOLS_DIR="$HOME/.air-tools"
JDK_DIR="$TOOLS_DIR/jdk-21"
export ANDROID_HOME="${ANDROID_HOME:-$HOME/android-sdk}"
mkdir -p "$TOOLS_DIR"

log() { echo "[startup] $*"; }

# ---------------------------------------------------------------------------
# JDK 21 (userspace install, no sudo). CI uses JDK 21 for examples/components
# and JDK 25 for the gradle plugin; 21 works for all Gradle builds here.
# ---------------------------------------------------------------------------
install_jdk() {
    if [ -x "$JDK_DIR/bin/java" ]; then
        return 0
    fi
    log "Installing Temurin JDK 21..."
    local tmp="$TOOLS_DIR/jdk.tar.gz"
    curl -fsSL "https://api.adoptium.net/v3/binary/latest/21/ga/linux/x64/jdk/hotspot/normal/eclipse" -o "$tmp"
    mkdir -p "$JDK_DIR.tmp"
    tar -xzf "$tmp" -C "$JDK_DIR.tmp" --strip-components=1
    rm -f "$tmp"
    mv "$JDK_DIR.tmp" "$JDK_DIR"
}

install_jdk
export JAVA_HOME="$JDK_DIR"
export PATH="$JAVA_HOME/bin:$PATH"

# Make JAVA_HOME/ANDROID_HOME available to subsequent shells in the environment.
PROFILE_SNIPPET="$HOME/.profile"
if ! grep -q "air-tools/jdk-21" "$PROFILE_SNIPPET" 2>/dev/null; then
    {
        echo "export JAVA_HOME=\"$JDK_DIR\""
        echo "export ANDROID_HOME=\"$ANDROID_HOME\""
        echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    } >> "$PROFILE_SNIPPET"
fi
if [ -w "$HOME/.bashrc" ] && ! grep -q "air-tools/jdk-21" "$HOME/.bashrc" 2>/dev/null; then
    {
        echo "export JAVA_HOME=\"$JDK_DIR\""
        echo "export ANDROID_HOME=\"$ANDROID_HOME\""
        echo "export PATH=\"\$JAVA_HOME/bin:\$PATH\""
    } >> "$HOME/.bashrc"
fi

# ---------------------------------------------------------------------------
# Android SDK command-line tools + platform-tools (userspace install).
# CI installs platform-tools only; AGP downloads the rest on demand.
# ---------------------------------------------------------------------------
install_android_sdk() {
    if [ -d "$ANDROID_HOME/cmdline-tools/latest" ]; then
        return 0
    fi
    log "Installing Android SDK command-line tools..."
    local tmp="$TOOLS_DIR/cmdline-tools.zip"
    curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip" -o "$tmp"
    mkdir -p "$ANDROID_HOME/cmdline-tools"
    unzip -q -o "$tmp" -d "$ANDROID_HOME/cmdline-tools"
    mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
    rm -f "$tmp"
    yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null 2>&1 || true
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" "platform-tools" >/dev/null
}

# ---------------------------------------------------------------------------
# Warmup: prime Gradle distributions and dependency caches for the two main
# builds (gradle-plugins and components). This work is baked into the snapshot.
# ---------------------------------------------------------------------------
warmup_gradle() {
    log "Warming up gradle-plugins build (Gradle 9.5 + deps)..."
    (cd "$REPO_ROOT/gradle-plugins" && chmod +x gradlew && ./gradlew --no-daemon :compose:classes)

    log "Warming up components build (Gradle 9.3.1 + deps)..."
    (cd "$REPO_ROOT/components" && chmod +x gradlew && ./gradlew --no-daemon help)
}

# ---------------------------------------------------------------------------
# Healthcheck: assert the environment actually works for a real task —
# the JDK runs and the main Gradle build configures and compiles.
# ---------------------------------------------------------------------------
healthcheck() {
    log "Healthcheck: java -version"
    java -version

    log "Healthcheck: gradle-plugins compiles"
    (cd "$REPO_ROOT/gradle-plugins" && ./gradlew --no-daemon :compose:classes --offline) \
        || (cd "$REPO_ROOT/gradle-plugins" && ./gradlew --no-daemon :compose:classes)

    log "Healthcheck: Android sdkmanager available"
    "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --version >/dev/null

    log "Healthcheck passed."
}

if [ -n "$WARMUP" ]; then
    install_android_sdk
    warmup_gradle
    healthcheck
else
    # Task run: snapshot already contains tools and caches; exit promptly.
    log "Task mode: environment ready (JAVA_HOME=$JAVA_HOME)."
fi
