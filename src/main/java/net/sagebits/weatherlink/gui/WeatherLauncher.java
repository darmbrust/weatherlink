package net.sagebits.weatherlink.gui;

import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import javafx.application.Application;

public class WeatherLauncher
{
	public static void main(String[] args)
	{
		if (args != null && args.length > 0)
		{
			Pattern p = Pattern.compile("^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
					"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
			if (p.matcher(args[0]).matches())
			{
				WeatherLinkLiveGUIController.ip = args[0];
				LogManager.getLogger(WeatherLauncher.class).debug("Read IP '{}' from command line arg", args[0]);
			}
			else
			{
				LogManager.getLogger(WeatherLauncher.class).debug("Passed in param '{}' not an ip, ignoring.", args[0]);
			}
			
			if (args.length > 1) 
			{
				WeatherLinkLiveGUIController.sensorId = args[1];
				LogManager.getLogger(WeatherLauncher.class).debug("Read SensorID '{}' from command line arg", args[1]);
			}
		}
		Application.launch(WeatherLinkLiveGUI.class);
	}
}
