# weatherlink
Data logger and Desktop GUI for Davis WeatherLink

[![Build Status](https://travis-ci.org/darmbrust/weatherlink.svg?branch=master)](https://travis-ci.org/darmbrust/weatherlink)

# Running
Requred Java 11 or newer.

Download the weatherlink.jar file for your platform from the [release](https://github.com/darmbrust/weatherlink/releases) folder.  
Depending on if/how you have java installed, you may be able to just double-click the jar file.

If not, run with 
```
java -jar weatherlink<Platform>.jar
```
If you don't have java, grab the latest 11 version for your platform:
https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot

# Data Store
Data and logs will be stored in your user home directory under Weather Link Live GUI Data.  It maintains a data store of the data it pulls
from your WeatherLinkLive every 10 seconds.

# Issues
Feel free to open open trackers here.

#TODOs
There are lots of TODOs.... useful things I may add (pull requests welcome)

 - Fix the menus
 - Add options for exporting data to TSV/CSV
 - Add config options for some string constants that are currently hardcoded (like "garage"
 - Add the ability to enable/disable gauges, and change their order
 - Add the ability to generate graphs from historical data with user specified options
 - Package the app as a self-contained JavaFX application


# Release Notes
```
mvn -B gitflow:release-start gitflow:release-finish -DreleaseVersion=1.00 -DdevelopmentVersion=1.01

```