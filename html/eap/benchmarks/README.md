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
    :html-eap-benchmarks:run
```

For a single-worker baseline:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true \
    :html-eap-benchmarks:run --args='-prof gc -t 1'
```

Defaults: 1 or 64 rows, two JVM forks, two one-second warmups, and three one-second measurements.
GC profiling reports allocation.

## Add a benchmark

Add a Java class under `src/main/java` with JMH `@Benchmark` methods.
Gradle discovers it automatically.
