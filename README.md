# weatherlink
Data logger and Desktop GUI for Davis WeatherLink

[![Build Status](https://travis-ci.org/darmbrust/weatherlink.svg?branch=master)](https://travis-ci.org/darmbrust/weatherlink)

![Screenshot](https://user-images.githubusercontent.com/5016252/72035210-d7f8ac80-325c-11ea-8db9-30ef083a728f.png)

# Running
Requres Java 11 or newer.

Download the weatherlink.jar file for your platform from the [release](https://github.com/darmbrust/weatherlink/releases) folder.  
Depending on if/how you have java installed, you may be able to just double-click the jar file.

If not, run with 
```
java -jar weatherlink<Platform>.jar [ip address]
```
If you don't have java, grab the latest 11 version for your platform:
https://adoptopenjdk.net/releases.html?variant=openjdk11&jvmVariant=hotspot

# First Run
It should be able to auto-locate your WeatherLinkLive, so long as it is on the local network.

If it fails to find your Weather Link Live, you can set the IP address as a command line parameter after the jar file name.

When you first run it, things may be a bit sparse.  Data will fill in as it runs.

# Data Store
Data and logs will be stored in your user home directory under Weather Link Live GUI Data.  It maintains a data store of the data it pulls
from your WeatherLinkLive every 10 seconds.

Data older than 7 days will be migrated from the weatherLinkData.mv.db database to the weatherLinkDataArchive.mv.db database - and the stored
data points will be trimmed to 1 per minute (instead of one every 10 seconds).

If you do not wish to maintain the historic data, you can simply delete the archive database (when the application is running)

# Issues
Feel free to open open trackers here.

#TODOs
There are lots of TODOs.... useful things I may add (pull requests welcome)

 - Add options for exporting data to TSV/CSV
 - Add config options for some string constants that are currently hardcoded (like "garage"
 - Add the ability to enable/disable gauges, and change their order
 - Add the ability to generate graphs from historical data with user specified options
 - Package the app as a self-contained JavaFX application
 - Push data to user-configured upstream stores like CWOP.

# Release Notes
```
mvn -B gitflow:release-start gitflow:release-finish -DreleaseVersion=1.07 -DdevelopmentVersion=1.08

```
