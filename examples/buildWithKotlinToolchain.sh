#!/bin/bash

# Script to build an example with the Kotlin Toolchain, optionally with the given Compose and Kotlin versions.
# The Kotlin Toolchain can't override versions from the command line, so in that case the example is copied
# to a temporary directory, and every module there applies a template with the versions and the dev maven repo.

set -euo pipefail

if [ "$#" -lt 2 ] || [ "$#" -gt 4 ]; then
    echo "Specify the example, the platform and optionally Compose and Kotlin versions. For example: ./buildWithKotlinToolchain.sh codeviewer jvm 1.13.0-alpha03 2.3.20"
    exit 1
fi

example="$1"
platform="$2"
compose_version="${3:-}"
kotlin_version="${4:-}"
build_dir="$example"

if [ -n "$compose_version" ]; then
    temp_dir="$(mktemp -d)"
    # Keep the copy if the build fails, so that its logs can be inspected
    trap 'if [ "$?" -eq 0 ]; then rm -rf "$temp_dir"; else echo "Failed $example, its copy is kept in $temp_dir"; fi' EXIT

    tar -C "$example" --exclude=./build -cf - . | tar -C "$temp_dir" -xf -
    echo "Building a copy of $example in $temp_dir with Compose $compose_version${kotlin_version:+ and Kotlin $kotlin_version}"

    template="validation-versions.module-template.yaml"
    {
        echo "repositories:"
        echo "  - https://packages.jetbrains.team/maven/p/cmp/dev"
        echo
        echo "settings:"
        echo "  compose:"
        echo "    version: $compose_version"
        if [ -n "$kotlin_version" ]; then
            echo "  kotlin:"
            echo "    version: $kotlin_version"
        fi
    } > "$temp_dir/$template"

    find "$temp_dir" -name module.yaml | while read -r module; do
        { printf 'apply:\n  - //%s\n\n' "$template"; cat "$module"; } > "$module.tmp"
        mv "$module.tmp" "$module"
    done

    if [ -f "$temp_dir/libs.versions.toml" ]; then
        sed -E "s/^(compose-multiplatform[[:space:]]*=[[:space:]]*).*/\1\"$compose_version\"/" "$temp_dir/libs.versions.toml" > "$temp_dir/libs.versions.toml.tmp"
        mv "$temp_dir/libs.versions.toml.tmp" "$temp_dir/libs.versions.toml"
    fi

    build_dir="$temp_dir"
fi

cd "$build_dir"
./kotlin build -p "$platform"
