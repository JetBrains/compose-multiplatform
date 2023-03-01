#!/bin/sh
rm archives/*
pushd multiplatform-template
./cleanup.sh
zip -r ../archives/multiplatform-template.zip . -x local.properties
popd
pushd desktop-template
./cleanup.sh
zip -r ../archives/desktop-template.zip . -x local.properties
popd
pushd web-template
./cleanup.sh
zip -r ../archives/web-template.zip . -x local.properties
popd
