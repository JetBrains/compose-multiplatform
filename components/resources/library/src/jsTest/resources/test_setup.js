// Kotlin/JS accesses Skiko through global symbols. Wait for the runtime before starting tests.
(function () {
    const loaded = window.__karma__.loaded;
    window.__karma__.loaded = function () {
        import('/js-reexport-symbols.mjs')
            .then(runtime => runtime.api.awaitSkiko)
            .then(() => loaded.call(window.__karma__))
            .catch(error => window.__karma__.error(error.stack || String(error)));
    };
})();
