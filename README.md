# weatherlink
Data logger and Desktop GUI for Davis WeatherLink

[![Build Status](https://travis-ci.org/darmbrust/weatherlink.svg?branch=master)](https://travis-ci.org/darmbrust/weatherlink)

# Running
Requred Java 11 or newer.

Download the jar file from the release folder.  Depending on if/how you have java installed, you may be able to just double-click the jar file.

If not, run with 
```
java -jar weatherLink.jar
```
 
If you don't have java, grab the latest 11 version for your platform:
https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot


# Release Notes
```
mvn -B gitflow:release-start gitflow:release-finish -DreleaseVersion=1.00 -DdevelopmentVersion=1.01

```