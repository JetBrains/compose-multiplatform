// This is a workaround for https://github.com/karma-runner/karma-teamcity-reporter/issues/86

// logger is needed only to log cases when `browser.id` is not in browserResults
const logger = require('../../../build/js/node_modules/karma/lib/logger')

const kotlinReporterModule = require("../../../build/js/packages_imported/kotlin-test-js-runner/1.6.21/karma-kotlin-reporter");

const KotlinReporter = kotlinReporterModule['reporter:karma-kotlin-reporter'][1];

const NewReporter = function(baseReporterDecorator, config, emitter) {
    KotlinReporter.call(this, baseReporterDecorator, config, emitter);

    const consoleLog = logger.create("NewReporter-KotlinReporter");

    const onBrowserLogOriginal = this.onBrowserLog;
    const onSpecCompleteOriginal = this.onSpecComplete;
    const onBrowserStartOriginal = this.onBrowserStart;


    this.onBrowserStart = (browser) => {
        consoleLog.info("onBrowserStart: id = " + browser.id);
        onBrowserStartOriginal(browser);
    }

    this.onBrowserLog = (browser, log, type) => {
        if (!this.browserResults[browser.id]) {
            consoleLog.info("onBrowserLog: force onBrowserStart id=" + browser.id);
            this.onBrowserStart(browser);
        }
        onBrowserLogOriginal(browser, log, type);
    }

    this.onSpecComplete = function (browser, result) {
        if (!this.browserResults[browser.id]) {
            consoleLog.info("onSpecComplete: force onBrowserStart id=" + browser.id);
            this.onBrowserStart(browser);
        }
        onSpecCompleteOriginal(browser, result);
    }
}

NewReporter.$inject = KotlinReporter.$inject;

module.exports = {
    'reporter:karma-kotlin-reporter': ['type', NewReporter]
};
