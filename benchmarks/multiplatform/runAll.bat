@echo off

for %%v in (1.7.3 1.8.0-rc01) do (
    for /l %%i in (1,1,5) do (
        for %%b in (AnimatedVisibility LazyGrid LazyGrid-ItemLaunchedEffect LazyGrid-SmoothScroll LazyGrid-SmoothScroll-ItemLaunchedEffect VisualEffects LazyList MultipleComponents MultipleComponents-NoVectorGraphics) do (
            gradlew run -Pcompose.version=%%v -PrunArguments="benchmarks=%%b saveStatsToCSV=true modes=SIMPLE"
        )
    )
)