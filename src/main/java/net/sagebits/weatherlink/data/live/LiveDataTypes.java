package net.sagebits.weatherlink.data.live;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Everything in this class will return a -100 for its property value, if it has never been set, or if it was missing in the most recent set, 
 * for some reason.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public enum LiveDataTypes
{
	wind_speed_last("last wind speed", 'f'),
	wind_dir_last("last wind direction", 'f'),
	rain_size("rain size", 'i'),
	rain_rate_last("rain rate", 'f'),
	rain_15_min("rain in the last 15 minutes", 'f'),
	rain_60_min("rain in the last 60 minutes", 'f'), 
	rain_24_hr("rain in the last 24 hours", 'f'),
	rain_storm("rain storm total", 'f'), 
	rain_storm_start_at("rain storm start", 'l'),
	rainfall_daily("rainfall since midnight", 'f'), 
	rainfall_monthly("rainfall this month", 'f'), 
	rainfall_year("rainfall this year", 'f'),
	wind_speed_hi_last_10_min("hi wind speed last 10 minutes", 'f'), 
	wind_dir_at_hi_speed_last_10_min("wind direction at hi wind speed last 10 minutes", 'f');
	
	private String description;
	private char dataType;
	
	private LiveDataTypes(String description, char dataType)
	{
		this.description = description;
		this.dataType = dataType;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	
	public Number parseJson(ObjectNode dataSource)
	{
		JsonNode node = dataSource.get(this.name());
		if (node != null && !"null".equals(node.asText()))
		{
			if (this.dataType == 'f')
			{
				return Float.parseFloat(node.asText());
			}
			else if (this.dataType == 'i')
			{
				return Integer.parseInt(node.asText());
			}
			else if (this.dataType == 'l')
			{
				//Adjust timestamps properly
				return Long.parseLong(node.asText()) * (this.ordinal() == rain_storm_start_at.ordinal() ? 1000 : 1);
			}
			else
			{
				throw new RuntimeException("Unexpected data type '" + dataType + "'");
			}
		}
		else
		{
			//Passed in data, but no value found.  Set to a -100
			return Double.valueOf(-100.0);
		}
	}
}
