# Static HTML source lowering

This standalone generator rewrites a small part of the Compose HTML DSL before the Compose compiler runs. It writes generated copies to a separate directory and leaves authored sources unchanged.

For example, `Span(attrs = { classes("label") }) { Text(name) }` becomes `LoweredDynamicTextTag` with a prevalidated start tag. `name` still runs inside the element scope, after the start tag. Constant subtrees become one HTML fragment; the browser runs their original DOM-building lambda.

Build and check with JDK 21:

```sh
./gradlew check
build/install/ssr-static-lowering/bin/ssr-static-lowering path/to/authored path/to/generated
```

By default, the generator also folds literal `repeat` loops with counts from 0 to 10,000 and bounded, constant HTML bodies. Dynamic values, side effects, shadowed names, and unsupported code keep their original form. `lowering-report.tsv` lists the rewrites. Use `--mode=containers` to compare performance without loop folding.

The generator checks syntax and imports, not resolved symbols. Unknown star imports disable rewriting. Use `--trusted-star-import=your.package` only after checking that the package does not conflict with HTML DSL names. Review that choice when the package changes.

Compile the same generated common sources on JVM, JS, and Wasm. Run generation before each compilation and use the generated directory in place of the authored one. `gradle-integration.gradle.kts` shows the wiring for a Kotlin Multiplatform application.

The runtime validates generated HTML against ordinary rendering once at initialization. Constant pages can then return their HTML string directly. This saves repeated work but can increase generated code size and first-render time. Forms, events, CSS blocks, raw-text elements, and unsupported parser cases keep their ordinary rendering paths.
