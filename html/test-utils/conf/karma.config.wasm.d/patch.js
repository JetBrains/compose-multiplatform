// Wasm uses Kotlin's standard test reporter and needs more time to start and disconnect.
require(require("path").resolve(
    config.basePath,
    "../../../../test-utils/conf/serve-html-test-resources.js"
))(config);

config.client.mocha = config.client.mocha || {};
config.client.mocha.timeout = 10000;

config.browserNoActivityTimeout = 60000;
config.browserDisconnectTimeout = 60000;
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
