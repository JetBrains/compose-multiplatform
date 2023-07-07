
RealWorld4 is a performance test that attempts to simulate a real-world application of reasonably
large scale (eg. gmail-sized application).

The test has a few noteworthy characteristics:
 - Large number of widget definitions (~150)
 - Large application data model
 - Widgets take in a variety of parameters
 - Most widgets take in models (since models are considered best practice)
 - About 10% of widgets take in unmemoizable parameters
 - Widgets do some amount of work during composition
 - Reads layout constraints to decide children

