# EAP benchmarks

`RendererPoolBenchmark` compares three ways to render HTML:

- `fresh`: create a renderer for each call.
- `threadLocal`: reuse a renderer on each worker thread.
- `sharedKeyedPool`: reuse the production pool with one key shared by all workers.

Each case checks its HTML output before measurement.

`SsrBenchmark` measures String, Writer and callback rendering with JMH.
`HydrationBenchmark` measures JS/Wasm hydration of JVM-rendered HTML and checks DOM adoption.
See [third-party notices](../../licenses/third_party/README.md) for adapted workloads.

## Run

From the repository root, choose a suite:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-benchmarks:ssrBenchmark
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-benchmarks:streamingBenchmark
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-benchmarks:hydrationBenchmark
```

Hydration runs both JS and Wasm in Chrome. `benchmark` runs all JVM cases, including pooling.
Add `-Pcompose.html.lowering.enabled=true` to enable JVM compiler lowering (disabled by default).

Two JVM forks. SSR and streaming use a 5-second warmup and ten 1-second measurements per fork.
Pooling uses two 1-second warmups, three 1-second measurements, four workers and 1 or 64 rows.
Results are saved under `html/eap/benchmarks/build/reports/benchmarks`.

For a short JVM smoke test:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true \
    :html-eap-benchmarks:smokeBenchmark
```

Hydration uses two warmups and five samples, reporting mean, standard deviation and range in
`html/eap/benchmarks/build/reports/tests` and the test output. Timing excludes HTML parsing and bundle loading.

## Add a benchmark

Add an `open` Kotlin class under `src/jvmMain/kotlin` with `kotlinx.benchmark` annotations.
Use `@JvmField` on `@Param` properties so JMH can populate them.
Shared workloads live in `src/commonMain/kotlin`. Browser benchmarks in `src/webTest/kotlin`.
