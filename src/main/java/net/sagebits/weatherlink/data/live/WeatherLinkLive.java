package net.sagebits.weatherlink.data.live;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Represents Live data sent by an instance of a WeatherLinkLive. Carries {@link ConditionsLive} for one or more sensors connected
 * to the WeatherLinkLive that return live data.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class WeatherLinkLive
{
	private final Logger log = LogManager.getLogger();
	private final String did;  //This is for the WLL device
	private HashMap<String, ConditionsLive> liveData = new HashMap<>(2);
	
	protected WeatherLinkLive(String did)
	{
		this.did = did;
	}
	
	protected void update(JsonNode data)
	{
		String localDid = Optional.ofNullable(data.get("did")).orElseThrow().asText();
		if (!localDid.equals(did))
		{
			throw new IllegalArgumentException("Wrong DID value!");
		}
		final long ts = Long.parseLong(Optional.ofNullable(data.get("ts")).orElseThrow().asText()) * 1000;
		ArrayNode conditions = Optional.ofNullable((ArrayNode) data.get("conditions")).orElseThrow();
		Iterator<JsonNode> condition = conditions.elements();
		while (condition.hasNext())
		{
			ObjectNode conditionData = (ObjectNode) condition.next();
			final String lsid = Optional.ofNullable(conditionData.get("lsid")).orElseThrow().asText();
			liveData.computeIfAbsent(lsid, keyAgain -> new ConditionsLive(lsid, conditionData)).update(ts, conditionData);
		}
		log.debug("Updated live data");
	}
	
	public String getWeatherLinkId()
	{
		return did;
	}
	
	public Set<String> getSensorIds()
	{
		return liveData.keySet();
	}
	
	public ConditionsLive getLiveData(String sensorId)
	{
		return liveData.get(sensorId);
	}
}
