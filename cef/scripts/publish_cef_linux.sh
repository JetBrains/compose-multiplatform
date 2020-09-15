#!/bin/sh
curl -T jcef-runtime-linux.zip -u$BINTRAY_USER:$BINTRAY_KEY -H "X-Bintray-Package:jcef" -H "X-Bintray-Version:jcef-linux-0.1" -H "X-Bintray-Publish:1" -H "X-Bintray-Override:1" https://api.bintray.com/content/jetbrains/skija/
