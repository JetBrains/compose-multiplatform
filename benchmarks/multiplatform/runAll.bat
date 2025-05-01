@echo off

for /l %%i in (1,1,1) do (
    for %%v in (1.7.3 1.8.0-rc01) do (
        @rem for %%b in (AnimatedVisibility LazyGrid LazyGrid-ItemLaunchedEffect LazyGrid-SmoothScroll LazyGrid-SmoothScroll-ItemLaunchedEffect VisualEffects LazyList MultipleComponents MultipleComponents-NoVectorGraphics) do (
        for %%b in (AnimatedVisibility) do (
            gradlew run -Pcompose.version=%%v -PrunArguments="benchmarks=%%b saveStatsToCSV=true"
        )
    )
)