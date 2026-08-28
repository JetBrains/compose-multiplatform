# Generated facade verification

This module verifies the generated facade in isolation. It combines newly generated sources with
the checked-in handwritten interop files, then compiles them for JS, Wasm/JS, and JVM against the
pinned `kotlinx-browser` version.

The JVM tests check the generated API and its reports. This includes declaration ordering,
inheritance, signatures, constructors, dictionaries, browser API surfaces, and JVM class loading.
They also compare the fresh API manifest with the checked-in baseline.

## Run

Run these commands from the parent `generator/` directory. Tasks regenerate the staged common subset
and stage the handwritten interop files automatically.

Run every verification compilation and test:

```shell
../gradlew :verification:check
```

Run only the JVM ledger and manifest tests:

```shell
../gradlew :verification:jvmTest
```

Compile the generated facade for each implementation target without running the JVM tests:

```shell
../gradlew :verification:compileKotlinJs \
    :verification:compileKotlinWasmJs \
    :verification:compileKotlinJvm
```

Run the generator's unit tests together with the complete verification module:

```shell
../gradlew check
```
