#!/bin/bash
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..
COMPOSE_OLDVER=0.3.0
COMPOSE_NEWVER=0.3.1
find -E $ROOT  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$COMPOSE_OLDVER/$COMPOSE_NEWVER/g" {} \;
APPCOMPAT_OLDVER=1.1.0
APPCOMPAT_NEWVER=1.3.0-beta01
find -E $ROOT  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$APPCOMPAT_OLDVER/$APPCOMPAT_NEWVER/g" {} \;

