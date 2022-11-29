build_log_simplifier process build messages, identifies the interesting ones, and validates that passing builds don't output any interesting messages

build_log_simplifier uses config files to do this analysis

If a config file changes, we want to rerun all tasks to re-validate the output messages

If we add these file as inputs to all tasks then it causes `clean` tasks to fail, saying something like `cleanMinifyReleaseWithR8 has both inputs and destroyables` ( b/258212798 )

So, we include build_log_simplifier's config files into buildSrc.jar here
