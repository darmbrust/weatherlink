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

You will want to use a x64 installer.  It should work with either the JDK or JRE.

For windows, the file name will be something like: OpenJDK11U-jre_x64_windows_hotspot_11.0.9.1_1.msi

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

# Multiple outdoor sensors / wrong sensors displayed

If you have multiple outdoor sensors, and the GUI is trying to display the wrong one - for now, you have to tell it which 
sensor id to display with a startup parameter.

First, look in the debug log file

C:\Users\<username>\Weather Link Live GUI Data\weatherLinkDebug.log

for lines like this:

```
2021-01-13 15:13:53,693 DEBUG [gui-init] gui.WeatherLinkLiveGUIController - Using sensor id '279091' for outside info
2021-01-13 15:13:53,693 DEBUG [gui-init] gui.WeatherLinkLiveGUIController - NOT using sensor id '123456' for outside info
2021-01-13 15:13:53,693 DEBUG [gui-init] gui.WeatherLinkLiveGUIController - NOT using sensor id '223456' for outside info
```

Copy the id of the sensor that you wish to be displayed, if there are more than 2, you may have to just guess.

Pass in the id value as the second paramter after the jar file name.  The first parameter is for the IP of your weather link live - if you don't wish 
to provide a static IP here, just pass in garbage, and it will ignore it and continue to auto-locate the IP address.  

Example:

```
java -jar weatherlinkWindows.jar notAnIP 223456
```

Future enhancments will allow for customizing the GUI and sensor selection from within the GUI.

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

Change version in WeatherLinkLiveGUI.java too
```
mvn -B gitflow:release-start gitflow:release-finish -DreleaseVersion=1.09 -DdevelopmentVersion=1.10

```
