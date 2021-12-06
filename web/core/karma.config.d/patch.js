config.plugins = config.plugins || [];
config.plugins = config.plugins.filter(it => it !== "kotlin-test-js-runner/karma-kotlin-reporter.js");
config.plugins.push("../../../../../karma-kotlin-runner-decorator/karma-kotlin-reporter-decorated.js")
