package net.sagebits.weatherlink.data.live;

import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Instance class to hold the most current live data at any given time.  Use this to get to a set of 
 * {@link ConditionsLive} for a particular sensor suite - which then contains observables that will update 
 * automatically as new data comes in.  Will support storing data for multiple WeatherLinkLive devices.
 * 
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class LiveData
{
	private volatile static LiveData instance_;
	
	private HashMap<String, WeatherLinkLive> wllData = new HashMap<>(2);
	
	private LiveData()
	{
		//singleton
	}
	
	public static LiveData getInstance()
	{
		if (instance_ == null)
		{
			synchronized (LiveData.class)
			{
				if (instance_ == null)
				{
					instance_ = new LiveData();
				}
			}
		}
		return instance_;
	}
	
	public Set<String> getWeatherLinkLiveIds()
	{
		return wllData.keySet();
	}
	
	public WeatherLinkLive getLiveData(String weatherLinkLiveId)
	{
		return wllData.get(weatherLinkLiveId);
	}
	
	protected void update(JsonNode data)
	{
		String did = Optional.ofNullable(data.get("did")).orElseThrow().asText();
		wllData.computeIfAbsent(did, keyAgain -> new WeatherLinkLive(did)).update(data);
	}
}
