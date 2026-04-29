#!/bin/bash

# Script to build most of the examples, to verify if they can compile.
# Example must be buildable with dev builds, so they should have dev maven repo set up.

set -euo pipefail

if [ "$#" -ne 2 ]; then
echo "Specify Compose and Kotlin version. For example: ./validateExamples.sh 1.1.1 1.6.10"
exit 1
fi
COMPOSE_VERSION=$1
KOTLIN_VERSION=$2

printCommand() {
    local label=$1
    shift

    echo ">>> $label"
    "$@" || true
}

printTool() {
    local tool=$1
    if command -v "$tool" >/dev/null 2>&1; then
        echo "$tool: $(command -v "$tool")"
        "$tool" --version 2>&1 | sed -n '1,5p' || true
    else
        echo "$tool: <missing>"
    fi
}

printEnvironmentDiagnostics() {
    echo "===== Validation environment ====="
    echo "PWD: $PWD"
    echo "USER: ${USER:-<unset>}"
    echo "HOME: ${HOME:-<unset>}"
    echo "PATH: ${PATH:-<unset>}"
    echo "JAVA_HOME: ${JAVA_HOME:-<unset>}"
    echo "GRADLE_OPTS: ${GRADLE_OPTS:-<unset>}"
    echo "COMPOSE_VERSION: $COMPOSE_VERSION"
    echo "KOTLIN_VERSION: $KOTLIN_VERSION"
    printCommand "uname" uname -a
    printCommand "os-release" sed -n '1,40p' /etc/os-release
    printCommand "java version" java -version
    printTool jpackage
    printTool fakeroot
    printTool dpkg
    printTool dpkg-deb
    printTool rpm
    printTool rpmbuild
    printCommand "disk usage" df -h .
    echo "=================================="
}

dumpFile() {
    local file=$1
    echo "----- $file -----"
    if [ -f "$file" ]; then
        sed -n '1,240p' "$file" || true
    else
        echo "<missing>"
    fi
}

dumpComposeDiagnostics() {
    local project=$1
    echo "===== Compose diagnostics for $project ====="
    printCommand "build/compose tree" find . -maxdepth 8 \( -path '*/build/compose' -o -path '*/build/compose/*' \) -print
    printCommand "jpackage args files" find . -path '*/build/compose/tmp/*.args.txt' -type f -print
    while IFS= read -r argsFile; do
        dumpFile "$argsFile"
    done < <(find . -path '*/build/compose/tmp/*.args.txt' -type f -print 2>/dev/null)

    printCommand "packageDeb tmp contents" find . -maxdepth 8 -path '*/build/compose/tmp/packageDeb*' -print
    printCommand "packageReleaseDeb tmp contents" find . -maxdepth 8 -path '*/build/compose/tmp/packageReleaseDeb*' -print
    printCommand "package outputs" find . -maxdepth 8 -path '*/build/compose/binaries/*' -print
    echo "==========================================="
}

runGradle() {
    local project=$1
    local task=$2

    pushd "$project"
    echo "===== Running $project :: $task ====="
    printEnvironmentDiagnostics
    dumpComposeDiagnostics "$project"
    if ./gradlew "$task" -Pcompose.version="$COMPOSE_VERSION" -Pkotlin.version="$KOTLIN_VERSION" --info --stacktrace; then
        echo "===== Completed $project :: $task ====="
        dumpComposeDiagnostics "$project"
    else
        local status=$?
        echo "===== Failed $project :: $task with exit code $status ====="
        dumpComposeDiagnostics "$project"
        popd
        return "$status"
    fi
    popd
}

runGradle chat packageDistributionForCurrentOS
runGradle codeviewer packageDistributionForCurrentOS
runGradle imageviewer packageDistributionForCurrentOS
runGradle issues packageDistributionForCurrentOS
runGradle graphics-2d packageDistributionForCurrentOS
