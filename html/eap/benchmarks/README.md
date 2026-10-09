# EAP benchmarks

Compare three ways to render HTML:

- `fresh`: create a renderer for each call.
- `threadLocal`: reuse a renderer on each worker thread.
- `sharedKeyedPool`: reuse the production pool with one key shared by all workers.

Each case checks its HTML output before measurement.

## Run

From the repository root, run (default 4 workers):

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true \
    :html-eap-benchmarks:benchmark
```

Defaults: 1 or 64 rows, two JVM forks, two one-second warmups, and three one-second measurements.
Results are saved under `html/eap/benchmarks/build/reports/benchmarks`.

For a short smoke test of all strategies and row counts:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true \
    :html-eap-benchmarks:smokeBenchmark
```

## Add a benchmark

Add an `open` Kotlin class under `src/main/kotlin` with `kotlinx.benchmark` annotations.
Use `@JvmField` on `@Param` properties so JMH can populate them.
