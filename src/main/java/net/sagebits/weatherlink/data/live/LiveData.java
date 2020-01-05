package net.sagebits.weatherlink.data.live;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	private final Logger log = LogManager.getLogger();
	
	//Map weatherLinkLive instances (by their did) to map of condition data (one per sensor id 'lsid') 
	private ConcurrentHashMap<String, ConcurrentHashMap<String, ConditionsLive>> liveData_= new ConcurrentHashMap<>(2);
	
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
	
	public Set<String> getAllWllDeviceIds()
	{
		return liveData_.keySet();
	}
	
	public Set<String> getSensorIds(String weatherLinkLiveId)
	{
		return Optional.ofNullable(liveData_.get(weatherLinkLiveId)).map(table -> (Set<String>)table.keySet()).orElse(new HashSet<>());
	}
	
	public ConditionsLive getLiveData(String weatherLinkLiveId, String sensorId)
	{
		ConcurrentHashMap<String, ConditionsLive> conditions = liveData_.computeIfAbsent(weatherLinkLiveId, keyAgain -> new ConcurrentHashMap<>(2));
		return conditions.computeIfAbsent(sensorId, keyAgain -> new ConditionsLive(sensorId, null));
	}
	
	protected void update(JsonNode data)
	{
		String did = Optional.ofNullable(data.get("did")).orElseThrow().asText();
		ConcurrentHashMap<String, ConditionsLive> conditions = liveData_.computeIfAbsent(did, keyAgain -> new ConcurrentHashMap<>(2));
		
		final long ts = Long.parseLong(Optional.ofNullable(data.get("ts")).orElseThrow().asText()) * 1000;
		
		ArrayNode conditionsData = Optional.ofNullable((ArrayNode) data.get("conditions")).orElseThrow();
		Iterator<JsonNode> condition = conditionsData.elements();
		while (condition.hasNext())
		{
			ObjectNode conditionData = (ObjectNode) condition.next();
			final String lsid = Optional.ofNullable(conditionData.get("lsid")).orElseThrow().asText();
			conditions.computeIfAbsent(lsid, keyAgain -> new ConditionsLive(lsid, conditionData)).update(ts, conditionData);
		}
		log.debug("Updated live data");
	}
}
