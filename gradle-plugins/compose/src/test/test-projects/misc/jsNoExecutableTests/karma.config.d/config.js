config.set({
    browsers: ["ChromeHeadless_ComposeCI"],
    customLaunchers: {
        ChromeHeadless_ComposeCI: {
            base: "ChromeHeadless",
            flags: ["--no-sandbox"]
        }
    }
});
