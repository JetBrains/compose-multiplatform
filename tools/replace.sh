#!/bin/bash
ROOT="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"/..
COMPOSE_OLDVER=0.4.0-rc1
COMPOSE_NEWVER=0.4.0-rc2
find -E $ROOT  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$COMPOSE_OLDVER/$COMPOSE_NEWVER/g" {} \;
APPCOMPAT_OLDVER=1.1.0
APPCOMPAT_NEWVER=1.3.0-beta01
find -E $ROOT  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$APPCOMPAT_OLDVER/$APPCOMPAT_NEWVER/g" {} \;
KOTLIN_OLDVER=1.5.0
KOTLIN_NEWVER=1.5.10
find -E $ROOT  -regex '.*\.(kts|properties|kt)' -exec sed -i '' -e "s/$KOTLIN_OLDVER/$KOTLIN_NEWVER/g" {} \;
git grep -C 1 __KOTLIN_COMPOSE_VERSION__ $ROOT
git grep -C 1 __LATEST_COMPOSE_RELEASE_VERSION__ $ROOT
