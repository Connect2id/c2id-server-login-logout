#!/usr/bin/bash

lastver=$(git describe --abbrev=0 --tags)

if [[ -n "$1" ]]; then
	version="$1"
else	
	echo "Last tag was: $lastver"
	echo "---"
	read -e -p "New version: " version	
fi

if [[ ! "$version" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
	echo "Invalid release version '$version'. Use MAJOR.MINOR.PATCH, for example 5.0.0." >&2
	exit 2
fi

if [[ -n "$(git status --porcelain)" ]]; then
	echo "The working tree is not clean. Commit or stash existing changes first." >&2
	exit 1
fi

branch="$(git branch --show-current)"
if [[ -z "$branch" ]]; then
	echo "The repository is in detached HEAD state. Check out the release branch first." >&2
	exit 1
fi

mvn versions:set -DnewVersion=$version

git add -A && git commit -m "Release $version." && git push origin master && \
git tag --annotate "$version" --message "Release $version" && \
git push origin "$version"

git log $lastver..HEAD^1 --oneline > changelog.txt && \
gh release create -F changelog.txt -t "$version" $version
rm changelog.txt

echo "Released c2id-server-login-logout:$version on GitHub"
