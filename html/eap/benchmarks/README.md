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

From the repository root:

```shell
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-benchmarks:ssrBenchmark
html/gradlew -p html -Pcompose.html.eap.enabled=true :html-eap-benchmarks:hydrationBenchmark
```

SSR defaults to String. Choose `-PrenderMode=writer`, `callback` or `all`.
Add `-Pcompose.html.lowering.enabled=true` for JVM compiler lowering.
Use `smokeBenchmark` for a short JVM run or `benchmark` for all JVM cases, including pooling.

SSR uses two forks, each with 5 seconds of warmup and ten 1-second measurements.
Hydration runs JS and Wasm in Chrome with two warmups and five samples, excluding HTML parsing and bundle loading.

Results: `html/eap/benchmarks/build/reports/benchmarks` for JVM.
`html/eap/benchmarks/build/reports/tests` and test output for hydration (mean, standard deviation and range).

## Add a benchmark

Add an `open` Kotlin class under `src/jvmMain/kotlin` with `kotlinx.benchmark` annotations.
Use `@JvmField` on `@Param` properties so JMH can populate them.
Shared workloads live in `src/commonMain/kotlin`. Browser benchmarks in `src/webTest/kotlin`.
