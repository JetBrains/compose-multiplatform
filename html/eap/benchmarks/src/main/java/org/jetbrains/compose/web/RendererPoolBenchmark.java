/*
 * Copyright 2020-2026 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

package org.jetbrains.compose.web;

import androidx.compose.runtime.Composer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import org.jetbrains.compose.web.dom.ElementsKt;
import org.openjdk.jmh.annotations.*;

/** Compares renderer reuse strategies with the same HTML workload on concurrent workers. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@Threads(4)
public class RendererPoolBenchmark {
    @Param({"1", "64"})
    public int rows;

    // Keep the previous cache shape for a direct before/after comparison.
    private final ThreadLocal<Map<String, HtmlRenderer>> threadLocal =
        ThreadLocal.withInitial(HashMap::new);
    private Function2<Composer, Integer, Unit> content;

    @Setup
    public void setup() {
        content = (composer, changed) -> {
            for (int row = 0; row < rows; row++) {
                ElementsKt.Div(null, (scope, childComposer, childChanged) -> {
                    ElementsKt.Text("row <&>\uD83D\uDE00", childComposer, 0);
                    ElementsKt.Span(null, (childScope, textComposer, textChanged) -> {
                        ElementsKt.Text("tail", textComposer, 0);
                        return Unit.INSTANCE;
                    }, childComposer, 0, 1);
                    return Unit.INSTANCE;
                }, composer, 0, 1);
            }
            return Unit.INSTANCE;
        };
        // Check identical output for every strategy before measuring performance.
        String expected = "<div>row &lt;&amp;&gt;\uD83D\uDE00<span>tail</span></div>".repeat(rows);
        check(expected, fresh());
        check(expected, threadLocal());
        check(expected, sharedKeyedPool());
    }

    // Allocation baseline: each call owns a new renderer.
    @Benchmark
    public String fresh() {
        return render(new HtmlRenderer());
    }

    // Each worker reuses its own renderer.
    @Benchmark
    public String threadLocal() {
        Map<String, HtmlRenderer> cache = threadLocal.get();
        HtmlRenderer renderer = cache.get("template");
        if (renderer == null) {
            renderer = new HtmlRenderer();
            cache.put("template", renderer);
        }
        return render(renderer);
    }

    // Workers borrow exclusive renderers from the same keyed pool.
    @Benchmark
    public String sharedKeyedPool() {
        HtmlRendererPool pool = ComposeHtmlToStringKt.getHtmlRendererPool("benchmark-template");
        HtmlRenderer renderer = pool.borrow();
        String result = render(renderer);
        pool.recycle(renderer);
        return result;
    }

    private String render(HtmlRenderer renderer) {
        // Use the same rendering path in every case, excluding public validation wrappers.
        return renderer.render(true, false, null, 2048, "", content);
    }

    private static void check(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Every storage strategy must render the expected HTML");
        }
    }
}
