# kotlinx-browser common subset

This module generates a multiplatform DOM facade from the published `kotlinx-browser` sources.
Common code receives portable `expect` declarations, web targets keep browser identity through
typealiases, and the JVM receives stubs.

The generator resolves
`org.jetbrains.kotlinx:kotlinx-browser:<version>:sources`, unpacks its `webMain` sources under the
generator runner's `build/`, and feeds the source set to KSP. Fresh output is staged
under `generator/runner/build/generated/kotlinxBrowserCommonSubset`. Reviewed source output is
checked in under `src/`, so compiling the library does not run KSP or require the sources JAR.
Generator checks and tests do run KSP and resolve the pinned sources artifact.

## How generation works

1. Gradle resolves and unpacks the pinned Maven sources artifact into the runner's `build/`.
2. KSP reads the browser source files named in
   [`portable-dom-selection.txt`](generator/src/main/resources/portable-dom-selection.txt).
3. `SelectionPolicy` chooses classifier identities, while `ClosureResolver` adds supported
   inheritance and signature dependencies.
4. `SignatureAnalyzer`, `PortableTypeMapper`, and `MemberScanner` build the portable model and
   record a decision for every declaration they inspect.
5. `FacadeSourceEmitter` renders KotlinPoet files as staged KSP resources.
6. Gradle stages those resources for comparison with, or explicit replacement of, checked-in
   sources.

## Input boundary

The selection policy names these generated browser declaration files:

- `org.w3c.dom.kt` contains the main DOM, HTML,
  canvas, network, and worker declarations.
- `org.w3c.dom.css.kt` contains stylesheets,
  CSS rules, and the inline-style `CSSStyleDeclaration` API.
- `org.w3c.dom.events.kt` contains the event
  types, listeners, and option dictionaries.
- `org.w3c.dom.clipboard.kt` contains clipboard events, dictionaries, and the
  asynchronous clipboard API.

KSP reads their `expect` declarations from `webMain`. The corresponding JS and Wasm/JS files are
target implementations. Generated web typealiases must compile
against both.

## Selection policy

The source selection policy supports:

- `input <file>`: inspect every top-level classifier in a browser source file.
- `signature-only-package <package>`: allow referenced classifiers as bare identities.
- `defer <reason> <classifier>`: keep a classifier outside the current facade with an explicit
  manifest reason.

Input classifiers are emitted by default. An explicit `defer` decision also prevents signature
discovery from adding the classifier back indirectly.

The selection file decides which packages may join the closure. `Names.kt` only converts selected
browser package names to facade names. Signature-only packages emit bare identities when reached
only through a signature. When a mapped classifier is an actual supertype, the closure emits it
normally instead and preserves the inheritance edge.

## Source sets

| Source set | Role |
| --- | --- |
| `commonMain` | Portable `expect` declarations, dictionaries, values, and interop contracts |
| `webMain` | Shared browser dependency only; no generated actuals |
| `jsMain` | Browser facade typealiases and bridges, plus JS interop implementations |
| `wasmJsMain` | Browser facade typealiases and bridges, plus Wasm/JS interop implementations |
| `jvmMain` | Inert but type-correct stubs, stateful dictionaries, constants, and enum-like values |

The JVM output is a compatibility stub, not a DOM implementation.

## Modeling rules

Classifiers preserve their portable modality and mapped inheritance edges. Behavioral interfaces
remain interfaces. If an inheritance edge cannot join the closure, generation fails.

A member is emitted only when its complete signature maps recursively. Generic arguments, callback
parameters and results, nullability, projections, and varargs are checked structurally. A failure is
reported against the unsupported leaf type rather than the surrounding shape.

Constructors follow the same signature rules as functions. The JVM emitter chooses a primary
constructor for delegation and adds a protected no-argument path when subclasses require one.

Top-level operator extensions are emitted as wrapper functions on every target, so their facade
parameters are always explicit. Browser `definedExternally` defaults cannot be copied safely into
the common wrapper contract because common code must also compile for the JVM.

Option dictionaries keep mutable properties and inheritance. Their factories use portable inert
defaults.

KSP exposes numeric companion constant names and types but not their initializer expressions. JVM
actuals therefore receive deterministic inert values derived only from the selected source model.

Browser string enums are emitted as classifier identities plus companion extension values. Web
targets forward to the browser values; JVM getters return stable private singletons.

Portable companion functions are retained with their complete signatures. Web typealiases call the
browser companion directly, while JVM companions provide inert, type-correct bodies.

Portable interop types cover browser signatures involving `JsAny`, `JsString`, `JsNumber`,
`JsDouble`, `JsArray`, and `Promise`.

## Reports and validation

Generation writes these reports beside the generated sources:

- `model.txt`: emitted classifiers, shapes, members, constructors, factories, and values.
- `coverage.txt`: every port or skip decision made while building the closure.
- `api-manifest.txt`: every declaration in the selected browser input files, marked `EMITTED` or
  `EXCLUDED` with a structured reason.

The API manifest is checked in at
[`api/dom-api-manifest.txt`](api/dom-api-manifest.txt). Generation fails for unaccounted
declarations or stale exclusions, and `GeneratedApiManifestTest` fails when generated output differs
from the checked-in baseline.

[`dom-api-exclusions.txt`](generator/src/main/resources/dom-api-exclusions.txt) is reserved for
specific declaration-level decisions that cannot be expressed by classifier selection.

## Run

Run all commands in this section from the repository's `html/` directory, which contains the Gradle
wrapper. Generate staged output without changing checked-in sources:

```shell
./gradlew generateKotlinxBrowserCommonSubset
```

Check that staged output matches `src/` and `api/dom-api-manifest.txt`:

```shell
./gradlew checkKotlinxBrowserCommonSubset
```

After reviewing a deliberate generated API change, replace the checked-in files explicitly:

```shell
./gradlew updateKotlinxBrowserCommonSubset
```

Run the generator tests and the library's multiplatform checks:

```shell
./gradlew -p kotlinx-browser-common-subset/generator test
./gradlew :kotlinx-browser-common-subset:check
```
