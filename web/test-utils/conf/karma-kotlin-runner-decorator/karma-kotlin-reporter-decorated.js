// This is a workaround for https://github.com/karma-runner/karma-teamcity-reporter/issues/86

const kotlinReporterModule = require("../../../build/js/packages_imported/kotlin-test-js-runner/1.5.31/karma-kotlin-reporter");

const KotlinReporter = kotlinReporterModule['reporter:karma-kotlin-reporter'][1];

const NewReporter = function(baseReporterDecorator, config, emitter) {
    KotlinReporter.call(this, baseReporterDecorator, config, emitter);

    const onBrowserLogOriginal = this.onBrowserLog;
    const onSpecCompleteOriginal = this.onSpecComplete;

    this.onBrowserLog = (browser, log, type) => {
        if (!this.browserResults[browser.id]) {
            this.initializeBrowser(browser);
        }
        onBrowserLogOriginal(browser, log, type);
    }

    this.onSpecComplete = function (browser, result) {
        if (!this.browserResults[browser.id]) {
            this.initializeBrowser(browser);
        }
        onSpecCompleteOriginal(browser, result);
    }
}

NewReporter.$inject = KotlinReporter.$inject;

module.exports = {
    'reporter:karma-kotlin-reporter': ['type', NewReporter]
};
