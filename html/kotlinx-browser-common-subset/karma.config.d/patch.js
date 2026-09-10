config.customLaunchers = {
    ChromeForComposeTests: {
        base: "ChromeHeadless",
        flags: ["--no-sandbox", "--disable-search-engine-choice-screen"]
    }
}

config.browsers = ["ChromeForComposeTests"]
