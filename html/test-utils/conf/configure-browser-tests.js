module.exports = function configureBrowserTests(config) {
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

    config.browsers = ["ChromeForComposeTests"]

    // The EAP browser tests copy their generated SSR fixtures to Karma's working directory.
    config.files.push({
        pattern: "*ssr*hydration*.html",
        included: false,
        served: true,
        watched: false,
    });

    for (const fixture of [
        "ssr-hydration.html",
        "ssr-hydration-state.html",
        "ssr-number-hydration.html",
        "svg-ssr-hydration.html",
    ]) {
        config.proxies[`/${fixture}`] = `/base/${fixture}`;
    }
};
