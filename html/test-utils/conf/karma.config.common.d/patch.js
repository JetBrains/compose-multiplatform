require("../../../../test-utils/conf/configure-browser-tests.js")(config);

config.plugins = config.plugins || [];
config.plugins = config.plugins.filter(it => it !== "kotlin-web-helpers/dist/karma-kotlin-reporter.js");
config.plugins.push(require("../../../../test-utils/conf/karma-kotlin-runner-decorator/karma-kotlin-reporter-decorated.js"));
