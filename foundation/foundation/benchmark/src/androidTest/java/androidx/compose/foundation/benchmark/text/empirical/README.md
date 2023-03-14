# (Text) Empirical derived benchmarks

These benchmarks are derived from realistic text usage gathered from instrumenting text.

You should consider a drop in one of these metrics a *critical* signal that some performance change
has occurred that will be perceivable to real-world users in real production apps.

## Cached design

By design, all tests in this module will _intentionally_ hit layout caching on platform. This is
accomplished by using deterministic strings (such as "OK") that will reliably be cached.

This does not completely remove text layout cost, but it controls it to allow changes in Compose
text overhead to be more visible.

As much as possible, new benchmarks added to this module should endeavor to avoid measuring the
cost of text layout and focus on Compose related text code.

Care should be taken to ensure we only hit caching in minikin, and avoid hitting caches inside of 
compose text implementations unless there is concrete evidence that we hit the caches in test at the
same rate as production code.

## Parameters

Parameters are determined by empirical analysis of real-world text usage.

Parameter sets should be one of:

* AllApps - aggregate totals for all apps
* ChatApps - benchmarks are unique to chat apps (e.g. longer strings)
* SocialApps - benchmarks that are unique te social apps (e.g. more spans)
* ShoppingApps - benchmarks that are unique to shopping apps (e.g. more spans)

To run the full suite, set [DoFullBenchmark]

## Use-case naming

Name new benchmarks after the developer task being performed.

For example 
* `SetText` the task of setting text after loading it from some source
* `IfNotEmptyCallText` the task of adding Text, only if not empty

Benchmarks should attempt to provide a realistic async loading "default" value as state between
every call, such as `null` or `""` to ensure we're measuring the cost of calling Text.