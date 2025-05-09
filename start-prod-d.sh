#!/bin/bash

git remote update
LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u})
BASE=$(git merge-base @ @{u})

if [ $LOCAL = $REMOTE ]; then
    echo "Up-to-date"
elif [ $LOCAL = $BASE ]; then
    echo "------- Outdated. Pulling and compiling. -------"
    git pull
    ./gradlew clean shadowJar
elif [ $REMOTE = $BASE ]; then
    echo "Need to push"
else
    echo "Diverged"
fi
echo "------- Running bot -------"
java -jar build/libs/ajSupport-*.jar "$1"