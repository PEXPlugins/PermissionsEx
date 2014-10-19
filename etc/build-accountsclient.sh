#!/bin/sh
set -e

DESTDIR="AccountsClient"
git clone https://github.com/Mojang/AccountsClient.git $DESTDIR
pushd $DESTDIR

sed -i "s/testLogging\.showStandardStreams = true//" build.gradle
sed -i "s/apply plugin: 'idea'/apply plugin: 'idea'\napply plugin: 'maven'\ngroup = 'com.mojang'/" build.gradle
echo "rootProject.name = 'AccountsClient'" >> settings.gradle

gradle clean install

rm -rf $DESTDIR
popd
