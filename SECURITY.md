## Security

We do our best to make sure our products are free of security vulnerabilities. To reduce the risk of introducing a vulnerability,
you can follow these best practices:

* Always use the latest release. For security purposes, we sign our releases published on Maven Central
  with these PGP keys:

    * Key ID: **compose@jetbrains.com**
    * Fingerprint: **2072 3A63 99BC 0601 5428 3B37 CFAE 163B 64AC 9189**
    * Key type: **ed25519**

* Follow the Gradle [Dependency Verification Guide](https://docs.gradle.org/current/userguide/dependency_verification.html)
  to set up continuous verification or learn how to [manually verify a dependency](https://docs.gradle.org/current/userguide/dependency_verification.html#sec:manual-checking-dependency). 

* Use the latest versions of your application's dependencies. If you need to use a specific version of a dependency,
  periodically check if any new security vulnerabilities have been discovered. You can follow
  [the guidelines from GitHub](https://docs.github.com/en/code-security)
  or browse known vulnerabilities in the [CVE base](https://www.cve.org/CVERecord/).

We are very eager and grateful to hear about any security issues you find.
To report vulnerabilities that you discover in Compose Multiplatform,
please post a message directly to our [issue tracker](https://youtrack.jetbrains.com/newIssue?project=CMP&c=Type%20Security%20Problem) or send us an [email](mailto:security@jetbrains.org).

For more information on how our responsible disclosure process works, please check the [JetBrains Coordinated Disclosure Policy](https://www.jetbrains.com/legal/docs/terms/coordinated-disclosure/).

