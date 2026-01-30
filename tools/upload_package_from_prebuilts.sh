#!/bin/bash

# Install Maven, i.e.
#  brew install mvn
#
# Create $HOME/.m2/settings.xml like this
#<settings>
#<repositories>
#    <repository>
#        <id>SpaceDev</id>
#        <url>https://maven.pkg.jetbrains.space/public/p/compose/dev</url>
#        <snapshots>
#            <enabled>false</enabled>
#        </snapshots>
#    </repository>
#</repositories>
#<servers>
#   <server>
#         <id>SpaceDev</id>
#         <username>XXX</username>
#         <password>XXX</password>
#   </server>
#</servers>
#</settings>
#
# Correct ANDROIDX_MAIN environment variable to point to androidx-main checkout.
export ANDROIDX_MAIN=$HOME/compose/androidx-main

# Set those variables to the package and version you'd like to upload.
export GROUP=org/jetbrains/dokka
export PACKAGE_ID=dokka-fatjar
export VERSION=0.9.17-g013

# No user-serviceable parts below, just run the script.
export PREBUILTS=${ANDROIDX_MAIN}/prebuilts/androidx/external
export DIR=${PREBUILTS}/${GROUP}/${PACKAGE_ID}/${VERSION}
export PACKAGE=${PACKAGE_ID}-${VERSION}
export POM=${DIR}/${PACKAGE}.pom
export JAR=${DIR}/${PACKAGE}.jar
export SOURCES=${DIR}/${PACKAGE}-sources.jar
export REPO=https://maven.pkg.jetbrains.space/public/p/compose/dev

mvn deploy:deploy-file \
  -Dfile=${JAR} \
  -DpomFile=${POM} \
  -Durl=${REPO} \
  -DrepositoryId=SpaceDev