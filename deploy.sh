#!/bin/bash
echo "Listing all tags:"
git tag
echo "---"
read -e -p "Version tag: " ver
read -e -p "New dev version: " devver

git add -A && git commit -m "Release v$ver." && git push origin master
mvn --batch-mode -Dtag=${ver} release:prepare -Dresume=false -DreleaseVersion=${ver} -DdevelopmentVersion=${devver}-SNAPSHOT && \
mvn release:perform
