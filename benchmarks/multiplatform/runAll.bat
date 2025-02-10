@echo off
setlocal enabledelayedexpansion

set "VERSIONS=0.240.0-SNAPSHOT 0.241.0-SNAPSHOT"
set "EXAMPLES=AnimatedVisibility LazyGrid VisualEffects LazyList Example1"

for /L %%N in (1 1 5) do (
    for %%V in (%VERSIONS%) do (
        for %%E in (%EXAMPLES%) do (
            call gradlew run -Pskiko.version=%%V -PrunArguments="benchmarks=%%E modes=SIMPLE"
        )
    )
)
