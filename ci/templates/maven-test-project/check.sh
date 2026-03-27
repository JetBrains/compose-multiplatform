#!/bin/bash

set -o pipefail

tempfile=$(mktemp)

# Pipe output to the file and terminal
if ! mvn clean install exec:java -Dexec.mainClass="MainKt" "$@" | tee "$tempfile"; then
    exit 1
fi

# For failing on warnings like
#   [WARNING] The POM for org.jetbrains.kotlin:kotlin-stdlib:jar:unspecified is missing, no dependency information available
#
# There is no a flag in Maven to fail on warnings in dependency resolution

if grep -q "\[WARNING\]" "$tempfile"; then
    echo "[ERROR] Warnings found"
    exit 1
fi
