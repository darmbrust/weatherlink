package net.sagebits.weatherlink.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javafx.application.Platform;
import net.sagebits.weatherlink.data.live.LiveData;
import net.sagebits.weatherlink.data.periodic.PeriodicData;

public class DataFetcher
{
	private volatile static DataFetcher instance_;
	private static final Logger log = LogManager.getLogger(DataFetcher.class);
	
	//Structure to store Properties for the most recent data entry of every periodic data item being followed.
	//WLL did|lsid ->  populated properties
	private ConcurrentHashMap<String, ConcurrentHashMap<StoredDataTypes, WeatherProperty>> mostRecentData= new ConcurrentHashMap<>(2);
	
	private DataFetcher()
	{
		//singleton
	}

	public static DataFetcher getInstance()
	{
		if (instance_ == null)
		{
			synchronized (DataFetcher.class)
			{
				if (instance_ == null)
				{
					instance_ = new DataFetcher();
				}
			}
		}
		return instance_;
	}
	
	/**
	 * May return null, otherwise, returns a WeatherProperty, that may be represent PeriodicData, or it may be further bound to LiveData. 
	 */
	public WeatherProperty getDataFor(String wllDeviceId, String sensorId, StoredDataTypes sdt)
	{
		ConcurrentHashMap<StoredDataTypes, WeatherProperty> data = mostRecentData.computeIfAbsent(wllDeviceId + "|" + sensorId, keyAgain -> new ConcurrentHashMap<>()); 

		WeatherProperty wp = data.computeIfAbsent(sdt, keyAgain -> {
			//we don't yet have data we are tracking for this element.  See if we have any data to populate with....
			
			//Try to read it from the DB
			WeatherProperty readData = PeriodicData.getInstance().getLatestData(wllDeviceId, sensorId, sdt);
			
			if (sdt.getLiveDataType() != null)
			{
				WeatherProperty liveProperty = LiveData.getInstance().getLiveData(wllDeviceId, sensorId).getValue(sdt.getLiveDataType());
				readData.bind(liveProperty);
			}
			return readData;
		});
		
		log.debug("Data requested for {} from {} {}, returning {}", sdt, wllDeviceId, sensorId, wp.get());
		return wp;
		
	}
	
	/**
	 * @return All unique WeatherLinkLive IDs, with a set of sensor IDs tied to each.
	 */
	public HashSet<String>  getWeatherLinkDeviceIds()
	{
		HashSet<String> result = PeriodicData.getInstance().getWeatherLinkDeviceIds();
		
		result.addAll(LiveData.getInstance().getAllWllDeviceIds());
		return result;
	}
	
	public Set<String> getSensorsFor(String wllDeviceId, StoredDataTypes sdt)
	{
		Set<String> result = PeriodicData.getInstance().getSensorsFor(wllDeviceId,sdt);
		
		if (sdt.getLiveDataType() != null)
		{
			result.addAll(LiveData.getInstance().getSensorIds(wllDeviceId));
		}
		return result;
	}

	public void update(HashMap<StoredDataTypes, Object> updatedData, String wllDeviceId, String sensorId, long timeStamp)
	{
		ConcurrentHashMap<StoredDataTypes, WeatherProperty> data = mostRecentData.computeIfAbsent(wllDeviceId + "|" + sensorId, keyAgain -> new ConcurrentHashMap<>());
		
		for (Entry<StoredDataTypes, Object> dataUpdate : updatedData.entrySet())
		{
			WeatherProperty wp = data.computeIfAbsent(dataUpdate.getKey(), keyAgain -> 
			{
				WeatherProperty readData = new WeatherProperty(dataUpdate.getKey().name());
				if (dataUpdate.getKey().getLiveDataType() != null)
				{
					readData.bind(LiveData.getInstance().getLiveData(wllDeviceId, sensorId).getValue(dataUpdate.getKey().getLiveDataType()));
				}
				//Set the initial values, so they aren't blank prior to the platform runLater, below, on the initial create.
				readData.setTimeStamp(timeStamp);
				readData.set(dataUpdate.getValue());
				return readData;
				
			});
			Platform.runLater(() ->
			{
				wp.setTimeStamp(timeStamp);
				wp.set(dataUpdate.getValue());
			});
		}
	}
}
