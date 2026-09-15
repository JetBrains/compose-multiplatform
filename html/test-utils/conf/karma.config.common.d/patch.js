config.plugins = config.plugins || [];
config.plugins = config.plugins.filter(it => it !== "kotlin-test-js-runner/karma-kotlin-reporter.js");
config.plugins.push("../../../../../test-utils/conf/karma-kotlin-runner-decorator/karma-kotlin-reporter-decorated.js");

require(require("path").resolve(
    config.basePath,
    "../../../../test-utils/conf/serve-html-test-resources.js"
))(config);

config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 10000;

config.browserNoActivityTimeout = 10000;
config.browserDisconnectTimeout = 10000;
config.browserDisconnectTolerance = 3;
config.browserConsoleLogOptions = {level: "debug", format: "%b %T: %m", terminal: true};
config.logLevel = config.LOG_DEBUG;

config.customLaunchers = {
    ChromeForComposeTests: {
        base: "ChromeHeadless",
        flags: ["--no-sandbox", "--disable-search-engine-choice-screen"]
    }
}

config.browsers = ["ChromeForComposeTests"];
