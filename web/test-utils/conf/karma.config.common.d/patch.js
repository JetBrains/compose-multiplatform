config.plugins = config.plugins || [];
config.plugins = config.plugins.filter(it => it !== "kotlin-test-js-runner/karma-kotlin-reporter.js");
config.plugins.push("../../../../../test-utils/conf/karma-kotlin-runner-decorator/karma-kotlin-reporter-decorated.js");

