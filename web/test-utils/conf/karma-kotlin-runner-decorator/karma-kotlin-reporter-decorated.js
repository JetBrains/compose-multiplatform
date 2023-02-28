// This is a workaround for https://github.com/karma-runner/karma-teamcity-reporter/issues/86

// logger is needed only to log cases when `browser.id` is not in browserResults
const logger = require('../../../build/js/node_modules/karma/lib/logger');

const NewReporter = function(baseReporterDecorator, config, emitter) {

    const path = require('path');
    const fs = require('fs');
    const kotlinVersion = fs.readFileSync(path.resolve(__dirname, "../../../buildSrc/build/kotlin.version"), 'utf8');

    var kotlinReporterModule;
    try {
        kotlinReporterModule = require(`../../../build/js/packages_imported/kotlin-test-js-runner/${kotlinVersion}/karma-kotlin-reporter`);
    } catch (e) { // For some reason kotlin 1.8.20-Beta provides kotlin-test-js-runner 0.0.1
        kotlinReporterModule = require(`../../../build/js/packages_imported/kotlin-test-js-runner/0.0.1/karma-kotlin-reporter`);
    }
    const KotlinReporter = kotlinReporterModule['reporter:karma-kotlin-reporter'][1];
    this.$inject = KotlinReporter.$inject

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

module.exports = {
    'reporter:karma-kotlin-reporter': ['type', NewReporter]
};
